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
} 