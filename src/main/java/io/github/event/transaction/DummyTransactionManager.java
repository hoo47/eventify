package io.github.event.transaction;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.RollbackException;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import lombok.Getter;

@Getter
public class DummyTransactionManager implements TransactionManager {

    @Getter
    private String state = "NONE";
    private final List<Runnable> synchronizations = new ArrayList<>();

    public void registerSynchronization(Runnable callback) {
        synchronizations.add(callback);
    }

    @Override
    public void begin() throws NotSupportedException, SystemException {
        state = "ACTIVE";
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException {
        if ("ACTIVE".equals(state)) {
            state = "COMMITTED";
        }
        for (Runnable callback : synchronizations) {
            callback.run();
        }
        synchronizations.clear();
    }

    @Override
    public void rollback() throws IllegalStateException, SystemException {
        if ("ACTIVE".equals(state)) {
            state = "ROLLED_BACK";
        }
    }

    @Override
    public int getStatus() throws SystemException {
        if ("ACTIVE".equals(state)) return 0;
        if ("COMMITTED".equals(state)) return 3;
        if ("ROLLED_BACK".equals(state)) return 4;
        return 6; // No transaction
    }

    @Override
    public Transaction getTransaction() throws SystemException {
        return null;
    }

    @Override
    public void resume(Transaction tobj) throws InvalidTransactionException, SystemException {
        // Dummy implementation: do nothing
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        // Dummy implementation: no operation
    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTransactionTimeout'");
    }

    @Override
    public Transaction suspend() throws SystemException {
        return null;
    }
} 
