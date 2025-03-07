package io.github.event.transaction;

public interface TransactionManager {
    void begin();
    void commit();
    void rollback();
} 