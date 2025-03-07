package io.github.event.integration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import io.github.event.annotations.Async;
import io.github.event.annotations.EventListener;
import io.github.event.annotations.TransactionalEventListener;
import io.github.event.annotations.TransactionalPhase;
import io.github.event.async.AsyncExecutor;
import io.github.event.publisher.ApplicationEventPublisher;
import io.github.event.publisher.EventPublisherFactory;
import io.github.event.publisher.TransactionalEventPublisher;
import io.github.event.registry.EventRegistry;
import io.github.event.transaction.DummyTransactionManager;
import lombok.Getter;

public class IntegrationTest {

    // 1. Synchronous event flow test
    static class SyncEvent { }

    @Getter
    static class SyncListener {
        private boolean invoked = false;

        @EventListener
        public void onSyncEvent(SyncEvent event) {
            invoked = true;
        }

    }

    @Test
    public void testSynchronousEventFlow() {
        EventRegistry registry = new EventRegistry();
        SyncListener listener = new SyncListener();
        registry.register(listener);

        // No transaction, no async executor
        ApplicationEventPublisher publisher = EventPublisherFactory.createEventPublisher(registry, null, null);
        publisher.publish(new SyncEvent());

        assertThat(listener.isInvoked())
            .as("Synchronous listener should be invoked immediately")
            .isTrue();
    }

    // 2. Transactional event flow test
    static class TxEvent { }

    @Getter
    static class TxListener {
        private boolean invoked = false;

        @EventListener
        public void onTxEvent(TxEvent event) {
            invoked = true;
        }

    }

    @Test
    public void testTransactionalEventFlow() {
        DummyTransactionManager dtm = new DummyTransactionManager();
        dtm.begin(); // Transaction is active

        EventRegistry registry = new EventRegistry();
        TxListener listener = new TxListener();
        registry.register(listener);

        ApplicationEventPublisher publisher = EventPublisherFactory.createEventPublisher(registry, dtm, null);
        publisher.publish(new TxEvent());

        // Since transaction is active, event should be delayed
        assertThat(listener.isInvoked())
            .as("Listener should not be invoked during an active transaction")
            .isFalse();

        dtm.commit(); // End transaction
        // Flush delayed events
        ((TransactionalEventPublisher) publisher).flush();

        assertThat(listener.isInvoked())
            .as("Listener should be invoked after flush when transaction is not active")
            .isTrue();
    }

    // 3. Asynchronous event flow test
    static class AsyncEvent { }

    @Getter
    static class AsyncListener {
        private boolean invoked = false;

        @EventListener
        @Async
        public void onAsyncEvent(AsyncEvent event) {
            invoked = true;
        }

    }

    @Test
    public void testAsynchronousEventFlow() throws InterruptedException {
        EventRegistry registry = new EventRegistry();
        AsyncListener listener = new AsyncListener();
        registry.register(listener);

        AsyncExecutor asyncExecutor = new AsyncExecutor();
        ApplicationEventPublisher publisher = EventPublisherFactory.createEventPublisher(registry, null, asyncExecutor);
        publisher.publish(new AsyncEvent());

        // Wait a bit for async processing
        Thread.sleep(1000);

        assertThat(listener.isInvoked())
            .as("Async listener should be invoked within 1 second")
            .isTrue();

        asyncExecutor.shutdown();
    }

    // 4. Transactional Async event flow test
    static class TxAsyncEvent { }

    @Getter
    static class TxAsyncListener {
        private volatile boolean invoked = false;

        @TransactionalEventListener(phase = TransactionalPhase.AFTER_COMMIT)
        @Async
        public void onTxAsyncEvent(TxAsyncEvent event) {
            invoked = true;
        }

    }

    @Test
    public void testTransactionalAsyncEventFlow() throws InterruptedException {
        // Create a DummyTransactionManager and begin a transaction
        DummyTransactionManager dtm = new DummyTransactionManager();
        dtm.begin(); // Transaction is active

        EventRegistry registry = new EventRegistry();
        TxAsyncListener listener = new TxAsyncListener();
        registry.register(listener);

        // Create AsyncExecutor and publisher that supports both transaction and async
        AsyncExecutor asyncExecutor = new AsyncExecutor();
        ApplicationEventPublisher publisher = EventPublisherFactory.createEventPublisher(registry, dtm, asyncExecutor);

        publisher.publish(new TxAsyncEvent());

        // During an active transaction the listener should not be invoked
        assertThat(listener.isInvoked())
            .as("Transactional async listener should not be invoked during an active transaction")
            .isFalse();

        // Commit the transaction and flush delayed events
        dtm.commit();
        ((TransactionalEventPublisher) publisher).flush();

        // Wait for async processing to complete
        Thread.sleep(3000);

        assertThat(listener.isInvoked())
            .as("Transactional async listener should be invoked asynchronously after flush")
            .isTrue();

        asyncExecutor.shutdown();
    }
} 
