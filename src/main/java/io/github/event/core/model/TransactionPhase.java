package io.github.event.core.model;

/**
 * 트랜잭션 단계를 정의하는 열거형
 */
public enum TransactionPhase {
    /**
     * 트랜잭션 커밋 전 단계
     */
    BEFORE_COMMIT,
    
    /**
     * 트랜잭션 커밋 후 단계
     */
    AFTER_COMMIT,
    
    /**
     * 트랜잭션 롤백 후 단계
     */
    AFTER_ROLLBACK,
    
    /**
     * 트랜잭션 완료 후 단계 (커밋 또는 롤백 후 항상 실행)
     */
    AFTER_COMPLETION;
    
    /**
     * 트랜잭션 단계가 커밋 관련 단계인지 확인합니다.
     * 
     * @return 커밋 관련 단계인 경우 true
     */
    public boolean isCommitPhase() {
        return this == BEFORE_COMMIT || this == AFTER_COMMIT;
    }
} 