package io.github.event.listener;

import io.github.event.annotation.*;
import io.github.event.core.api.Event;
import io.github.event.core.api.EventDispatcher;
import io.github.event.core.api.EventListener;
import io.github.event.core.api.EventProcessorCallback;
import io.github.event.core.model.TransactionPhase;
import io.github.event.idempotent.IdempotentEventProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 이벤트를 리스너에게 전달하는 멀티캐스터
 */
@Slf4j
public class ApplicationMulticaster implements EventProcessorCallback {

    private final Map<Class<? extends Event>, List<EventListener<?>>> listenerMap = new ConcurrentHashMap<>();
    private EventDispatcher eventDispatcher;
    private final IdempotentEventProcessor idempotentProcessor;

    public ApplicationMulticaster(EventDispatcher eventDispatcher, IdempotentEventProcessor idempotentProcessor) {
        this.eventDispatcher = eventDispatcher;
        this.idempotentProcessor = idempotentProcessor;
    }

    /**
     * 이벤트 디스패처를 설정합니다.
     *
     * @param eventDispatcher 설정할 이벤트 디스패처
     */
    public void setEventDispatcher(EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    /**
     * 리스너를 등록합니다.
     *
     * @param listener 등록할 리스너
     */
    public <T extends Event> void addListener(EventListener<T> listener) {
        Class<T> eventType = listener.getEventType();
        listenerMap.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * 이벤트를 모든 해당 리스너에게 전달합니다.
     *
     * @param event 전달할 이벤트
     */
    @Override
    public void multicast(Event event) {
        Class<?> eventType = event.getClass();

        // 이벤트 타입과 일치하는 리스너 찾기
        for (Map.Entry<Class<? extends Event>, List<EventListener<?>>> entry : listenerMap.entrySet()) {
            if (entry.getKey().isAssignableFrom(eventType)) {
                for (EventListener<?> listener : entry.getValue()) {
                    processEvent(event, listener);
                }
            }
        }
    }

    /**
     * 리스너의 설정에 따라 이벤트를 처리합니다.
     *
     * @param event    처리할 이벤트
     * @param listener 이벤트를 처리할 리스너
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Event> void processEvent(Event event, EventListener<?> listener) {
        EventListener<T> typedListener = (EventListener<T>) listener;

        // 리스너 설정 확인 (async, transactional, phase, idempotent)
        boolean isAsync = false;
        boolean isTransactional = false;
        TransactionPhase phase = TransactionPhase.AFTER_COMMIT;
        boolean isIdempotent = false;

        // MethodEventListener인 경우 설정 가져오기
        if (typedListener instanceof MethodEventListener) {
            MethodEventListener<?> methodListener = (MethodEventListener<?>) typedListener;
            isAsync = methodListener.isAsync();
            isTransactional = methodListener.isTransactional();
            phase = methodListener.getTransactionPhase();
            isIdempotent = methodListener.isIdempotent();

            // 멱등성 처리
            if (isIdempotent && idempotentProcessor.isProcessed(event)) {
                log.debug("이미 처리된 이벤트 무시: {}, ID: {}",
                        event.getClass().getSimpleName(), event.getEventId());
                return;
            }

            // AsyncProcessing 어노테이션 확인 (메서드 레벨)
            AsyncProcessing methodAsyncProcessing = methodListener.getMethod().getAnnotation(AsyncProcessing.class);
            if (methodAsyncProcessing != null) {
                log.debug("메서드 레벨 AsyncProcessing 어노테이션 발견: {}, 모드: {}",
                        methodListener.getMethodName(), methodAsyncProcessing.mode());
            }
        } else {
            // 클래스 레벨 어노테이션 처리
            Class<?> listenerClass = typedListener.getClass();

            // 멱등성 처리 확인
            isIdempotent = listenerClass.isAnnotationPresent(Idempotent.class);
            if (isIdempotent && idempotentProcessor.isProcessed(event)) {
                log.debug("이미 처리된 이벤트 무시: {}, ID: {}",
                        event.getClass().getSimpleName(), event.getEventId());
                return;
            }

            // AsyncListener 확인
            AsyncListener asyncListener = listenerClass.getAnnotation(AsyncListener.class);
            if (asyncListener != null) {
                isAsync = true;
                log.debug("클래스 레벨 AsyncListener 어노테이션 발견: {}, 모드: {}",
                        listenerClass.getSimpleName(), asyncListener.mode());
            }

            // TransactionalListener 확인
            TransactionalListener txListener = listenerClass.getAnnotation(TransactionalListener.class);
            if (txListener != null) {
                isTransactional = true;
                phase = txListener.value();
            }

            // AsyncTransactionalListener 확인
            AsyncTransactionalListener asyncTxListener = listenerClass.getAnnotation(AsyncTransactionalListener.class);
            if (asyncTxListener != null) {
                isAsync = true;
                isTransactional = true;
                phase = asyncTxListener.value();
                log.debug("클래스 레벨 AsyncTransactionalListener 어노테이션 발견: {}, 모드: {}, 단계: {}",
                        listenerClass.getSimpleName(), asyncTxListener.mode(), asyncTxListener.value());
            }

            // AsyncProcessing 어노테이션 확인 (클래스 레벨)
            AsyncProcessing classAsyncProcessing = listenerClass.getAnnotation(AsyncProcessing.class);
            if (classAsyncProcessing != null) {
                log.debug("클래스 레벨 AsyncProcessing 어노테이션 발견: {}, 모드: {}",
                        listenerClass.getSimpleName(), classAsyncProcessing.mode());
            }
        }

        // 이벤트 클래스의 AsyncProcessing 어노테이션 확인
        AsyncProcessing eventAsyncProcessing = event.getClass().getAnnotation(AsyncProcessing.class);
        if (eventAsyncProcessing != null) {
            log.debug("이벤트 클래스 AsyncProcessing 어노테이션 발견: {}, 모드: {}",
                    event.getClass().getSimpleName(), eventAsyncProcessing.mode());
        }

        // 디스패처에 위임
        try {
            if (isTransactional) {
                if (isAsync) {
                    eventDispatcher.dispatchAsyncInTransaction(event, typedListener, phase);
                } else {
                    eventDispatcher.dispatchInTransaction(event, typedListener, phase);
                }
            } else {
                if (isAsync) {
                    eventDispatcher.dispatchAsync(event, typedListener);
                } else {
                    eventDispatcher.dispatch(event, typedListener);
                }
            }

            // 멱등성 처리 완료 표시
            if (isIdempotent) {
                idempotentProcessor.markAsProcessed(event);
            }
        } catch (Exception e) {
            log.error("이벤트 처리 중 오류 발생: {}, 리스너: {}",
                    event.getClass().getSimpleName(),
                    typedListener.getClass().getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 트랜잭션 단계에서 이벤트를 특정 리스너에게 전달합니다.
     *
     * @param event    전달할 이벤트
     * @param listener 이벤트를 처리할 리스너
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> void multicastTransactional(Event event, EventListener<T> listener) {
        try {
            listener.onEvent((T) event);
        } catch (ClassCastException e) {
            throw new IllegalStateException("리스너가 처리할 수 없는 이벤트 타입입니다: " + event.getClass(), e);
        } catch (Exception e) {
            throw new RuntimeException("이벤트 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }
}
