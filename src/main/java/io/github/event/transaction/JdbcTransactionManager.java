package io.github.event.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

public class JdbcTransactionManager implements TransactionManager {
    
    private final DataSource dataSource;
    private final ThreadLocal<JdbcTransactionStatus> currentTransaction = new ThreadLocal<>();
    private final ThreadLocal<List<TransactionSynchronization>> synchronizations = 
            ThreadLocal.withInitial(ArrayList::new);
    
    public JdbcTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public boolean isTransactionActive() {
        return currentTransaction.get() != null;
    }
    
    @Override
    public void registerSynchronization(TransactionSynchronization synchronization) {
        if (!isTransactionActive()) {
            throw new IllegalStateException("활성화된 트랜잭션이 없습니다");
        }
        synchronizations.get().add(synchronization);
    }
    
    @Override
    public TransactionStatus beginTransaction() {
        try {
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            JdbcTransactionStatus status = new JdbcTransactionStatus(connection);
            currentTransaction.set(status);
            return status;
        } catch (SQLException e) {
            throw new RuntimeException("트랜잭션 시작 중 오류 발생", e);
        }
    }
    
    @Override
    public void commit(TransactionStatus status) {
        if (!(status instanceof JdbcTransactionStatus)) {
            throw new IllegalArgumentException("JdbcTransactionStatus 타입이 아닙니다");
        }
        
        JdbcTransactionStatus jdbcStatus = (JdbcTransactionStatus) status;
        Connection connection = jdbcStatus.getConnection();
        
        try {
            if (!status.isRollbackOnly()) {
                // 커밋 전 콜백 호출
                for (TransactionSynchronization sync : synchronizations.get()) {
                    sync.beforeCommit();
                }
                
                connection.commit();
                
                // 커밋 후 콜백 호출
                for (TransactionSynchronization sync : synchronizations.get()) {
                    sync.afterCommit();
                }
            } else {
                connection.rollback();
                
                // 롤백 후 콜백 호출
                for (TransactionSynchronization sync : synchronizations.get()) {
                    sync.afterRollback();
                }
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
                // 롤백 후 콜백 호출
                for (TransactionSynchronization sync : synchronizations.get()) {
                    sync.afterRollback();
                }
            } catch (SQLException ex) {
                throw new RuntimeException("롤백 중 오류 발생", ex);
            }
            throw new RuntimeException("트랜잭션 커밋 중 오류 발생", e);
        } finally {
            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException("연결 정리 중 오류 발생", e);
            }
            
            // 완료 콜백 호출
            for (TransactionSynchronization sync : synchronizations.get()) {
                sync.afterCompletion();
            }
            
            currentTransaction.remove();
            synchronizations.remove();
        }
    }
    
    @Override
    public void rollback(TransactionStatus status) {
        if (!(status instanceof JdbcTransactionStatus)) {
            throw new IllegalArgumentException("JdbcTransactionStatus 타입이 아닙니다");
        }
        
        JdbcTransactionStatus jdbcStatus = (JdbcTransactionStatus) status;
        Connection connection = jdbcStatus.getConnection();
        
        try {
            connection.rollback();
            
            // 롤백 후 콜백 호출
            for (TransactionSynchronization sync : synchronizations.get()) {
                sync.afterRollback();
            }
        } catch (SQLException e) {
            throw new RuntimeException("트랜잭션 롤백 중 오류 발생", e);
        } finally {
            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException("연결 정리 중 오류 발생", e);
            }
            
            // 완료 콜백 호출
            for (TransactionSynchronization sync : synchronizations.get()) {
                sync.afterCompletion();
            }
            
            currentTransaction.remove();
            synchronizations.remove();
        }
    }
    
    private static class JdbcTransactionStatus implements TransactionStatus {
        private final Connection connection;
        private boolean rollbackOnly = false;
        
        JdbcTransactionStatus(Connection connection) {
            this.connection = connection;
        }
        
        Connection getConnection() {
            return connection;
        }
        
        @Override
        public boolean isNewTransaction() {
            return true; // JDBC에서는 항상 새 트랜잭션
        }
        
        @Override
        public void setRollbackOnly() {
            rollbackOnly = true;
        }
        
        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly;
        }
    }
} 
