package io.github.event.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 멱등성 처리가 필요한 리스너를 표시하는 어노테이션
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Idempotent {
    
    /**
     * 멱등성 처리 기록의 유지 기간 (초)
     * 기본값은 24시간
     */
    long retentionSeconds() default 86400;
} 