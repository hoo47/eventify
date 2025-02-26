package io.github.event.listener;

import io.github.event.core.api.Event;
import io.github.event.core.api.EventListener;
import io.github.event.core.model.TransactionPhase;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Method;

@Slf4j
public class MethodEventListener<T extends Event> implements EventListener<T> {
    
    private final Object target;
    @Getter
    private final Method method;
    @Getter
    private final Class<T> eventType;
    @Getter
    private final boolean async;
    @Getter
    private final boolean transactional;
    @Getter
    private final TransactionPhase transactionPhase;
    @Getter
    private final boolean idempotent;
    
    public MethodEventListener(
            Object target, 
            Method method, 
            Class<T> eventType,
            boolean async,
            boolean transactional,
            TransactionPhase transactionPhase,
            boolean idempotent) {
        this.target = target;
        this.method = method;
        this.eventType = eventType;
        this.async = async;
        this.transactional = transactional;
        this.transactionPhase = transactionPhase;
        this.idempotent = idempotent;
        
        // 메서드 접근성 보장 (Java 9+ 호환)
        try {
            method.setAccessible(true);
        } catch (Exception e) {
            log.warn("메서드 접근성 설정 실패: {}.{} - {}", 
                     target.getClass().getSimpleName(), 
                     method.getName(), 
                     e.getMessage());
        }
    }
    
    @Override
    public void onEvent(T event) {
        try {
            method.invoke(target, event);
        } catch (Exception e) {
            log.error("이벤트 처리 중 오류 발생: {}.{} -> {}", 
                    target.getClass().getSimpleName(), 
                    method.getName(), 
                    e.getMessage(), e);
            throw new RuntimeException("이벤트 처리 중 오류 발생", e);
        }
    }

    /**
     * 대상 객체의 클래스를 반환합니다.
     */
    public Class<?> getTargetClass() {
        return target.getClass();
    }

    /**
     * 메서드 이름을 반환합니다.
     */
    public String getMethodName() {
        return method.getName();
    }
} 
