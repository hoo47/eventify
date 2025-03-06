package io.github.event.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

class JdbcTransactionManagerTest {

    private DataSource dataSource;
    private JdbcTransactionManager transactionManager;
    private List<String> synchronizationEvents;

    @BeforeEach
    void setUp() {
        // H2 데이터베이스 설정
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setAutoCommit(false);
        
        dataSource = new HikariDataSource(config);
        transactionManager = new JdbcTransactionManager(dataSource);
        synchronizationEvents = new ArrayList<>();
        
        // 테스트 테이블 생성
        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(255))");
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("테이블 생성 실패", e);
        }
    }

    @AfterEach
    void tearDown() {
        // 테스트 테이블 삭제
        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS test_table");
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("테이블 삭제 실패", e);
        }
        
        // 데이터소스 종료
        ((HikariDataSource) dataSource).close();
    }

    @Test
    void testBeginTransaction() throws SQLException {
        // 트랜잭션 시작
        TransactionStatus status = transactionManager.beginTransaction();
        
        // 트랜잭션 상태 검증
        assertThat(status).isNotNull();
        assertThat(status.isNewTransaction()).isTrue();
        assertThat(status.isRollbackOnly()).isFalse();
        assertThat(transactionManager.isTransactionActive()).isTrue();
        
        // 정리
        transactionManager.rollback(status);
    }

    @Test
    void testCommit() throws SQLException {
        // 트랜잭션 시작
        TransactionStatus status = transactionManager.beginTransaction();
        
        // 데이터 삽입
        Connection conn = transactionManager.getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO test_table (id, name) VALUES (1, 'test')");
        }
        
        // 트랜잭션 커밋
        transactionManager.commit(status);
        
        // 데이터가 실제로 삽입되었는지 확인
        try (Connection checkConn = dataSource.getConnection();
             Statement stmt = checkConn.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM test_table WHERE id = 1");
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }

    @Test
    void testRollback() throws SQLException {
        // 트랜잭션 시작
        TransactionStatus status = transactionManager.beginTransaction();
        
        // 데이터 삽입
        Connection conn = transactionManager.getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO test_table (id, name) VALUES (1, 'test')");
        }
        
        // 트랜잭션 롤백
        transactionManager.rollback(status);
        
        // 데이터가 롤백되었는지 확인
        try (Connection checkConn = dataSource.getConnection();
             Statement stmt = checkConn.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM test_table WHERE id = 1");
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(0);
        }
    }

    @Test
    void testTransactionSynchronization() throws SQLException {
        // 트랜잭션 동기화 객체 생성
        TransactionSynchronization synchronization = new TransactionSynchronization() {
            @Override
            public void beforeCommit() {
                synchronizationEvents.add("beforeCommit");
            }
            
            @Override
            public void afterCommit() {
                synchronizationEvents.add("afterCommit");
            }
            
            @Override
            public void afterRollback() {
                synchronizationEvents.add("afterRollback");
            }
            
            @Override
            public void afterCompletion() {
                synchronizationEvents.add("afterCompletion");
            }
        };
        
        // 트랜잭션 시작 및 동기화 등록
        TransactionStatus status = transactionManager.beginTransaction();
        transactionManager.registerSynchronization(synchronization);
        
        // 트랜잭션 커밋
        transactionManager.commit(status);
        
        // 동기화 이벤트 순서 검증
        assertThat(synchronizationEvents).containsExactly(
            "beforeCommit",
            "afterCommit",
            "afterCompletion"
        );
    }

    @Test
    void testTransactionSynchronization_Rollback() throws SQLException {
        // 트랜잭션 동기화 객체 생성
        TransactionSynchronization synchronization = new TransactionSynchronization() {
            @Override
            public void beforeCommit() {
                synchronizationEvents.add("beforeCommit");
            }
            
            @Override
            public void afterCommit() {
                synchronizationEvents.add("afterCommit");
            }
            
            @Override
            public void afterRollback() {
                synchronizationEvents.add("afterRollback");
            }
            
            @Override
            public void afterCompletion() {
                synchronizationEvents.add("afterCompletion");
            }
        };
        
        // 트랜잭션 시작 및 동기화 등록
        TransactionStatus status = transactionManager.beginTransaction();
        transactionManager.registerSynchronization(synchronization);
        
        // 트랜잭션 롤백
        transactionManager.rollback(status);
        
        // 동기화 이벤트 순서 검증
        assertThat(synchronizationEvents).containsExactly(
            "afterRollback",
            "afterCompletion"
        );
    }

    @Test
    void testRollbackOnly() throws SQLException {
        // 트랜잭션 시작
        TransactionStatus status = transactionManager.beginTransaction();
        
        // 롤백 전용으로 설정
        status.setRollbackOnly();
        
        // 상태 검증
        assertThat(status.isRollbackOnly()).isTrue();
        
        // 커밋 시도 시 예외 발생
        assertThatThrownBy(() -> transactionManager.commit(status))
            .isInstanceOf(IllegalTransactionStateException.class)
            .hasMessageContaining("트랜잭션이 롤백 전용으로 설정되어 있습니다");
            
        // 정리
        transactionManager.rollback(status);
    }

    @Test
    void testNestedTransactions() throws SQLException {
        // 외부 트랜잭션 시작
        TransactionStatus outerStatus = transactionManager.beginTransaction();
        
        try {
            // 중첩 트랜잭션 시작 시도
            assertThatThrownBy(() -> transactionManager.beginTransaction())
                .isInstanceOf(IllegalTransactionStateException.class)
                .hasMessageContaining("이미 활성화된 트랜잭션이 있습니다");
        } finally {
            // 정리
            transactionManager.rollback(outerStatus);
        }
    }
} 