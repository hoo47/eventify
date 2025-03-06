package io.github.event.transaction;

/**
 * 트랜잭션 동기화를 위한 인터페이스입니다.
 * 트랜잭션의 각 단계에서 실행될 콜백을 정의합니다.
 */
public interface TransactionSynchronization {
    
    /**
     * 트랜잭션이 커밋되기 전에 호출됩니다.
     */
    default void beforeCommit() {}
    
    /**
     * 트랜잭션이 커밋된 후에 호출됩니다.
     */
    default void afterCommit() {}
    
    /**
     * 트랜잭션이 롤백되기 전에 호출됩니다.
     */
    default void beforeRollback() {}
    
    /**
     * 트랜잭션이 롤백된 후에 호출됩니다.
     */
    default void afterRollback() {}
    
    /**
     * 트랜잭션이 완료된 후에 호출됩니다.
     * 커밋이나 롤백 상관없이 항상 호출됩니다.
     */
    default void afterCompletion() {}
} 