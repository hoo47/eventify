package io.github.event.publisher;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import io.github.event.annotation.AsyncProcessing;
import io.github.event.core.api.Event;
import io.github.event.core.api.EventListener;
import io.github.event.core.api.EventProcessorCallback;
import io.github.event.core.api.EventPublisher;
import lombok.extern.slf4j.Slf4j;

/**
 * 기본 이벤트 발행자 구현
 */
@Slf4j
public class DefaultEventPublisher implements EventPublisher {
    
    private final EventProcessorCallback processorCallback;
    private final Executor asyncExecutor;
    
    public DefaultEventPublisher(EventProcessorCallback processorCallback) {
        this(processorCallback, null);
    }
    
    public DefaultEventPublisher(EventProcessorCallback processorCallback, Executor asyncExecutor) {
        this.processorCallback = processorCallback;
        this.asyncExecutor = asyncExecutor;
    }
    
    @Override
    public void publish(Event event) {
        if (shouldProcessAsync(event)) {
            publishAsync(event);
        } else {
            publishSync(event);
        }
    }
    
    @Override
    public <T extends Event> void publish(Event event, EventListener<T> listener) {
        if (shouldProcessAsync(event, listener)) {
            publishAsync(event, listener);
        } else {
            publishSync(event, listener);
        }
    }
    
    private void publishSync(Event event) {
        try {
            processorCallback.multicast(event);
        } catch (Exception e) {
            log.error("이벤트 처리 중 오류 발생: {}", event, e);
            throw e;
        }
    }
    
    private <T extends Event> void publishSync(Event event, EventListener<T> listener) {
        try {
            processorCallback.processEvent(event, listener);
        } catch (Exception e) {
            log.error("이벤트 처리 중 오류 발생: {}", event, e);
            throw e;
        }
    }
    
    private void publishAsync(Event event) {
        if (asyncExecutor == null) {
            throw new IllegalStateException("비동기 실행기가 설정되지 않았습니다");
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                processorCallback.multicast(event);
            } catch (Exception e) {
                log.error("비동기 이벤트 처리 중 오류 발생: {}", event, e);
                throw e;
            }
        }, asyncExecutor);
    }
    
    private <T extends Event> void publishAsync(Event event, EventListener<T> listener) {
        if (asyncExecutor == null) {
            throw new IllegalStateException("비동기 실행기가 설정되지 않았습니다");
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                processorCallback.processEvent(event, listener);
            } catch (Exception e) {
                log.error("비동기 이벤트 처리 중 오류 발생: {}", event, e);
                throw e;
            }
        }, asyncExecutor);
    }
    
    private boolean shouldProcessAsync(Event event) {
        // 이벤트 클래스에 @AsyncProcessing 어노테이션이 있는지 확인
        return event.getClass().isAnnotationPresent(AsyncProcessing.class);
    }
    
    private boolean shouldProcessAsync(Event event, EventListener<?> listener) {
        // 이벤트나 리스너 클래스에 @AsyncProcessing 어노테이션이 있는지 확인
        return shouldProcessAsync(event) || 
               listener.getClass().isAnnotationPresent(AsyncProcessing.class);
    }
} 
