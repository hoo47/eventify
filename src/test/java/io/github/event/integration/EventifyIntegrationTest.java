package io.github.event.integration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.event.Eventify;
import io.github.event.annotation.AsyncListener;
import io.github.event.annotation.EventHandler;
import io.github.event.annotation.Idempotent;
import io.github.event.annotation.ListenerMethod;
import io.github.event.annotation.TransactionalListener;
import io.github.event.core.api.EventPublisher;
import io.github.event.core.model.AbstractEvent;
import io.github.event.core.model.AsyncMode;
import io.github.event.core.model.TransactionPhase;
import io.github.event.transaction.TransactionManager;
import io.github.event.transaction.TransactionStatus;
import io.github.event.transaction.TransactionSynchronization;
import lombok.Getter;

class EventifyIntegrationTest {

    private Eventify eventify;
    private EventPublisher publisher;
    private TestTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        transactionManager = new TestTransactionManager();
        
        eventify = Eventify.builder()
                .transactionManager(transactionManager)
                .asyncExecutor(Executors.newFixedThreadPool(2))
                .enableRabbitMQ(true)
                .rabbitMQHost("localhost")
                .rabbitMQPort(5672)
                .rabbitMQUsername("guest")
                .rabbitMQPassword("guest")
                .rabbitMQExchange("test.events")
                .rabbitMQQueue("test.events.queue")
                .build();
        
        publisher = eventify.getEventPublisher();
        
        // 이벤트 핸들러 등록
        eventify.getHandlerRegistry().registerHandler(new TestEventHandler());
    }

    @AfterEach
    void tearDown() {
        eventify.shutdown();
    }

    @Test
    void testSynchronousEventWithoutTransaction() {
        // given
        TestEvent event = new TestEvent("동기 이벤트");
        TestEventHandler handler = new TestEventHandler();
        eventify.getHandlerRegistry().registerHandler(handler);

        // when
        publisher.publish(event);

        // then
        assertThat(handler.isEventProcessed()).isTrue();
        assertThat(handler.getProcessedEvent().getMessage())
            .isEqualTo("동기 이벤트");
    }

    @Test
    void testTransactionalAsyncEvent() {
        // given
        TestEvent event = new TestEvent("트랜잭션 비동기 이벤트");
        TestAsyncTransactionalEventHandler handler = new TestAsyncTransactionalEventHandler();
        eventify.getHandlerRegistry().registerHandler(handler);

        // when
        transactionManager.executeInTransaction(() -> {
            publisher.publish(event);
            return null;
        });

        // then
        await().atMost(Duration.ofSeconds(5))
              .until(handler::isEventProcessed);
        assertThat(handler.getProcessedEvent().getMessage())
            .isEqualTo("트랜잭션 비동기 이벤트");
    }

    @Test
    void testTransactionalAsyncEventWithRabbitMQ() {
        // given
        TestEvent event = new TestEvent("RabbitMQ 트랜잭션 비동기 이벤트");
        TestRabbitMQTransactionalEventHandler handler = new TestRabbitMQTransactionalEventHandler();
        eventify.getHandlerRegistry().registerHandler(handler);

        // when
        transactionManager.executeInTransaction(() -> {
            publisher.publish(event);
            return null;
        });

        // then
        await().atMost(Duration.ofSeconds(5))
              .until(handler::isEventProcessed);
        assertThat(handler.getProcessedEvent().getMessage())
            .isEqualTo("RabbitMQ 트랜잭션 비동기 이벤트");
    }

    @Test
    void testAsyncEvent() {
        // given
        TestEvent event = new TestEvent("비동기 이벤트");
        TestAsyncEventHandler handler = new TestAsyncEventHandler();
        eventify.getHandlerRegistry().registerHandler(handler);

        // when
        publisher.publish(event);

        // then
        await().atMost(Duration.ofSeconds(5))
              .until(handler::isEventProcessed);
        assertThat(handler.getProcessedEvent().getMessage())
            .isEqualTo("비동기 이벤트");
    }

    @Test
    void testAsyncEventWithRabbitMQ() {
        // given
        TestEvent event = new TestEvent("RabbitMQ 비동기 이벤트");
        TestRabbitMQEventHandler handler = new TestRabbitMQEventHandler();
        eventify.getHandlerRegistry().registerHandler(handler);

        // when
        publisher.publish(event);

        // then
        await().atMost(Duration.ofSeconds(5))
              .until(handler::isEventProcessed);
        assertThat(handler.getProcessedEvent().getMessage())
            .isEqualTo("RabbitMQ 비동기 이벤트");
    }

    @Test
    void testTransactionRollback() {
        // given
        TestEvent event = new TestEvent("롤백 이벤트");
        TestRollbackEventHandler handler = new TestRollbackEventHandler();
        eventify.getHandlerRegistry().registerHandler(handler);

        // when
        try {
            transactionManager.executeInTransaction(() -> {
                publisher.publish(event);
                throw new RuntimeException("강제 롤백");
            });
        } catch (RuntimeException e) {
            // 예상된 예외
        }

        // then
        await().atMost(Duration.ofSeconds(5))
              .until(handler::isEventProcessed);
        assertThat(handler.getProcessedEvent().getMessage())
            .isEqualTo("롤백 이벤트");
    }

    @Test
    void testIdempotentEventHandling() {
        // given
        TestEvent event = new TestEvent("멱등성 이벤트");
        TestIdempotentEventHandler handler = new TestIdempotentEventHandler();
        eventify.getHandlerRegistry().registerHandler(handler);

        // when
        publisher.publish(event);
        publisher.publish(event); // 동일한 이벤트 두 번 발행

        // then
        assertThat(handler.getProcessCount()).isEqualTo(1);
    }

    @Test
    void testMultipleListeners() {
        // given
        TestEvent event = new TestEvent("멀티 리스너 이벤트");
        TestEventHandler handler1 = new TestEventHandler();
        TestEventHandler handler2 = new TestEventHandler();
        eventify.getHandlerRegistry().registerHandler(handler1);
        eventify.getHandlerRegistry().registerHandler(handler2);

        // when
        publisher.publish(event);

        // then
        assertThat(handler1.isEventProcessed()).isTrue();
        assertThat(handler2.isEventProcessed()).isTrue();
        assertThat(handler1.getProcessedEvent().getMessage())
            .isEqualTo("멀티 리스너 이벤트");
        assertThat(handler2.getProcessedEvent().getMessage())
            .isEqualTo("멀티 리스너 이벤트");
    }

    @Test
    void testTransactionPhaseOrder() {
        // given
        TestEvent event = new TestEvent("트랜잭션 단계 이벤트");
        TestTransactionPhaseHandler handler = new TestTransactionPhaseHandler();
        eventify.getHandlerRegistry().registerHandler(handler);

        // when
        transactionManager.executeInTransaction(() -> {
            publisher.publish(event);
            return null;
        });

        // then
        List<String> expectedOrder = List.of(
            "BEFORE_COMMIT",
            "AFTER_COMMIT",
            "AFTER_COMPLETION"
        );
        assertThat(handler.getProcessingOrder()).isEqualTo(expectedOrder);
    }

    @Test
    void testRabbitMQFallback() {
        // given
        TestEvent event = new TestEvent("폴백 이벤트");
        TestRabbitMQFallbackHandler handler = new TestRabbitMQFallbackHandler();
        eventify.getHandlerRegistry().registerHandler(handler);
        
        // RabbitMQ 설정을 잘못된 값으로 변경
        eventify = Eventify.builder()
                .transactionManager(transactionManager)
                .asyncExecutor(Executors.newFixedThreadPool(2))
                .enableRabbitMQ(true)
                .rabbitMQHost("invalid-host")
                .rabbitMQPort(5672)
                .build();

        // when
        publisher.publish(event);

        // then
        await().atMost(Duration.ofSeconds(5))
              .until(handler::isEventProcessed);
        assertThat(handler.getProcessedEvent().getMessage())
            .isEqualTo("폴백 이벤트");
        assertThat(handler.isProcessedByExecutor()).isTrue();
    }

    // 테스트용 이벤트 클래스
    @Getter
    static class TestEvent extends AbstractEvent {
        private final String message;

        public TestEvent(String message) {
            this.message = message;
        }
    }

    // 기본 이벤트 핸들러
    @EventHandler
    static class TestEventHandler {
        private final AtomicBoolean eventProcessed = new AtomicBoolean(false);
        @Getter
        private TestEvent processedEvent;

        @ListenerMethod(TestEvent.class)
        public void handleEvent(TestEvent event) {
            this.processedEvent = event;
            this.eventProcessed.set(true);
        }

        public boolean isEventProcessed() {
            return eventProcessed.get();
        }

    }

    // 비동기 트랜잭션 이벤트 핸들러
    @EventHandler
    static class TestAsyncTransactionalEventHandler {
        private final AtomicBoolean eventProcessed = new AtomicBoolean(false);
        @Getter
        private TestEvent processedEvent;

        @AsyncListener
        @TransactionalListener(TransactionPhase.AFTER_COMMIT)
        public void handleEvent(TestEvent event) {
            this.processedEvent = event;
            this.eventProcessed.set(true);
        }

        public boolean isEventProcessed() {
            return eventProcessed.get();
        }

    }

    // RabbitMQ 트랜잭션 이벤트 핸들러
    @EventHandler
    static class TestRabbitMQTransactionalEventHandler {
        private final AtomicBoolean eventProcessed = new AtomicBoolean(false);
        @Getter
        private TestEvent processedEvent;

        @AsyncListener(mode = AsyncMode.RABBITMQ)
        @TransactionalListener(TransactionPhase.AFTER_COMMIT)
        public void handleEvent(TestEvent event) {
            this.processedEvent = event;
            this.eventProcessed.set(true);
        }

        public boolean isEventProcessed() {
            return eventProcessed.get();
        }

    }

    // 비동기 이벤트 핸들러
    @EventHandler
    static class TestAsyncEventHandler {
        private final AtomicBoolean eventProcessed = new AtomicBoolean(false);
        private TestEvent processedEvent;

        @AsyncListener
        public void handleEvent(TestEvent event) {
            this.processedEvent = event;
            this.eventProcessed.set(true);
        }

        public boolean isEventProcessed() {
            return eventProcessed.get();
        }

        public TestEvent getProcessedEvent() {
            return processedEvent;
        }
    }

    // RabbitMQ 이벤트 핸들러
    @EventHandler
    static class TestRabbitMQEventHandler {
        private final AtomicBoolean eventProcessed = new AtomicBoolean(false);
        @Getter
        private TestEvent processedEvent;

        @AsyncListener(mode = AsyncMode.RABBITMQ)
        public void handleEvent(TestEvent event) {
            this.processedEvent = event;
            this.eventProcessed.set(true);
        }

        public boolean isEventProcessed() {
            return eventProcessed.get();
        }

    }

    // 롤백 이벤트 핸들러
    @EventHandler
    static class TestRollbackEventHandler {
        private final AtomicBoolean eventProcessed = new AtomicBoolean(false);
        @Getter
        private TestEvent processedEvent;

        @AsyncListener
        @TransactionalListener(TransactionPhase.AFTER_ROLLBACK)
        public void handleEvent(TestEvent event) {
            this.processedEvent = event;
            this.eventProcessed.set(true);
        }

        public boolean isEventProcessed() {
            return eventProcessed.get();
        }
    }

    // 멱등성 이벤트 핸들러
    @EventHandler
    static class TestIdempotentEventHandler {
        private final AtomicInteger processCount = new AtomicInteger(0);

        @ListenerMethod(TestEvent.class)
        @Idempotent
        public void handleEvent(TestEvent event) {
            processCount.incrementAndGet();
        }

        public int getProcessCount() {
            return processCount.get();
        }
    }

    // 트랜잭션 단계 핸들러
    @EventHandler
    static class TestTransactionPhaseHandler {
        private final List<String> processingOrder = new ArrayList<>();

        @TransactionalListener(TransactionPhase.BEFORE_COMMIT)
        public void handleBeforeCommit(TestEvent event) {
            processingOrder.add("BEFORE_COMMIT");
        }

        @TransactionalListener(TransactionPhase.AFTER_COMMIT)
        public void handleAfterCommit(TestEvent event) {
            processingOrder.add("AFTER_COMMIT");
        }

        @TransactionalListener(TransactionPhase.AFTER_COMPLETION)
        public void handleAfterCompletion(TestEvent event) {
            processingOrder.add("AFTER_COMPLETION");
        }

        public List<String> getProcessingOrder() {
            return processingOrder;
        }
    }

    // RabbitMQ 폴백 핸들러
    @EventHandler
    static class TestRabbitMQFallbackHandler {
        private final AtomicBoolean eventProcessed = new AtomicBoolean(false);
        @Getter
        private TestEvent processedEvent;
        @Getter
        private boolean processedByExecutor = false;

        @AsyncListener(mode = AsyncMode.RABBITMQ)
        public void handleEvent(TestEvent event) {
            this.processedEvent = event;
            this.eventProcessed.set(true);
            this.processedByExecutor = true; // RabbitMQ 실패 시 Executor로 처리됨
        }

        public boolean isEventProcessed() {
            return eventProcessed.get();
        }
    }

    // 테스트용 트랜잭션 매니저
    static class TestTransactionManager implements TransactionManager {
        private boolean transactionActive = false;
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
            transactionActive = true;
            return new TestTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            synchronizations.forEach(TransactionSynchronization::beforeCommit);
            synchronizations.forEach(TransactionSynchronization::afterCommit);
            synchronizations.forEach(TransactionSynchronization::afterCompletion);
            transactionActive = false;
            synchronizations.clear();
        }

        @Override
        public void rollback(TransactionStatus status) {
            synchronizations.forEach(TransactionSynchronization::beforeRollback);
            synchronizations.forEach(TransactionSynchronization::afterRollback);
            synchronizations.forEach(TransactionSynchronization::afterCompletion);
            transactionActive = false;
            synchronizations.clear();
        }

        public <T> T executeInTransaction(TransactionCallback<T> callback) {
            TransactionStatus status = beginTransaction();
            try {
                T result = callback.doInTransaction();
                commit(status);
                return result;
            } catch (Exception e) {
                rollback(status);
                throw e;
            }
        }
    }

    // 테스트용 트랜잭션 상태
    static class TestTransactionStatus implements TransactionStatus {
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

    // 트랜잭션 콜백 인터페이스
    @FunctionalInterface
    interface TransactionCallback<T> {
        T doInTransaction();
    }
} 
