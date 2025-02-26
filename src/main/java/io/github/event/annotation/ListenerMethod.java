package io.github.event.annotation;

import io.github.event.core.api.Event;
import io.github.event.core.model.TransactionPhase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이벤트 리스너 메서드를 표시하는 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ListenerMethod {
    
    /**
     * 처리할 이벤트 타입
     */
    Class<? extends Event> value();
    
    /**
     * 비동기 처리 여부
     */
    boolean async() default false;
    
    /**
     * 트랜잭션 처리 여부
     */
    boolean transactional() default false;
    
    /**
     * 트랜잭션 단계 (transactional이 true인 경우에만 사용)
     */
    TransactionPhase transactionPhase() default TransactionPhase.AFTER_COMMIT;
    
    /**
     * 멱등성 보장 여부
     */
    boolean idempotent() default false;
} 
