package io.github.event.dispatcher;

import io.github.event.core.api.Event;
import io.github.event.core.api.EventDispatcher;
import io.github.event.core.api.EventListener;
import io.github.event.core.api.TransactionalEventPublisher;
import io.github.event.core.model.TransactionPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.Executor;

/**
 * 기본 이벤트 디스패처 구현
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultEventDispatcher implements EventDispatcher {
    
    private final Executor asyncExecutor;
    private final TransactionalEventPublisher transactionalPublisher;
    
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Event> void dispatch(Event event, EventListener<T> listener) {
        try {
            listener.onEvent((T) event);
            log.debug("이벤트 처리 완료: {}, 리스너: {}", 
                    event.getClass().getSimpleName(), 
                    listener.getClass().getSimpleName());
        } catch (ClassCastException e) {
            log.error("이벤트 타입 불일치: {}, 리스너: {}", 
                    event.getClass().getSimpleName(), 
                    listener.getClass().getSimpleName(), e);
            throw new IllegalStateException("리스너가 처리할 수 없는 이벤트 타입입니다: " + event.getClass(), e);
        } catch (Exception e) {
            log.error("이벤트 처리 중 오류 발생: {}, 리스너: {}", 
                    event.getClass().getSimpleName(), 
                    listener.getClass().getSimpleName(), e);
            throw new RuntimeException("이벤트 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Event> void dispatchAsync(Event event, EventListener<T> listener) {
        asyncExecutor.execute(() -> {
            try {
                listener.onEvent((T) event);
                log.debug("비동기 이벤트 처리 완료: {}, 리스너: {}", 
                        event.getClass().getSimpleName(), 
                        listener.getClass().getSimpleName());
            } catch (ClassCastException e) {
                log.error("비동기 이벤트 타입 불일치: {}, 리스너: {}", 
                        event.getClass().getSimpleName(), 
                        listener.getClass().getSimpleName(), e);
            } catch (Exception e) {
                log.error("비동기 이벤트 처리 중 오류 발생: {}, 리스너: {}", 
                        event.getClass().getSimpleName(), 
                        listener.getClass().getSimpleName(), e);
            }
        });
    }
    
    @Override
    public <T extends Event> void dispatchInTransaction(Event event, EventListener<T> listener, TransactionPhase phase) {
        transactionalPublisher.publishInTransaction(event, phase, listener);
    }
    
    @Override
    public <T extends Event> void dispatchAsyncInTransaction(Event event, EventListener<T> listener, TransactionPhase phase) {
        transactionalPublisher.publishAsyncInTransaction(event, phase, listener);
    }
} 
