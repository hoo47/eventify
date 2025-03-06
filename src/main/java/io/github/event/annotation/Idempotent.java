package io.github.event.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이벤트 처리의 멱등성을 보장하기 위한 어노테이션
 * 클래스나 메서드에 적용할 수 있으며, 동일한 이벤트에 대해 한 번만 처리되는 것을 보장합니다.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    
    /**
     * 멱등성 보장을 위한 이벤트 ID 보관 기간 (초 단위)
     * 기본값은 24시간 (86400초)
     */
    long retentionSeconds() default 86400L;
} 