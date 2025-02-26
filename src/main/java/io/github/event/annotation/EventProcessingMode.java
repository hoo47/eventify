package io.github.event.annotation;

import io.github.event.core.model.ProcessingMode;
import io.github.event.core.model.TransactionPhase;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이벤트의 처리 방식을 지정하는 어노테이션
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventProcessingMode {
    
    /**
     * 이벤트 처리 모드
     */
    ProcessingMode mode() default ProcessingMode.SYNCHRONOUS;
    
    /**
     * 이벤트가 비동기적으로 처리되어야 하는지 여부
     * @deprecated {@link #mode()} 사용을 권장합니다.
     */
    @Deprecated
    boolean async() default false;
    
    /**
     * 이벤트가 트랜잭션 내에서 처리되어야 하는지 여부
     * @deprecated {@link #mode()} 사용을 권장합니다.
     */
    @Deprecated
    boolean transactional() default false;
    
    /**
     * 트랜잭션 단계 (transactional이 true인 경우에만 사용)
     */
    TransactionPhase transactionPhase() default TransactionPhase.AFTER_COMMIT;
} 