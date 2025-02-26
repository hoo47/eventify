package io.github.event.core.model;

/**
 * 이벤트 처리 모드를 정의하는 열거형
 */
public enum ProcessingMode {
    /**
     * 동기 처리 모드
     */
    SYNCHRONOUS,
    
    /**
     * 비동기 처리 모드
     */
    ASYNCHRONOUS,
    
    /**
     * 트랜잭션 내 처리 모드
     */
    TRANSACTIONAL,
    
    /**
     * 트랜잭션 내 비동기 처리 모드
     */
    ASYNC_TRANSACTIONAL;
    
    /**
     * 비동기 처리 여부를 반환합니다.
     */
    public boolean isAsync() {
        return this == ASYNCHRONOUS || this == ASYNC_TRANSACTIONAL;
    }
    
    /**
     * 트랜잭션 처리 여부를 반환합니다.
     */
    public boolean isTransactional() {
        return this == TRANSACTIONAL || this == ASYNC_TRANSACTIONAL;
    }
} 