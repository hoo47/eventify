package io.github.event.listener;

import io.github.event.core.api.Event;
import io.github.event.core.api.EventListener;
import io.github.event.idempotent.IdempotentEventProcessor;

/**
 * 멱등성을 보장하는 이벤트 리스너 데코레이터
 */
public class IdempotentEventListener<T extends Event> implements EventListener<T> {
    
    private final EventListener<T> delegate;
    private final IdempotentEventProcessor idempotentProcessor;
    
    public IdempotentEventListener(EventListener<T> delegate, IdempotentEventProcessor idempotentProcessor) {
        this.delegate = delegate;
        this.idempotentProcessor = idempotentProcessor;
    }
    
    @Override
    public Class<T> getEventType() {
        return delegate.getEventType();
    }
    
    @Override
    public void onEvent(T event) {
        // 이미 처리된 이벤트인지 확인
        if (idempotentProcessor.isProcessed(event)) {
            // 이미 처리된 이벤트는 무시
            return;
        }
        
        try {
            // 실제 이벤트 처리
            delegate.onEvent(event);
            
            // 처리 완료 표시
            idempotentProcessor.markAsProcessed(event);
        } catch (Exception e) {
            // 예외 발생 시 처리 (재시도 정책에 따라 다름)
            throw e;
        }
    }
} 
