package io.github.event.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import io.github.event.core.model.AbstractEvent;
import io.github.event.core.model.AsyncMode;
import io.github.event.core.model.TransactionPhase;

class AsyncTransactionalListenerTest {

    @Test
    void testAnnotationDefaults() throws NoSuchMethodException {
        // 메서드에 적용된 기본 어노테이션 값 테스트
        Method method = TestEventHandler.class.getMethod("handleDefaultEvent", TestEvent.class);
        AsyncTransactionalListener annotation = method.getAnnotation(AsyncTransactionalListener.class);
        
        // 기본값 검증
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(annotation.mode()).isEqualTo(AsyncMode.EXECUTOR);
    }
    
    @Test
    void testCustomAnnotationValues() throws NoSuchMethodException {
        // 메서드에 적용된 커스텀 어노테이션 값 테스트
        Method method = TestEventHandler.class.getMethod("handleCustomEvent", TestEvent.class);
        AsyncTransactionalListener annotation = method.getAnnotation(AsyncTransactionalListener.class);
        
        // 커스텀 값 검증
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
        assertThat(annotation.mode()).isEqualTo(AsyncMode.RABBITMQ);
    }
    
    @Test
    void testAfterRollbackPhase() throws NoSuchMethodException {
        // 롤백 후 단계 테스트
        Method method = TestEventHandler.class.getMethod("handleAfterRollbackEvent", TestEvent.class);
        AsyncTransactionalListener annotation = method.getAnnotation(AsyncTransactionalListener.class);
        
        // 롤백 후 단계 검증
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(TransactionPhase.AFTER_ROLLBACK);
        assertThat(annotation.mode()).isEqualTo(AsyncMode.EXECUTOR);
    }
    
    @Test
    void testAfterCompletionPhase() throws NoSuchMethodException {
        // 완료 후 단계 테스트
        Method method = TestEventHandler.class.getMethod("handleAfterCompletionEvent", TestEvent.class);
        AsyncTransactionalListener annotation = method.getAnnotation(AsyncTransactionalListener.class);
        
        // 완료 후 단계 검증
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(TransactionPhase.AFTER_COMPLETION);
        assertThat(annotation.mode()).isEqualTo(AsyncMode.EXECUTOR);
    }
    
    @Test
    void testAnnotationAbsence() throws NoSuchMethodException {
        // 어노테이션이 없는 메서드 테스트
        Method method = TestEventHandler.class.getMethod("nonAnnotatedMethod", TestEvent.class);
        AsyncTransactionalListener annotation = method.getAnnotation(AsyncTransactionalListener.class);
        
        // 어노테이션 부재 검증
        assertThat(annotation).isNull();
    }
    
    @Test
    void testClassLevelAnnotation() {
        // 클래스 레벨 어노테이션 테스트
        AsyncTransactionalListener annotation = AsyncTransactionalAnnotatedClass.class.getAnnotation(AsyncTransactionalListener.class);
        
        // 클래스 레벨 어노테이션 검증
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
        assertThat(annotation.mode()).isEqualTo(AsyncMode.RABBITMQ);
    }
    
    @Test
    void testAnnotationRetention() {
        // 어노테이션 보존 정책 테스트
        Retention retention = AsyncTransactionalListener.class.getAnnotation(Retention.class);
        
        // 런타임 보존 검증
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }
    
    @Test
    void testAnnotationTarget() {
        // 어노테이션 대상 테스트
        Target target = AsyncTransactionalListener.class.getAnnotation(Target.class);
        
        // 타입과 메서드 대상 검증
        assertThat(target).isNotNull();
        assertThat(target.value()).contains(ElementType.TYPE, ElementType.METHOD);
    }
    
    // 테스트용 이벤트 클래스
    static class TestEvent extends AbstractEvent {
        private final String message;
        
        public TestEvent(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    // 테스트용 이벤트 핸들러 클래스
    static class TestEventHandler {
        
        @AsyncTransactionalListener
        public void handleDefaultEvent(TestEvent event) {
            // 기본 비동기 트랜잭션 리스너 메서드 (AFTER_COMMIT, EXECUTOR)
        }
        
        @AsyncTransactionalListener(value = TransactionPhase.BEFORE_COMMIT, mode = AsyncMode.RABBITMQ)
        public void handleCustomEvent(TestEvent event) {
            // 커스텀 비동기 트랜잭션 리스너 메서드
        }
        
        @AsyncTransactionalListener(TransactionPhase.AFTER_ROLLBACK)
        public void handleAfterRollbackEvent(TestEvent event) {
            // 롤백 후 비동기 트랜잭션 리스너 메서드
        }
        
        @AsyncTransactionalListener(TransactionPhase.AFTER_COMPLETION)
        public void handleAfterCompletionEvent(TestEvent event) {
            // 완료 후 비동기 트랜잭션 리스너 메서드
        }
        
        public void nonAnnotatedMethod(TestEvent event) {
            // 어노테이션이 없는 메서드
        }
    }
    
    // 클래스 레벨 어노테이션 테스트용 클래스
    @AsyncTransactionalListener(value = TransactionPhase.BEFORE_COMMIT, mode = AsyncMode.RABBITMQ)
    static class AsyncTransactionalAnnotatedClass {
        // 클래스 레벨 어노테이션 테스트용
    }
} 