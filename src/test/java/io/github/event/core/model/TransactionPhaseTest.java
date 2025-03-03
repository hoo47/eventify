package io.github.event.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TransactionPhaseTest {

    @Test
    void testBeforeCommitPhase() {
        TransactionPhase phase = TransactionPhase.BEFORE_COMMIT;
        
        assertThat(phase.isCommitPhase()).isTrue();
    }
    
    @Test
    void testAfterCommitPhase() {
        TransactionPhase phase = TransactionPhase.AFTER_COMMIT;
        
        assertThat(phase.isCommitPhase()).isTrue();
    }
    
    @Test
    void testAfterRollbackPhase() {
        TransactionPhase phase = TransactionPhase.AFTER_ROLLBACK;
        
        assertThat(phase.isCommitPhase()).isFalse();
    }
    
    @Test
    void testImmediatePhase() {
        TransactionPhase phase = TransactionPhase.IMMEDIATE;
        
        assertThat(phase.isCommitPhase()).isFalse();
    }
    
    @ParameterizedTest
    @EnumSource(value = TransactionPhase.class, names = {"BEFORE_COMMIT", "AFTER_COMMIT"})
    void testCommitPhases(TransactionPhase phase) {
        assertThat(phase.isCommitPhase()).isTrue();
    }
    
    @ParameterizedTest
    @EnumSource(value = TransactionPhase.class, names = {"AFTER_ROLLBACK", "IMMEDIATE"})
    void testNonCommitPhases(TransactionPhase phase) {
        assertThat(phase.isCommitPhase()).isFalse();
    }
    
    @Test
    void testEnumValues() {
        TransactionPhase[] phases = TransactionPhase.values();
        
        assertThat(phases).hasSize(4);
        assertThat(phases).contains(
            TransactionPhase.BEFORE_COMMIT,
            TransactionPhase.AFTER_COMMIT,
            TransactionPhase.AFTER_ROLLBACK,
            TransactionPhase.IMMEDIATE
        );
    }
    
    @Test
    void testEnumValueOf() {
        assertThat(TransactionPhase.valueOf("BEFORE_COMMIT")).isEqualTo(TransactionPhase.BEFORE_COMMIT);
        assertThat(TransactionPhase.valueOf("AFTER_COMMIT")).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(TransactionPhase.valueOf("AFTER_ROLLBACK")).isEqualTo(TransactionPhase.AFTER_ROLLBACK);
        assertThat(TransactionPhase.valueOf("IMMEDIATE")).isEqualTo(TransactionPhase.IMMEDIATE);
    }
} 