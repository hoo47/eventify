package io.github.event.publisher;

import io.github.event.core.api.AsynchronousEventPublisher;
import io.github.event.core.api.Event;
import io.github.event.core.api.EventProcessorCallback;
import io.github.event.core.api.SynchronousEventPublisher;
import lombok.RequiredArgsConstructor;
import java.util.concurrent.CompletableFuture;

/**
 * 기본 이벤트 발행자 구현
 */
@RequiredArgsConstructor
public class DefaultEventPublisher implements SynchronousEventPublisher, AsynchronousEventPublisher {
    
    private final EventProcessorCallback processorCallback;
    
    @Override
    public void publish(Event event) {
        processorCallback.multicast(event);
    }
    
    @Override
    public void publishSync(Event event) {
        processorCallback.multicast(event);
    }
    
    @Override
    public CompletableFuture<Void> publishAsync(Event event) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            processorCallback.multicast(event);
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }
} 
