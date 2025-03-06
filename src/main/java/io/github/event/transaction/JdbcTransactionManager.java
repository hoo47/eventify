package io.github.event.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * JDBC 기반 트랜잭션 관리자
 */
@Slf4j
public class JdbcTransactionManager implements TransactionManager {
    
    private final DataSource dataSource;
    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    private final ThreadLocal<List<TransactionSynchronization>> synchronizations = new ThreadLocal<>();
    
    public JdbcTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public TransactionStatus beginTransaction() {
        if (isTransactionActive()) {
            throw new IllegalTransactionStateException("이미 활성화된 트랜잭션이 있습니다");
        }
        
        try {
            // 새로운 커넥션 생성 및 트랜잭션 시작
            Connection conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            connectionHolder.set(conn);
            synchronizations.set(new ArrayList<>());
            
            return new JdbcTransactionStatus();
        } catch (SQLException e) {
            throw new RuntimeException("트랜잭션 시작 실패", e);
        }
    }
    
    @Override
    public void commit(TransactionStatus status) {
        if (!isTransactionActive()) {
            throw new IllegalTransactionStateException("활성화된 트랜잭션이 없습니다");
        }
        
        if (status.isRollbackOnly()) {
            throw new IllegalTransactionStateException("트랜잭션이 롤백 전용으로 설정되어 있습니다");
        }
        
        Connection conn = getConnection();
        List<TransactionSynchronization> syncList = getSynchronizations();
        
        try {
            // beforeCommit 콜백 호출
            for (TransactionSynchronization sync : syncList) {
                sync.beforeCommit();
            }
            
            // 트랜잭션 커밋
            conn.commit();
            
            // afterCommit 콜백 호출
            for (TransactionSynchronization sync : syncList) {
                sync.afterCommit();
            }
        } catch (SQLException e) {
            // 커밋 실패 시 롤백
            try {
                conn.rollback();
                // 롤백 후 콜백 호출
                for (TransactionSynchronization sync : syncList) {
                    sync.afterRollback();
                }
            } catch (SQLException ex) {
                log.error("롤백 실패", ex);
            }
            throw new RuntimeException("트랜잭션 커밋 실패", e);
        } finally {
            try {
                // afterCompletion 콜백 호출
                for (TransactionSynchronization sync : syncList) {
                    sync.afterCompletion();
                }
                
                // 리소스 정리
                conn.close();
                connectionHolder.remove();
                synchronizations.remove();
            } catch (SQLException e) {
                log.error("커넥션 정리 실패", e);
            }
        }
    }
    
    @Override
    public void rollback(TransactionStatus status) {
        if (!isTransactionActive()) {
            throw new IllegalTransactionStateException("활성화된 트랜잭션이 없습니다");
        }
        
        Connection conn = getConnection();
        List<TransactionSynchronization> syncList = getSynchronizations();
        
        try {
            // 트랜잭션 롤백
            conn.rollback();
            
            // afterRollback 콜백 호출
            for (TransactionSynchronization sync : syncList) {
                sync.afterRollback();
            }
        } catch (SQLException e) {
            throw new RuntimeException("트랜잭션 롤백 실패", e);
        } finally {
            try {
                // afterCompletion 콜백 호출
                for (TransactionSynchronization sync : syncList) {
                    sync.afterCompletion();
                }
                
                // 리소스 정리
                conn.close();
                connectionHolder.remove();
                synchronizations.remove();
            } catch (SQLException e) {
                log.error("커넥션 정리 실패", e);
            }
        }
    }
    
    @Override
    public boolean isTransactionActive() {
        return connectionHolder.get() != null;
    }
    
    @Override
    public void registerSynchronization(TransactionSynchronization synchronization) {
        if (!isTransactionActive()) {
            throw new IllegalTransactionStateException("활성화된 트랜잭션이 없습니다");
        }
        getSynchronizations().add(synchronization);
    }
    
    /**
     * 현재 트랜잭션의 JDBC 커넥션을 반환합니다.
     */
    public Connection getConnection() {
        Connection conn = connectionHolder.get();
        if (conn == null) {
            throw new IllegalTransactionStateException("활성화된 트랜잭션이 없습니다");
        }
        return conn;
    }
    
    private List<TransactionSynchronization> getSynchronizations() {
        List<TransactionSynchronization> syncList = synchronizations.get();
        if (syncList == null) {
            throw new IllegalTransactionStateException("활성화된 트랜잭션이 없습니다");
        }
        return syncList;
    }
    
    /**
     * JDBC 트랜잭션 상태 클래스
     */
    private static class JdbcTransactionStatus implements TransactionStatus {
        private boolean rollbackOnly = false;
        
        @Override
        public boolean isNewTransaction() {
            return true;
        }
        
        @Override
        public void setRollbackOnly() {
            this.rollbackOnly = true;
        }
        
        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly;
        }
    }
} 
