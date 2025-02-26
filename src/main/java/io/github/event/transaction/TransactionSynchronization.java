package io.github.event.transaction;

/**
 * 트랜잭션 단계별 콜백을 정의하는 인터페이스
 */
public interface TransactionSynchronization {
    
    /**
     * 트랜잭션 커밋 전에 호출됩니다.
     */
    default void beforeCommit() {}
    
    /**
     * 트랜잭션 커밋 후에 호출됩니다.
     */
    default void afterCommit() {}
    
    /**
     * 트랜잭션 롤백 후에 호출됩니다.
     */
    default void afterRollback() {}
    
    /**
     * 트랜잭션 완료 후(커밋 또는 롤백)에 호출됩니다.
     */
    default void afterCompletion() {}
} 