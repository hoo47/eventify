package io.github.event.dispatcher;

import io.github.event.core.api.Event;
import io.github.event.core.api.EventDispatcher;
import io.github.event.core.api.EventListener;
import io.github.event.core.model.AsyncEventWrapper;
import io.github.event.core.model.TransactionPhase;
import io.github.event.rabbitmq.RabbitMQEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RabbitMQ 이벤트 디스패처
 */
@Slf4j
@RequiredArgsConstructor
public class RabbitMQEventDispatcher implements EventDispatcher {
     
    private final RabbitMQEventPublisher rabbitMQPublisher;
    private final DefaultEventDispatcher fallbackDispatcher;
    
    @Override
    public <T extends Event> void dispatch(Event event, EventListener<T> listener) {
        // 동기 처리는 항상 로컬에서 처리
        fallbackDispatcher.dispatch(event, listener);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Event> void dispatchAsync(Event event, EventListener<T> listener) {
        // RabbitMQ를 통한 비동기 처리
        AsyncEventWrapper<T> wrapper = new AsyncEventWrapper<>((T) event, listener);
        rabbitMQPublisher.publishAsyncEvent(wrapper);
    }
    
    @Override
    public <T extends Event> void dispatchInTransaction(Event event, EventListener<T> listener, TransactionPhase phase) {
        // 동기 트랜잭션 처리는 항상 로컬에서 처리
        fallbackDispatcher.dispatchInTransaction(event, listener, phase);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Event> void dispatchAsyncInTransaction(Event event, EventListener<T> listener, TransactionPhase phase) {
        // RabbitMQ를 통한 비동기 트랜잭션 처리
        AsyncEventWrapper<T> wrapper = new AsyncEventWrapper<>((T) event, listener, phase);
        rabbitMQPublisher.publishAsyncEvent(wrapper, phase);
    }
} 
