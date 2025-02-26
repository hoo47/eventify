package io.github.event.transaction;

/**
 * 트랜잭션 상태를 나타내는 인터페이스
 */
public interface TransactionStatus {
    
    /**
     * 트랜잭션이 새로 생성된 것인지 확인합니다.
     * @return 새 트랜잭션 여부
     */
    boolean isNewTransaction();
    
    /**
     * 트랜잭션을 롤백 전용으로 표시합니다.
     */
    void setRollbackOnly();
    
    /**
     * 트랜잭션이 롤백 전용으로 표시되었는지 확인합니다.
     * @return 롤백 전용 여부
     */
    boolean isRollbackOnly();
} 