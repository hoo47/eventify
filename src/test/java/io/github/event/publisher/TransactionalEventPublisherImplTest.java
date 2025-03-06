package io.github.event.publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.event.core.api.Event;
import io.github.event.core.api.EventListener;
import io.github.event.core.api.EventProcessorCallback;
import io.github.event.core.model.AbstractEvent;
import io.github.event.core.model.TransactionPhase;
import io.github.event.transaction.TransactionManager;
import io.github.event.transaction.TransactionStatus;
import io.github.event.transaction.TransactionSynchronization;

class TransactionalEventPublisherImplTest {

    private TestEventProcessorCallback processorCallback;
    private TestTransactionManager transactionManager;
    private TestExecutor executor;
    private TransactionalEventPublisherImpl publisher;
    private TestEvent testEvent;
    private TestEventListener testListener;

    @BeforeEach
    void setUp() {
        processorCallback = new TestEventProcessorCallback();
        transactionManager = new TestTransactionManager();
        executor = new TestExecutor();
        publisher = new TransactionalEventPublisherImpl(processorCallback, transactionManager, executor);
        testEvent = new TestEvent("test-message");
        testListener = new TestEventListener();
    }

    @Test
    void testPublishInTransaction_NoActiveTransaction() {
        // 트랜잭션이 활성화되지 않은 상태에서 이벤트 발행 테스트
        transactionManager.setTransactionActive(false);
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMMIT);
        
        // 이벤트가 즉시 처리되었는지 검증
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        assertThat(transactionManager.getSynchronizations()).isEmpty();
    }

    @Test
    void testPublishInTransaction_BeforeCommit() {
        // 커밋 전 단계에서 이벤트 발행 테스트
        publisher.publishInTransaction(testEvent, TransactionPhase.BEFORE_COMMIT);
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // beforeCommit 호출 시 이벤트가 처리되는지 검증
        transactionManager.getSynchronizations().get(0).beforeCommit();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishInTransaction_AfterCommit() {
        // 커밋 후 단계에서 이벤트 발행 테스트
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMMIT);
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterCommit 호출 시 이벤트가 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCommit();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishInTransaction_AfterRollback() {
        // 롤백 후 단계에서 이벤트 발행 테스트
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_ROLLBACK);
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterRollback 호출 시 이벤트가 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterRollback();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishInTransaction_AfterCompletion() {
        // 완료 후 단계에서 이벤트 발행 테스트
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMPLETION);
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterCompletion 호출 시 이벤트가 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCompletion();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishInTransaction_AfterCompletionWithCommit() {
        // 커밋 후 완료 단계에서 이벤트 발행 테스트
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMPLETION);
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterCommit과 afterCompletion 호출 시 이벤트가 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCommit();
        transactionManager.getSynchronizations().get(0).afterCompletion();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishInTransaction_AfterCompletionWithRollback() {
        // 롤백 후 완료 단계에서 이벤트 발행 테스트
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMPLETION);
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterRollback과 afterCompletion 호출 시 이벤트가 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterRollback();
        transactionManager.getSynchronizations().get(0).afterCompletion();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishInTransaction_WithListener() {
        // 리스너와 함께 이벤트 발행 테스트
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMMIT, testListener);
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterCommit 호출 시 이벤트가 리스너에 의해 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCommit();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        assertThat(processorCallback.getProcessedListeners()).hasSize(1);
        assertThat(processorCallback.getProcessedListeners().get(0)).isEqualTo(testListener);
    }

    @Test
    void testPublishInTransaction_WithListener_AfterCompletion() {
        // 완료 단계에서 리스너와 함께 이벤트 발행 테스트
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMPLETION, testListener);
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterCompletion 호출 시 이벤트가 리스너에 의해 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCompletion();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        assertThat(processorCallback.getProcessedListeners()).hasSize(1);
        assertThat(processorCallback.getProcessedListeners().get(0)).isEqualTo(testListener);
    }

    @Test
    void testPublishInTransaction_WithListener_NoActiveTransaction() {
        // 트랜잭션이 활성화되지 않은 상태에서 리스너와 함께 이벤트 발행 테스트
        transactionManager.setTransactionActive(false);
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMMIT, testListener);
        
        // 이벤트가 즉시 리스너에 의해 처리되었는지 검증
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        assertThat(processorCallback.getProcessedListeners()).hasSize(1);
        assertThat(processorCallback.getProcessedListeners().get(0)).isEqualTo(testListener);
        assertThat(transactionManager.getSynchronizations()).isEmpty();
    }

    @Test
    void testPublishAsyncInTransaction() {
        // 비동기 이벤트 발행 테스트
        CompletableFuture<Void> future = publisher.publishAsyncInTransaction(testEvent, TransactionPhase.AFTER_COMMIT);
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        assertThat(future).isCompleted();
        
        // afterCommit 호출 시 이벤트가 비동기로 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCommit();
        assertThat(executor.getExecutedRunnables()).hasSize(1);
        
        // 비동기 작업 실행
        executor.executeAll();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishAsyncInTransaction_AfterCompletion() {
        // 완료 단계에서 비동기 이벤트 발행 테스트
        CompletableFuture<Void> future = publisher.publishAsyncInTransaction(testEvent, TransactionPhase.AFTER_COMPLETION);
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        assertThat(future).isCompleted();
        
        // afterCompletion 호출 시 이벤트가 비동기로 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCompletion();
        assertThat(executor.getExecutedRunnables()).hasSize(1);
        
        // 비동기 작업 실행
        executor.executeAll();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishAsyncInTransaction_NoActiveTransaction() {
        // 트랜잭션이 활성화되지 않은 상태에서 비동기 이벤트 발행 테스트
        transactionManager.setTransactionActive(false);
        CompletableFuture<Void> future = publisher.publishAsyncInTransaction(testEvent, TransactionPhase.AFTER_COMMIT);
        
        // 이벤트가 즉시 비동기로 처리되었는지 검증
        assertThat(executor.getExecutedRunnables()).hasSize(1);
        assertThat(transactionManager.getSynchronizations()).isEmpty();
        
        // 비동기 작업 실행
        executor.executeAll();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        
        // Future가 완료되었는지 검증
        assertThat(future).isCompleted();
    }

    @Test
    void testPublishAsyncInTransaction_WithListener() {
        // 리스너와 함께 비동기 이벤트 발행 테스트
        publisher.publishAsyncInTransaction(testEvent, TransactionPhase.AFTER_COMMIT, testListener);
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterCommit 호출 시 이벤트가 비동기로 리스너에 의해 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCommit();
        assertThat(executor.getExecutedRunnables()).hasSize(1);
        
        // 비동기 작업 실행
        executor.executeAll();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        assertThat(processorCallback.getProcessedListeners()).hasSize(1);
        assertThat(processorCallback.getProcessedListeners().get(0)).isEqualTo(testListener);
    }

    @Test
    void testPublishAsyncInTransaction_WithListener_AfterCompletion() {
        // 완료 단계에서 리스너와 함께 비동기 이벤트 발행 테스트
        publisher.publishAsyncInTransaction(testEvent, TransactionPhase.AFTER_COMPLETION, testListener);
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterCompletion 호출 시 이벤트가 비동기로 리스너에 의해 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCompletion();
        assertThat(executor.getExecutedRunnables()).hasSize(1);
        
        // 비동기 작업 실행
        executor.executeAll();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        assertThat(processorCallback.getProcessedListeners()).hasSize(1);
        assertThat(processorCallback.getProcessedListeners().get(0)).isEqualTo(testListener);
    }

    @Test
    void testPublishAsyncInTransaction_WithListener_NoActiveTransaction() {
        // 트랜잭션이 활성화되지 않은 상태에서 리스너와 함께 비동기 이벤트 발행 테스트
        transactionManager.setTransactionActive(false);
        publisher.publishAsyncInTransaction(testEvent, TransactionPhase.AFTER_COMMIT, testListener);
        
        // 이벤트가 즉시 비동기로 리스너에 의해 처리되었는지 검증
        assertThat(executor.getExecutedRunnables()).hasSize(1);
        assertThat(transactionManager.getSynchronizations()).isEmpty();
        
        // 비동기 작업 실행
        executor.executeAll();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        assertThat(processorCallback.getProcessedListeners()).hasSize(1);
        assertThat(processorCallback.getProcessedListeners().get(0)).isEqualTo(testListener);
    }

    // 테스트용 이벤트 프로세서 콜백 클래스
    static class TestEventProcessorCallback implements EventProcessorCallback {
        private final List<Event> processedEvents = new ArrayList<>();
        private final List<EventListener<?>> processedListeners = new ArrayList<>();
        
        @Override
        public void multicast(Event event) {
            processedEvents.add(event);
        }
        
        @Override
        public <T extends Event> void processEvent(Event event, EventListener<?> listener) {
            processedEvents.add(event);
            processedListeners.add(listener);
        }
        
        public List<Event> getProcessedEvents() {
            return processedEvents;
        }
        
        public List<EventListener<?>> getProcessedListeners() {
            return processedListeners;
        }
        
        public void clearProcessedEvents() {
            processedEvents.clear();
            processedListeners.clear();
        }
    }
    
    // 테스트용 트랜잭션 관리자 클래스
    static class TestTransactionManager implements TransactionManager {
        @Setter
        private boolean transactionActive = true;
        @Getter
        private final List<TransactionSynchronization> synchronizations = new ArrayList<>();
        
        @Override
        public boolean isTransactionActive() {
            return transactionActive;
        }
        
        @Override
        public void registerSynchronization(TransactionSynchronization synchronization) {
            synchronizations.add(synchronization);
        }
        
        @Override
        public TransactionStatus beginTransaction() {
            return new TestTransactionStatus();
        }
        
        @Override
        public void commit(TransactionStatus status) {
            // 테스트용 메서드
        }
        
        @Override
        public void rollback(TransactionStatus status) {
            // 테스트용 메서드
        }

        public void clearSynchronizations() {
            synchronizations.clear();
        }
    }
    
    // 테스트용 트랜잭션 상태 클래스
    static class TestTransactionStatus implements TransactionStatus {
        private boolean rollbackOnly = false;
        
        @Override
        public boolean isNewTransaction() {
            return true;
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
    
    // 테스트용 실행기 클래스
    static class TestExecutor implements Executor {
        private final List<Runnable> executedRunnables = new ArrayList<>();
        
        @Override
        public void execute(Runnable command) {
            executedRunnables.add(command);
        }
        
        public List<Runnable> getExecutedRunnables() {
            return executedRunnables;
        }
        
        public void executeAll() {
            List<Runnable> runnables = new ArrayList<>(executedRunnables);
            executedRunnables.clear();
            runnables.forEach(Runnable::run);
        }
        
        public void clearExecutedRunnables() {
            executedRunnables.clear();
        }
    }
    
    // 테스트용 이벤트 클래스
    static class TestEvent extends AbstractEvent {
        private final String message;
        
        public TestEvent(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return "TestEvent{" +
                    "eventId='" + getEventId() + '\'' +
                    ", message='" + message + '\'' +
                    ", issuedAt=" + getIssuedAt() +
                    '}';
        }
    }
    
    // 테스트용 이벤트 리스너 클래스
    static class TestEventListener implements EventListener<TestEvent> {
        private boolean eventHandled = false;
        
        @Override
        public void onEvent(TestEvent event) {
            eventHandled = true;
        }
        
        @Override
        public Class<TestEvent> getEventType() {
            return TestEvent.class;
        }
        
        public boolean isEventHandled() {
            return eventHandled;
        }
        
        public void reset() {
            eventHandled = false;
        }
    }
} 
