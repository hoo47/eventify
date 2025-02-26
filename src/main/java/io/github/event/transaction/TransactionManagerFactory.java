package io.github.event.transaction;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * SPI를 통해 TransactionManager 구현체를 찾는 팩토리
 */
public class TransactionManagerFactory {
    
    /**
     * 클래스패스에서 사용 가능한 TransactionManager 구현체를 찾아 반환합니다.
     * 여러 구현체가 있는 경우 첫 번째 구현체를 반환합니다.
     * 
     * @return 발견된 TransactionManager 구현체, 없으면 NoOpTransactionManager
     */
    public static TransactionManager getTransactionManager() {
        ServiceLoader<TransactionManager> loader = ServiceLoader.load(TransactionManager.class);
        Iterator<TransactionManager> iterator = loader.iterator();
        
        if (iterator.hasNext()) {
            return iterator.next();
        }
        
        // 기본 구현체 반환
        return new NoOpTransactionManager();
    }
    
    /**
     * 트랜잭션을 지원하지 않는 기본 구현체
     */
    private static class NoOpTransactionManager implements TransactionManager {
        @Override
        public boolean isTransactionActive() {
            return false;
        }
        
        @Override
        public void registerSynchronization(TransactionSynchronization synchronization) {
            throw new UnsupportedOperationException("트랜잭션을 지원하지 않습니다");
        }
        
        @Override
        public TransactionStatus beginTransaction() {
            throw new UnsupportedOperationException("트랜잭션을 지원하지 않습니다");
        }
        
        @Override
        public void commit(TransactionStatus status) {
            throw new UnsupportedOperationException("트랜잭션을 지원하지 않습니다");
        }
        
        @Override
        public void rollback(TransactionStatus status) {
            throw new UnsupportedOperationException("트랜잭션을 지원하지 않습니다");
        }
    }
} 