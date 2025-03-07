package io.github.event.publisher;

import java.util.ArrayList;
import java.util.List;

import io.github.event.registry.EventRegistry;
import io.github.event.transaction.DummyTransactionManager;
import jakarta.transaction.TransactionManager;

public class TransactionalEventPublisher implements ApplicationEventPublisher {

    private final ApplicationEventPublisher delegatePublisher;
    private final TransactionManager transactionManager;
    private final List<Object> delayedEvents = new ArrayList<>();
    private boolean synchronizationRegistered = false;

    public TransactionalEventPublisher(EventRegistry registry, TransactionManager transactionManager) {
        // Use DefaultEventPublisher as the delegate.
        this.delegatePublisher = new DefaultEventPublisher(registry);
        this.transactionManager = transactionManager;
    }

    @Override
    public void publish(Object event) {
        // If a transaction is active, delay the event publishing
        if (transactionManager != null &&
            "ACTIVE".equals(((DummyTransactionManager)transactionManager).getState())) {
            delayedEvents.add(event);
            if (!synchronizationRegistered) {
                ((DummyTransactionManager)transactionManager).registerSynchronization(() -> {
                    flush();
                    synchronizationRegistered = false;
                });
                synchronizationRegistered = true;
            }
        } else {
            delegatePublisher.publish(event);
        }
    }

    // Flush delayed events, typically to be called upon transaction commit
    private void flush() {
        for (Object event : delayedEvents) {
            delegatePublisher.publish(event);
        }
        delayedEvents.clear();
    }
} 
