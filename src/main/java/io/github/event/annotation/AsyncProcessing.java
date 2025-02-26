package io.github.event.annotation;

import io.github.event.core.model.AsyncMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 비동기 처리 메커니즘을 지정하는 어노테이션
 * 이벤트 클래스나 리스너 클래스에 적용할 수 있습니다.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AsyncProcessing {
    /**
     * 사용할 비동기 처리 메커니즘
     */
    AsyncMode mode() default AsyncMode.EXECUTOR;
} 
