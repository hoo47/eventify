package io.github.event.publisher;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.Getter;
import org.junit.jupiter.api.Test;

import io.github.event.annotations.EventListener;
import io.github.event.registry.EventRegistry;
import io.github.event.transaction.DummyTransactionManager;

public class TransactionalEventPublisherTest {

    static class TestEvent { }

    @Getter
    static class DummyListener {
        private boolean invoked = false;

        @EventListener
        public void handleEvent(TestEvent event) {
            invoked = true;
        }

    }

    @Test
    public void publishEventDuringActiveTransaction_delaysEvent() {
        // Arrange: create a DummyTransactionManager and start a transaction
        DummyTransactionManager dtm = new DummyTransactionManager();
        dtm.begin(); // state becomes ACTIVE
        
        EventRegistry registry = new EventRegistry();
        DummyListener listener = new DummyListener();
        registry.register(listener);
        
        TransactionalEventPublisher publisher = new TransactionalEventPublisher(registry, dtm);
        
        // Act: publish an event while transaction is active
        TestEvent event = new TestEvent();
        publisher.publish(event);
        
        // Assert: since transaction is active, event should be delayed and not immediately invoke the listener
        assertThat(listener.isInvoked())
            .as("Listener should not be immediately invoked during an active transaction")
            .isFalse();
        
        // Simulate transaction commit by changing state to COMMITTED
        dtm.commit(); // state becomes COMMITTED
        
        // Act: flush delayed events
        publisher.flush();
        
        // Assert: after flush, listener should be invoked
        assertThat(listener.isInvoked())
            .as("Listener should be invoked after flush when transaction is not active")
            .isTrue();
    }

    @Test
    public void publishEventWithoutActiveTransaction_publishesImmediately() {
        // Arrange: create a DummyTransactionManager with no active transaction
        DummyTransactionManager dtm = new DummyTransactionManager();
        // Do not begin transaction, so state remains "NONE"
        
        EventRegistry registry = new EventRegistry();
        DummyListener listener = new DummyListener();
        registry.register(listener);
        
        TransactionalEventPublisher publisher = new TransactionalEventPublisher(registry, dtm);
        
        // Act: publish an event
        TestEvent event = new TestEvent();
        publisher.publish(event);
        
        // Assert: listener should be invoked immediately
        assertThat(listener.isInvoked())
            .as("Listener should be immediately invoked when there is no active transaction")
            .isTrue();
    }
} 
