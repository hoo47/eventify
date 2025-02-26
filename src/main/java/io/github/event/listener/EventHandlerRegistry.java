package io.github.event.listener;

import io.github.event.annotation.*;
import io.github.event.core.api.Event;
import io.github.event.core.model.TransactionPhase;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class EventHandlerRegistry {

    private final ApplicationMulticaster multicaster;
    private final Map<Class<?>, Object> handlerInstances = new ConcurrentHashMap<>();

    public EventHandlerRegistry(ApplicationMulticaster multicaster) {
        this.multicaster = multicaster;
    }

    public void registerHandler(Object handler) {
        if (!handler.getClass().isAnnotationPresent(EventHandler.class)) {
            log.warn("객체 {}는 @EventHandler 어노테이션이 없습니다", handler.getClass().getName());
            return;
        }

        // 핸들러 인스턴스 저장
        handlerInstances.put(handler.getClass(), handler);

        Arrays.stream(handler.getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(ListenerMethod.class))
                .forEach(method -> registerMethod(handler, method));
    }

    private void registerMethod(Object handler, Method method) {
        // ListenerMethod 어노테이션 처리
        if (method.isAnnotationPresent(ListenerMethod.class)) {
            ListenerMethod annotation = method.getAnnotation(ListenerMethod.class);
            Class<? extends Event> eventType = annotation.value();

            MethodEventListener<?> listener = new MethodEventListener<>(
                    handler,
                    method,
                    eventType,
                    annotation.async(),
                    annotation.transactional(),
                    annotation.transactionPhase(),
                    annotation.idempotent()
            );

            multicaster.addListener(listener);
            log.info("이벤트 리스너 등록: {}.{} -> {}",
                    handler.getClass().getSimpleName(),
                    method.getName(),
                    eventType.getSimpleName());
        }
        // AsyncListener 어노테이션 처리
        else if (method.isAnnotationPresent(AsyncListener.class)) {
            Class<? extends Event> eventType = determineEventType(method);

            MethodEventListener<?> listener = new MethodEventListener<>(
                    handler,
                    method,
                    eventType,
                    true,  // async
                    false, // transactional
                    TransactionPhase.AFTER_COMMIT,
                    false  // idempotent
            );

            multicaster.addListener(listener);
            log.info("비동기 이벤트 리스너 등록: {}.{} -> {}",
                    handler.getClass().getSimpleName(),
                    method.getName(),
                    eventType.getSimpleName());
        }
        // TransactionalListener 어노테이션 처리
        else if (method.isAnnotationPresent(TransactionalListener.class)) {
            TransactionalListener annotation = method.getAnnotation(TransactionalListener.class);
            Class<? extends Event> eventType = determineEventType(method);

            MethodEventListener<?> listener = new MethodEventListener<>(
                    handler,
                    method,
                    eventType,
                    false, // async
                    true,  // transactional
                    annotation.value(),
                    false  // idempotent
            );

            multicaster.addListener(listener);
            log.info("트랜잭션 이벤트 리스너 등록: {}.{} -> {}, 단계: {}",
                    handler.getClass().getSimpleName(),
                    method.getName(),
                    eventType.getSimpleName(),
                    annotation.value());
        }
        // AsyncTransactionalListener 어노테이션 처리
        else if (method.isAnnotationPresent(AsyncTransactionalListener.class)) {
            AsyncTransactionalListener annotation = method.getAnnotation(AsyncTransactionalListener.class);
            Class<? extends Event> eventType = determineEventType(method);

            MethodEventListener<?> listener = new MethodEventListener<>(
                    handler,
                    method,
                    eventType,
                    true,  // async
                    true,  // transactional
                    annotation.value(),
                    false  // idempotent
            );

            multicaster.addListener(listener);
            log.info("비동기 트랜잭션 이벤트 리스너 등록: {}.{} -> {}, 단계: {}",
                    handler.getClass().getSimpleName(),
                    method.getName(),
                    eventType.getSimpleName(),
                    annotation.value());
        }
    }

    /**
     * 메서드 파라미터에서 이벤트 타입을 결정합니다.
     */
    @SuppressWarnings("unchecked")
    private Class<? extends Event> determineEventType(Method method) {
        if (method.getParameterCount() != 1) {
            throw new IllegalArgumentException("이벤트 리스너 메서드는 정확히 하나의 파라미터를 가져야 합니다: " +
                    method.getDeclaringClass().getName() + "." + method.getName());
        }

        Class<?> paramType = method.getParameterTypes()[0];
        if (!Event.class.isAssignableFrom(paramType)) {
            throw new IllegalArgumentException("이벤트 리스너 메서드의 파라미터는 Event 타입이어야 합니다: " +
                    method.getDeclaringClass().getName() + "." + method.getName());
        }

        return (Class<? extends Event>) paramType;
    }

    /**
     * 특정 클래스의 핸들러 인스턴스를 반환합니다.
     */
    public Object getHandler(Class<?> handlerClass) {
        return handlerInstances.get(handlerClass);
    }
} 
