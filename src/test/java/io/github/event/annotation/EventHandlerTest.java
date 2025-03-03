package io.github.event.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class EventHandlerTest {

    @Test
    void testAnnotationPresence() {
        EventHandler annotation = TestEventHandler.class.getAnnotation(EventHandler.class);
        
        assertThat(annotation).isNotNull();
    }
    
    @Test
    void testAnnotationAbsence() {
        EventHandler annotation = NonAnnotatedClass.class.getAnnotation(EventHandler.class);
        
        assertThat(annotation).isNull();
    }
    
    @Test
    void testAnnotationRetention() {
        Retention retention = EventHandler.class.getAnnotation(Retention.class);
        
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }
    
    @Test
    void testAnnotationTarget() {
        Target target = EventHandler.class.getAnnotation(Target.class);
        
        assertThat(target).isNotNull();
        assertThat(target.value()).containsExactly(ElementType.TYPE);
    }
    
    @EventHandler
    static class TestEventHandler {
        public void handleEvent(Object event) {
            // 테스트용 메서드
        }
    }
    
    static class NonAnnotatedClass {
        public void handleEvent(Object event) {
            // 테스트용 메서드
        }
    }
} 