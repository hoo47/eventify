package io.github.event.core.api;

/**
 * 이벤트 처리 콜백 인터페이스
 * ApplicationMulticaster와 TransactionalEventPublisherImpl 간의 순환 의존성을 해결하기 위한 인터페이스
 */
public interface EventProcessorCallback {
    /**
     * 이벤트를 처리합니다.
     * 
     * @param event 처리할 이벤트
     * @param listener 이벤트를 처리할 리스너
     */
    <T extends Event> void processEvent(Event event, EventListener<?> listener);
    
    /**
     * 이벤트를 모든 해당 리스너에게 전달합니다.
     * 
     * @param event 전달할 이벤트
     */
    void multicast(Event event);
} 