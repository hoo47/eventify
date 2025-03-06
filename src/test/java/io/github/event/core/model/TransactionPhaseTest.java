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
        assertThat(phase.name()).isEqualTo("BEFORE_COMMIT");
        assertThat(phase.ordinal()).isEqualTo(0);
    }
    
    @Test
    void testAfterCommitPhase() {
        TransactionPhase phase = TransactionPhase.AFTER_COMMIT;
        
        assertThat(phase.isCommitPhase()).isTrue();
        assertThat(phase.name()).isEqualTo("AFTER_COMMIT");
        assertThat(phase.ordinal()).isEqualTo(1);
    }
    
    @Test
    void testAfterRollbackPhase() {
        TransactionPhase phase = TransactionPhase.AFTER_ROLLBACK;
        
        assertThat(phase.isCommitPhase()).isFalse();
        assertThat(phase.name()).isEqualTo("AFTER_ROLLBACK");
        assertThat(phase.ordinal()).isEqualTo(2);
    }

    @Test
    void testAfterCompletionPhase() {
        TransactionPhase phase = TransactionPhase.AFTER_COMPLETION;
        assertThat(phase.isCommitPhase()).isFalse();
        assertThat(phase.name()).isEqualTo("AFTER_COMPLETION");
        assertThat(phase.ordinal()).isEqualTo(3);
    }

    @ParameterizedTest
    @EnumSource(value = TransactionPhase.class, names = {"BEFORE_COMMIT", "AFTER_COMMIT"})
    void testCommitPhases(TransactionPhase phase) {
        assertThat(phase.isCommitPhase()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = TransactionPhase.class, names = {"AFTER_ROLLBACK", "AFTER_COMPLETION"})
    void testNonCommitPhases(TransactionPhase phase) {
        assertThat(phase.isCommitPhase()).isFalse();
    }
    
    @Test
    void testEnumValues() {
        TransactionPhase[] phases = TransactionPhase.values();
        
        assertThat(phases).hasSize(4);
        assertThat(phases[0]).isEqualTo(TransactionPhase.BEFORE_COMMIT);
        assertThat(phases[1]).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(phases[2]).isEqualTo(TransactionPhase.AFTER_ROLLBACK);
        assertThat(phases[3]).isEqualTo(TransactionPhase.AFTER_COMPLETION);
    }
    
    @Test
    void testEnumValueOf() {
        assertThat(TransactionPhase.valueOf("BEFORE_COMMIT")).isEqualTo(TransactionPhase.BEFORE_COMMIT);
        assertThat(TransactionPhase.valueOf("AFTER_COMMIT")).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(TransactionPhase.valueOf("AFTER_ROLLBACK")).isEqualTo(TransactionPhase.AFTER_ROLLBACK);
        assertThat(TransactionPhase.valueOf("AFTER_COMPLETION")).isEqualTo(TransactionPhase.AFTER_COMPLETION);
    }
} 
