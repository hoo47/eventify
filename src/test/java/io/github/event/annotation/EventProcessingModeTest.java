package io.github.event.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import io.github.event.core.model.ProcessingMode;
import io.github.event.core.model.TransactionPhase;

class EventProcessingModeTest {

    @Test
    void testAnnotationDefaults() {
        EventProcessingMode annotation = DefaultAnnotatedClass.class.getAnnotation(EventProcessingMode.class);
        
        assertThat(annotation).isNotNull();
        assertThat(annotation.mode()).isEqualTo(ProcessingMode.SYNCHRONOUS);
        assertThat(annotation.async()).isFalse();
        assertThat(annotation.transactional()).isFalse();
        assertThat(annotation.transactionPhase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
    
    @Test
    void testCustomAnnotationValues() {
        EventProcessingMode annotation = CustomAnnotatedClass.class.getAnnotation(EventProcessingMode.class);
        
        assertThat(annotation).isNotNull();
        assertThat(annotation.mode()).isEqualTo(ProcessingMode.ASYNC_TRANSACTIONAL);
        assertThat(annotation.async()).isTrue();
        assertThat(annotation.transactional()).isTrue();
        assertThat(annotation.transactionPhase()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
    }
    
    @Test
    void testAnnotationRetention() {
        Retention retention = EventProcessingMode.class.getAnnotation(Retention.class);
        
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }
    
    @Test
    void testAnnotationTarget() {
        Target target = EventProcessingMode.class.getAnnotation(Target.class);
        
        assertThat(target).isNotNull();
        assertThat(target.value()).containsExactly(ElementType.TYPE);
    }
    
    @EventProcessingMode
    static class DefaultAnnotatedClass {
    }
    
    @EventProcessingMode(
            mode = ProcessingMode.ASYNC_TRANSACTIONAL,
            async = true,
            transactional = true,
            transactionPhase = TransactionPhase.BEFORE_COMMIT
    )
    static class CustomAnnotatedClass {
    }
} 