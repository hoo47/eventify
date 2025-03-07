package io.github.event.annotations;

public enum TransactionalPhase {
    BEFORE_COMMIT,
    AFTER_COMMIT,
    AFTER_ROLLBACK,
    AFTER_COMPLETION
} 