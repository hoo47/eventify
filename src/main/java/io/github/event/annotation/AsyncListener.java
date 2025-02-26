package io.github.event.annotation;

import io.github.event.core.model.AsyncMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



/**
 * 비동기 이벤트 리스너를 표시하는 어노테이션
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AsyncListener {
    /**
     * 사용할 비동기 처리 메커니즘
     */
    AsyncMode mode() default AsyncMode.EXECUTOR;
} 
