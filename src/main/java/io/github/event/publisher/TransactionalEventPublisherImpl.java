package io.github.event.publisher;

import io.github.event.core.api.Event;
import io.github.event.core.api.EventListener;
import io.github.event.core.api.EventProcessorCallback;
import io.github.event.core.api.TransactionalEventPublisher;
import io.github.event.core.model.TransactionPhase;
import io.github.event.transaction.TransactionManager;
import io.github.event.transaction.TransactionSynchronization;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 트랜잭션 단계에 따라 이벤트를 발행하는 클래스
 */
@Slf4j
public class TransactionalEventPublisherImpl implements TransactionalEventPublisher {
    
    private final EventProcessorCallback processorCallback;
    private final TransactionManager transactionManager;
    private final Executor asyncExecutor;
    private final ConcurrentMap<TransactionPhase, List<EventListenerPair<?>>> eventsMap = new ConcurrentHashMap<>();
    
    /**
     * 기본 스레드 풀을 사용하는 생성자 
     * 
     * @param processorCallback 이벤트 처리 콜백
     * @param transactionManager 트랜잭션 관리자
     */
    public TransactionalEventPublisherImpl(EventProcessorCallback processorCallback, TransactionManager transactionManager) {
        this(processorCallback, transactionManager, 
             Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }
    
    /**
     * 커스텀 스레드 풀을 사용하는 생성자
     * 
     * @param processorCallback 이벤트 처리 콜백
     * @param transactionManager 트랜잭션 관리자
     * @param asyncExecutor 비동기 처리를 위한 실행기
     */
    public TransactionalEventPublisherImpl(EventProcessorCallback processorCallback, 
                                          TransactionManager transactionManager,
                                          Executor asyncExecutor) {
        this.processorCallback = processorCallback;
        this.transactionManager = transactionManager;
        this.asyncExecutor = asyncExecutor;
    }
    
    @Override
    public void publishInTransaction(Event event, TransactionPhase phase) {
        if (phase == TransactionPhase.IMMEDIATE || !transactionManager.isTransactionActive()) {
            // 즉시 발행하거나 트랜잭션이 없는 경우
            processorCallback.multicast(event);
            return;
        }
        
        // 트랜잭션 단계별로 이벤트 저장
        eventsMap.computeIfAbsent(phase, k -> new ArrayList<>())
                .add(new EventListenerPair<>(event, null));
        
        // 첫 이벤트인 경우에만 동기화 등록
        if (eventsMap.get(phase).size() == 1) {
            transactionManager.registerSynchronization(new EventTransactionSynchronization());
        }
    }
    
    @Override
    public CompletableFuture<Void> publishAsyncInTransaction(Event event, TransactionPhase phase) {
        publishInTransaction(event, phase);
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public <T extends Event> void publishInTransaction(Event event, TransactionPhase phase, EventListener<T> listener) {
        if (phase == TransactionPhase.IMMEDIATE || !transactionManager.isTransactionActive()) {
            // 즉시 발행하거나 트랜잭션이 없는 경우
            invokeListener(event, listener);
            return;
        }
        
        // 트랜잭션 단계별로 이벤트와 리스너 저장
        eventsMap.computeIfAbsent(phase, k -> new ArrayList<>())
                .add(new EventListenerPair<>(event, listener));
        
        // 첫 이벤트인 경우에만 동기화 등록
        if (eventsMap.get(phase).size() == 1) {
            transactionManager.registerSynchronization(new EventTransactionSynchronization());
        }
    }
    
    @Override
    public <T extends Event> void publishAsyncInTransaction(Event event, TransactionPhase phase, EventListener<T> listener) {
        if (phase == TransactionPhase.IMMEDIATE || !transactionManager.isTransactionActive()) {
            // 즉시 발행하거나 트랜잭션이 없는 경우
            asyncExecutor.execute(() -> invokeListener(event, listener));
            return;
        }
        
        // 트랜잭션 단계별로 이벤트와 리스너 저장 (비동기 플래그 설정)
        eventsMap.computeIfAbsent(phase, k -> new ArrayList<>())
                .add(new EventListenerPair<>(event, listener, true));
        
        // 첫 이벤트인 경우에만 동기화 등록
        if (eventsMap.get(phase).size() == 1) {
            transactionManager.registerSynchronization(new EventTransactionSynchronization());
        }
    }
    
    /**
     * 리스너의 이벤트 처리 메서드를 호출합니다.
     * 
     * @param event 처리할 이벤트
     * @param listener 이벤트를 처리할 리스너
     */
    @SuppressWarnings("unchecked")
    private <T extends Event> void invokeListener(Event event, EventListener<T> listener) {
        try {
            if (listener != null) {
                processorCallback.processEvent(event, listener);
            } else {
                // 리스너가 지정되지 않은 경우 멀티캐스터를 통해 모든 리스너에게 전달
                processorCallback.multicast(event);
            }
        } catch (Exception e) {
            // 기타 예외 처리
            throw new RuntimeException("이벤트 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    private class EventTransactionSynchronization implements TransactionSynchronization {
        
        @Override
        public void beforeCommit() {
            publishEventsForPhase(TransactionPhase.BEFORE_COMMIT);
        }
        
        @Override
        public void afterCommit() {
            publishEventsForPhase(TransactionPhase.AFTER_COMMIT);
        }
        
        @Override
        public void afterRollback() {
            publishEventsForPhase(TransactionPhase.AFTER_ROLLBACK);
        }
        
        private void publishEventsForPhase(TransactionPhase phase) {
            List<EventListenerPair<?>> events = eventsMap.remove(phase);
            if (events == null || events.isEmpty()) {
                return;
            }
            
            for (EventListenerPair<?> pair : events) {
                try {
                    if (pair.isAsync()) {
                        asyncExecutor.execute(() -> processEventPair(pair));
                    } else {
                        processEventPair(pair);
                    }
                } catch (Exception e) {
                    // 오류 로깅 및 계속 진행
                    // 다른 이벤트 처리에 영향을 주지 않도록 예외를 잡음
                    log.error("이벤트 처리 중 오류 발생: {}", e.getMessage(), e);
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        private <T extends Event> void processEventPair(EventListenerPair<?> pair) {
            try {
                if (pair.getListener() != null) {
                    EventListener<T> listener = (EventListener<T>) pair.getListener();
                    processorCallback.processEvent(pair.getEvent(), listener);
                } else {
                    processorCallback.multicast(pair.getEvent());
                }
            } catch (Exception e) {
                // 오류 로깅
                log.error("이벤트 처리 중 오류 발생: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 이벤트와 리스너 쌍을 저장하는 내부 클래스
     */
    @Getter
    @AllArgsConstructor
    private static class EventListenerPair<T extends Event> {
        private final Event event;
        private final EventListener<T> listener;
        private final boolean async;
        
        public EventListenerPair(Event event, EventListener<T> listener) {
            this(event, listener, false);
        }
    }
} 
