package io.github.event.core.api;

/**
 * 이벤트를 발행하는 인터페이스
 */
public interface EventPublisher {
    
    /**
     * 이벤트를 발행합니다.
     * 실제 처리 방식(동기/비동기, 트랜잭션 여부)은 리스너에 의해 결정됩니다.
     * 
     * @param event 발행할 이벤트
     */
    void publish(Event event);
    
    /**
     * 이벤트를 특정 리스너에게 발행합니다.
     * 이벤트나 리스너의 설정에 따라 동기 또는 비동기로 처리됩니다.
     *
     * @param event 발행할 이벤트
     * @param listener 이벤트를 처리할 리스너
     */
    <T extends Event> void publish(Event event, EventListener<T> listener);
} 
