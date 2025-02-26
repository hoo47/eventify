package io.github.event.transaction;

/**
 * 트랜잭션 관리를 위한 인터페이스
 */
public interface TransactionManager {
    
    /**
     * 현재 활성화된 트랜잭션이 있는지 확인합니다.
     * @return 트랜잭션 활성화 여부
     */
    boolean isTransactionActive();
    
    /**
     * 현재 트랜잭션에 동기화 객체를 등록합니다.
     * @param synchronization 등록할 동기화 객체
     */
    void registerSynchronization(TransactionSynchronization synchronization);
    
    /**
     * 트랜잭션을 시작합니다.
     * @return 트랜잭션 상태 객체
     */
    TransactionStatus beginTransaction();
    
    /**
     * 트랜잭션을 커밋합니다.
     * @param status 트랜잭션 상태 객체
     */
    void commit(TransactionStatus status);
    
    /**
     * 트랜잭션을 롤백합니다.
     * @param status 트랜잭션 상태 객체
     */
    void rollback(TransactionStatus status);
} 