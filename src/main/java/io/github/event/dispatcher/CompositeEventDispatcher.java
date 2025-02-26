package io.github.event.dispatcher;

import io.github.event.core.api.Event;
import io.github.event.core.api.EventDispatcher;
import io.github.event.core.api.EventListener;
import io.github.event.core.model.TransactionPhase;
import lombok.RequiredArgsConstructor;

/**
 * 여러 디스패처를 조합하여 사용하는 컴포지트 디스패처
 */
@RequiredArgsConstructor
public class CompositeEventDispatcher implements EventDispatcher {
    private final DispatcherSelectionStrategy selectionStrategy;
    
    @Override
    public <T extends Event> void dispatch(Event event, EventListener<T> listener) {
        EventDispatcher dispatcher = selectionStrategy.selectDispatcher(event, listener);
        dispatcher.dispatch(event, listener);
    }
    
    @Override
    public <T extends Event> void dispatchAsync(Event event, EventListener<T> listener) {
        EventDispatcher dispatcher = selectionStrategy.selectDispatcher(event, listener);
        dispatcher.dispatchAsync(event, listener);
    }
    
    @Override
    public <T extends Event> void dispatchInTransaction(Event event, EventListener<T> listener, TransactionPhase phase) {
        EventDispatcher dispatcher = selectionStrategy.selectDispatcher(event, listener);
        dispatcher.dispatchInTransaction(event, listener, phase);
    }
    
    @Override
    public <T extends Event> void dispatchAsyncInTransaction(Event event, EventListener<T> listener, TransactionPhase phase) {
        EventDispatcher dispatcher = selectionStrategy.selectDispatcher(event, listener);
        dispatcher.dispatchAsyncInTransaction(event, listener, phase);
    }
} 
