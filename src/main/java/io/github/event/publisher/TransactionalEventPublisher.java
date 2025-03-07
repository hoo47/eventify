package io.github.event.publisher;

import java.util.ArrayList;
import java.util.List;

import io.github.event.registry.EventRegistry;
import io.github.event.transaction.DummyTransactionManager;
import io.github.event.transaction.TransactionManager;

public class TransactionalEventPublisher implements ApplicationEventPublisher {

    private final ApplicationEventPublisher delegatePublisher;
    private final TransactionManager transactionManager;
    private final List<Object> delayedEvents = new ArrayList<>();

    public TransactionalEventPublisher(EventRegistry registry, TransactionManager transactionManager) {
        // Use DefaultEventPublisher as the delegate.
        this.delegatePublisher = new DefaultEventPublisher(registry);
        this.transactionManager = transactionManager;
    }

    @Override
    public void publish(Object event) {
        // If a transaction is active, delay the event publishing
        if (transactionManager instanceof DummyTransactionManager &&
            "ACTIVE".equals(((DummyTransactionManager)transactionManager).getState())) {
            delayedEvents.add(event);
        } else {
            delegatePublisher.publish(event);
        }
    }

    // Flush delayed events, typically to be called upon transaction commit
    public void flush() {
        for (Object event : delayedEvents) {
            delegatePublisher.publish(event);
        }
        delayedEvents.clear();
    }
} 