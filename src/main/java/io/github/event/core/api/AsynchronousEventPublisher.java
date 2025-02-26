package io.github.event.core.api;

import java.util.concurrent.CompletableFuture;

/**
 * 비동기 이벤트 발행자 인터페이스
 */
public interface AsynchronousEventPublisher extends EventPublisher {
    /**
     * 이벤트를 비동기적으로 발행합니다.
     * 
     * @param event 발행할 이벤트
     * @return 비동기 작업의 Future
     */
    CompletableFuture<Void> publishAsync(Event event);
} 