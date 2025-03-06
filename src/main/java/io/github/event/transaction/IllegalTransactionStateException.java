package io.github.event.transaction;

/**
 * 잘못된 트랜잭션 상태일 때 발생하는 예외
 */
public class IllegalTransactionStateException extends RuntimeException {
    
    public IllegalTransactionStateException(String message) {
        super(message);
    }
    
    public IllegalTransactionStateException(String message, Throwable cause) {
        super(message, cause);
    }
} 