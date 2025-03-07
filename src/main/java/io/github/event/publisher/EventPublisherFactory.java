package io.github.event.publisher;

import io.github.event.async.AsyncExecutor;
import io.github.event.registry.EventRegistry;
import jakarta.transaction.TransactionManager;

public class EventPublisherFactory {
    public static ApplicationEventPublisher createEventPublisher(
            EventRegistry registry,
            TransactionManager transactionManager,
            AsyncExecutor asyncExecutor
    ) {
        if (transactionManager != null) {
            return new TransactionalEventPublisher(registry, transactionManager);
        } else if (asyncExecutor != null) {
            return new DefaultEventPublisher(registry, asyncExecutor);
        } else {
            return new DefaultEventPublisher(registry);
        }
    }
} 
