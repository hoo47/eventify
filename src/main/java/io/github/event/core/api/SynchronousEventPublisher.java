package io.github.event.core.api;

/**
 * 동기 이벤트 발행자 인터페이스
 */
public interface SynchronousEventPublisher extends EventPublisher {
    /**
     * 이벤트를 동기적으로 발행합니다.
     * 
     * @param event 발행할 이벤트
     */
    void publishSync(Event event);
} 