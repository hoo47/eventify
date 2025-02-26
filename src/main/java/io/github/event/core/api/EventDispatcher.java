package io.github.event.core.api;

import io.github.event.core.model.TransactionPhase;

/**
 * 이벤트 디스패처 인터페이스
 */
public interface EventDispatcher {
    <T extends Event> void dispatch(Event event, EventListener<T> listener);
    <T extends Event> void dispatchAsync(Event event, EventListener<T> listener);
    <T extends Event> void dispatchInTransaction(Event event, EventListener<T> listener, TransactionPhase phase);
    <T extends Event> void dispatchAsyncInTransaction(Event event, EventListener<T> listener, TransactionPhase phase);
} 