package io.github.event.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import io.github.event.core.model.AbstractEvent;
import io.github.event.core.model.TransactionPhase;

class ListenerMethodTest {

    @Test
    void testAnnotationDefaults() throws NoSuchMethodException {
        Method method = TestEventHandler.class.getMethod("handleDefaultEvent", TestEvent.class);
        ListenerMethod annotation = method.getAnnotation(ListenerMethod.class);
        
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(TestEvent.class);
        assertThat(annotation.async()).isFalse();
        assertThat(annotation.transactional()).isFalse();
        assertThat(annotation.transactionPhase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(annotation.idempotent()).isFalse();
    }
    
    @Test
    void testCustomAnnotationValues() throws NoSuchMethodException {
        Method method = TestEventHandler.class.getMethod("handleCustomEvent", TestEvent.class);
        ListenerMethod annotation = method.getAnnotation(ListenerMethod.class);
        
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(TestEvent.class);
        assertThat(annotation.async()).isTrue();
        assertThat(annotation.transactional()).isTrue();
        assertThat(annotation.transactionPhase()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
        assertThat(annotation.idempotent()).isTrue();
    }
    
    @Test
    void testAnnotationAbsence() throws NoSuchMethodException {
        Method method = TestEventHandler.class.getMethod("nonAnnotatedMethod", TestEvent.class);
        ListenerMethod annotation = method.getAnnotation(ListenerMethod.class);
        
        assertThat(annotation).isNull();
    }
    
    @Test
    void testAnnotationRetention() {
        Retention retention = ListenerMethod.class.getAnnotation(Retention.class);
        
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }
    
    @Test
    void testAnnotationTarget() {
        Target target = ListenerMethod.class.getAnnotation(Target.class);
        
        assertThat(target).isNotNull();
        assertThat(target.value()).containsExactly(ElementType.METHOD);
    }
    
    static class TestEvent extends AbstractEvent {
        private final String message;
        
        public TestEvent(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    @EventHandler
    static class TestEventHandler {
        
        @ListenerMethod(TestEvent.class)
        public void handleDefaultEvent(TestEvent event) {
            // 테스트용 메서드
        }
        
        @ListenerMethod(
                value = TestEvent.class,
                async = true,
                transactional = true,
                transactionPhase = TransactionPhase.BEFORE_COMMIT,
                idempotent = true
        )
        public void handleCustomEvent(TestEvent event) {
            // 테스트용 메서드
        }
        
        public void nonAnnotatedMethod(TestEvent event) {
            // 테스트용 메서드
        }
    }
} 