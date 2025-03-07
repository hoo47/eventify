package io.github.event.publisher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import io.github.event.annotations.Async;
import io.github.event.async.AsyncExecutor;
import io.github.event.registry.EventRegistry;
import io.github.event.registry.HandlerMethod;

public class DefaultEventPublisher implements ApplicationEventPublisher {

    private final EventRegistry registry;
    private final AsyncExecutor asyncExecutor;

    // Constructor with AsyncExecutor
    public DefaultEventPublisher(EventRegistry registry, AsyncExecutor asyncExecutor) {
        this.registry = registry;
        this.asyncExecutor = asyncExecutor;
    }

    // Constructor without AsyncExecutor, behaves synchronously
    public DefaultEventPublisher(EventRegistry registry) {
        this(registry, null);
    }

    @Override
    public void publish(Object event) {
        List<HandlerMethod> handlers = registry.getHandlersForEvent(event);
        for (HandlerMethod handler : handlers) {
            Method method = handler.getMethod();
            method.setAccessible(true);
            if (asyncExecutor != null && method.isAnnotationPresent(Async.class)) {
                asyncExecutor.submit(() -> {
                    try {
                        method.invoke(handler.getInstance(), event);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        Throwable cause = (e instanceof InvocationTargetException && e.getCause() != null) ? e.getCause() : e;
                        throw new RuntimeException("Failed to invoke async event handler: " + cause.getMessage(), e);
                    }
                });
            } else {
                try {
                    method.invoke(handler.getInstance(), event);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    Throwable cause = (e instanceof InvocationTargetException && e.getCause() != null) ? e.getCause() : e;
                    throw new RuntimeException("Failed to invoke event handler: " + cause.getMessage(), e);
                }
            }
        }
    }
} 