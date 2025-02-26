package io.github.event.annotation;

import io.github.event.core.model.TransactionPhase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



/**
 * 트랜잭션 단계에 따라 이벤트를 처리하는 리스너를 표시합니다.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TransactionalListener {
    
    /**
     * 트랜잭션 단계를 지정합니다.
     */
    TransactionPhase value() default TransactionPhase.AFTER_COMMIT;
} 
