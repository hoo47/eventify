package io.github.event.core.model;

/**
 * 비동기 처리 메커니즘을 정의하는 열거형
 */
public enum AsyncMode {
    /**
     * 로컬 스레드 풀을 사용한 비동기 처리
     */
    EXECUTOR,
    
    /**
     * RabbitMQ를 사용한 비동기 처리
     */
    RABBITMQ
}
