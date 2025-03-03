package io.github.event.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class IdempotentTest {

    @Test
    void testAnnotationDefaults() {
        // 기본 어노테이션 값 테스트
        Idempotent annotation = DefaultIdempotentClass.class.getAnnotation(Idempotent.class);
        
        // 기본값 검증 (24시간 = 86400초)
        assertThat(annotation).isNotNull();
        assertThat(annotation.retentionSeconds()).isEqualTo(86400L);
    }
    
    @Test
    void testCustomAnnotationValues() {
        // 커스텀 어노테이션 값 테스트
        Idempotent annotation = CustomIdempotentClass.class.getAnnotation(Idempotent.class);
        
        // 커스텀 값 검증 (1시간 = 3600초)
        assertThat(annotation).isNotNull();
        assertThat(annotation.retentionSeconds()).isEqualTo(3600L);
    }
    
    @Test
    void testAnnotationAbsence() {
        // 어노테이션이 없는 클래스 테스트
        Idempotent annotation = NonAnnotatedClass.class.getAnnotation(Idempotent.class);
        
        // 어노테이션 부재 검증
        assertThat(annotation).isNull();
    }
    
    @Test
    void testAnnotationRetention() {
        // 어노테이션 보존 정책 테스트
        Retention retention = Idempotent.class.getAnnotation(Retention.class);
        
        // 런타임 보존 검증
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }
    
    @Test
    void testAnnotationTarget() {
        // 어노테이션 대상 테스트
        Target target = Idempotent.class.getAnnotation(Target.class);
        
        // 타입 대상 검증
        assertThat(target).isNotNull();
        assertThat(target.value()).containsExactly(ElementType.TYPE);
    }
    
    // 기본 멱등성 어노테이션 테스트용 클래스
    @Idempotent
    static class DefaultIdempotentClass {
        // 기본 멱등성 어노테이션 테스트용
    }
    
    // 커스텀 멱등성 어노테이션 테스트용 클래스
    @Idempotent(retentionSeconds = 3600)
    static class CustomIdempotentClass {
        // 커스텀 멱등성 어노테이션 테스트용
    }
    
    // 어노테이션이 없는 클래스
    static class NonAnnotatedClass {
        // 어노테이션이 없는 클래스
    }
} 