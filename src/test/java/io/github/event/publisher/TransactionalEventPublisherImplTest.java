package io.github.event.publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
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
        // 활성 트랜잭션이 없는 경우 테스트
        transactionManager.setTransactionActive(false);
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMMIT);
        
        // 즉시 처리되었는지 검증
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        
        // 트랜잭션 동기화가 등록되지 않았는지 검증
        assertThat(transactionManager.getSynchronizations()).isEmpty();
    }

    @Test
    void testPublishInTransaction_BeforeCommit() {
        // BEFORE_COMMIT 단계 테스트
        publisher.publishInTransaction(testEvent, TransactionPhase.BEFORE_COMMIT);
        
        // 즉시 처리되지 않았는지 검증
        assertThat(processorCallback.getProcessedEvents()).isEmpty();
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // beforeCommit 호출 시 이벤트가 처리되는지 검증
        transactionManager.getSynchronizations().get(0).beforeCommit();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishInTransaction_AfterCommit() {
        // AFTER_COMMIT 단계 테스트
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMMIT);
        
        // 즉시 처리되지 않았는지 검증
        assertThat(processorCallback.getProcessedEvents()).isEmpty();
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterCommit 호출 시 이벤트가 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCommit();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishInTransaction_AfterRollback() {
        // AFTER_ROLLBACK 단계 테스트
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_ROLLBACK);
        
        // 즉시 처리되지 않았는지 검증
        assertThat(processorCallback.getProcessedEvents()).isEmpty();
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterRollback 호출 시 이벤트가 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterRollback();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishInTransaction_AfterCompletion() {
        // AFTER_COMPLETION 단계 테스트
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMPLETION);
        
        // 즉시 처리되지 않았는지 검증
        assertThat(processorCallback.getProcessedEvents()).isEmpty();
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterCompletion 호출 시 이벤트가 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCompletion();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishInTransaction_AfterCompletionWithCommit() {
        // AFTER_COMPLETION 단계 테스트 (커밋 후)
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMPLETION);
        
        // 즉시 처리되지 않았는지 검증
        assertThat(processorCallback.getProcessedEvents()).isEmpty();
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // 커밋 후 afterCompletion 호출 시 이벤트가 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCommit();
        transactionManager.getSynchronizations().get(0).afterCompletion();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishInTransaction_AfterCompletionWithRollback() {
        // AFTER_COMPLETION 단계 테스트 (롤백 후)
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMPLETION);
        
        // 즉시 처리되지 않았는지 검증
        assertThat(processorCallback.getProcessedEvents()).isEmpty();
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // 롤백 후 afterCompletion 호출 시 이벤트가 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterRollback();
        transactionManager.getSynchronizations().get(0).afterCompletion();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishInTransaction_WithListener() {
        // 특정 리스너에게 이벤트 발행 테스트
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMMIT, testListener);
        
        // 즉시 처리되지 않았는지 검증
        assertThat(processorCallback.getProcessedEvents()).isEmpty();
        assertThat(processorCallback.getProcessedListeners()).isEmpty();
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterCommit 호출 시 이벤트가 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCommit();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        assertThat(processorCallback.getProcessedListeners()).hasSize(1);
        assertThat(processorCallback.getProcessedListeners().get(0)).isEqualTo(testListener);
    }

    @Test
    void testPublishInTransaction_WithListener_AfterCompletion() {
        // 특정 리스너에게 AFTER_COMPLETION 단계 이벤트 발행 테스트
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMPLETION, testListener);
        
        // 즉시 처리되지 않았는지 검증
        assertThat(processorCallback.getProcessedEvents()).isEmpty();
        assertThat(processorCallback.getProcessedListeners()).isEmpty();
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterCompletion 호출 시 이벤트가 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCompletion();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        assertThat(processorCallback.getProcessedListeners()).hasSize(1);
        assertThat(processorCallback.getProcessedListeners().get(0)).isEqualTo(testListener);
    }

    @Test
    void testPublishInTransaction_WithListener_NoActiveTransaction() {
        // 활성 트랜잭션이 없는 경우 특정 리스너에게 이벤트 발행 테스트
        transactionManager.setTransactionActive(false);
        publisher.publishInTransaction(testEvent, TransactionPhase.AFTER_COMMIT, testListener);
        
        // 즉시 처리되었는지 검증
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        assertThat(processorCallback.getProcessedListeners()).hasSize(1);
        assertThat(processorCallback.getProcessedListeners().get(0)).isEqualTo(testListener);
        
        // 트랜잭션 동기화가 등록되지 않았는지 검증
        assertThat(transactionManager.getSynchronizations()).isEmpty();
    }

    @Test
    void testPublishAsyncInTransaction() {
        // 비동기 이벤트 발행 테스트
        CompletableFuture<Void> future = publisher.publishAsyncInTransaction(testEvent, TransactionPhase.AFTER_COMMIT);
        
        // Future가 완료되었는지 검증
        assertThat(future.isDone()).isTrue();
        
        // 즉시 처리되지 않았는지 검증
        assertThat(processorCallback.getProcessedEvents()).isEmpty();
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterCommit 호출 시 이벤트가 비동기적으로 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCommit();
        
        // 실행기에 작업이 등록되었는지 검증
        assertThat(executor.getExecutedRunnables()).hasSize(1);
        
        // 실행된 Runnable 실행
        executor.executeAll();
        
        // 이벤트가 처리되었는지 검증
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishAsyncInTransaction_AfterCompletion() {
        // AFTER_COMPLETION 단계 비동기 이벤트 발행 테스트
        CompletableFuture<Void> future = publisher.publishAsyncInTransaction(testEvent, TransactionPhase.AFTER_COMPLETION);
        
        // Future가 완료되었는지 검증
        assertThat(future.isDone()).isTrue();
        
        // 즉시 처리되지 않았는지 검증
        assertThat(processorCallback.getProcessedEvents()).isEmpty();
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterCompletion 호출 시 이벤트가 비동기적으로 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCompletion();
        
        // 실행기에 작업이 등록되었는지 검증
        assertThat(executor.getExecutedRunnables()).hasSize(1);
        
        // 실행된 Runnable 실행
        executor.executeAll();
        
        // 이벤트가 처리되었는지 검증
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishAsyncInTransaction_NoActiveTransaction() {
        // 활성 트랜잭션이 없는 경우 비동기 이벤트 발행 테스트
        transactionManager.setTransactionActive(false);
        CompletableFuture<Void> future = publisher.publishAsyncInTransaction(testEvent, TransactionPhase.AFTER_COMMIT);
        
        // 비동기 실행기에 작업이 등록되었는지 검증
        assertThat(executor.getExecutedRunnables()).hasSize(1);
        
        // 트랜잭션 동기화가 등록되지 않았는지 검증
        assertThat(transactionManager.getSynchronizations()).isEmpty();
        
        // 실행된 Runnable 실행
        executor.executeAll();
        
        // 이벤트가 처리되었는지 검증
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        
        // Future가 완료되었는지 검증
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCompletedExceptionally()).isFalse();
    }

    @Test
    void testPublishAsyncInTransaction_WithListener() {
        // 특정 리스너에게 비동기 이벤트 발행 테스트
        publisher.publishAsyncInTransaction(testEvent, TransactionPhase.AFTER_COMMIT, testListener);
        
        // 즉시 처리되지 않았는지 검증
        assertThat(processorCallback.getProcessedEvents()).isEmpty();
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterCommit 호출 시 이벤트가 비동기적으로 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCommit();
        assertThat(executor.getExecutedRunnables()).hasSize(1);
        
        // 실행된 Runnable 실행
        executor.executeAll();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        assertThat(processorCallback.getProcessedListeners()).hasSize(1);
        assertThat(processorCallback.getProcessedListeners().get(0)).isEqualTo(testListener);
    }

    @Test
    void testPublishAsyncInTransaction_WithListener_AfterCompletion() {
        // 특정 리스너에게 AFTER_COMPLETION 단계 비동기 이벤트 발행 테스트
        publisher.publishAsyncInTransaction(testEvent, TransactionPhase.AFTER_COMPLETION, testListener);
        
        // 즉시 처리되지 않았는지 검증
        assertThat(processorCallback.getProcessedEvents()).isEmpty();
        
        // 트랜잭션 동기화가 등록되었는지 검증
        assertThat(transactionManager.getSynchronizations()).hasSize(1);
        
        // afterCompletion 호출 시 이벤트가 비동기적으로 처리되는지 검증
        transactionManager.getSynchronizations().get(0).afterCompletion();
        assertThat(executor.getExecutedRunnables()).hasSize(1);
        
        // 실행된 Runnable 실행
        executor.executeAll();
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        assertThat(processorCallback.getProcessedListeners()).hasSize(1);
        assertThat(processorCallback.getProcessedListeners().get(0)).isEqualTo(testListener);
    }

    @Test
    void testPublishAsyncInTransaction_WithListener_NoActiveTransaction() {
        // 활성 트랜잭션이 없는 경우 특정 리스너에게 비동기 이벤트 발행 테스트
        transactionManager.setTransactionActive(false);
        publisher.publishAsyncInTransaction(testEvent, TransactionPhase.AFTER_COMMIT, testListener);
        
        // 비동기 실행기에 작업이 등록되었는지 검증
        assertThat(executor.getExecutedRunnables()).hasSize(1);
        
        // 트랜잭션 동기화가 등록되지 않았는지 검증
        assertThat(transactionManager.getSynchronizations()).isEmpty();
        
        // 실행된 Runnable 실행
        executor.executeAll();
        
        // 이벤트가 처리되었는지 검증
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
        private boolean transactionActive = true;
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
            // 테스트용 구현
        }
        
        @Override
        public void rollback(TransactionStatus status) {
            // 테스트용 구현
        }
        
        public void setTransactionActive(boolean transactionActive) {
            this.transactionActive = transactionActive;
        }
        
        public List<TransactionSynchronization> getSynchronizations() {
            return synchronizations;
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
            for (Runnable runnable : runnables) {
                runnable.run();
            }
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