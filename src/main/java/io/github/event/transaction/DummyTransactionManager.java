package io.github.event.transaction;

public class DummyTransactionManager implements TransactionManager {

    private String state = "NONE";

    public String getState() {
        return state;
    }

    @Override
    public void begin() {
        state = "ACTIVE";
    }

    @Override
    public void commit() {
        if ("ACTIVE".equals(state)) {
            state = "COMMITTED";
        }
    }

    @Override
    public void rollback() {
        if ("ACTIVE".equals(state)) {
            state = "ROLLED_BACK";
        }
    }
} 