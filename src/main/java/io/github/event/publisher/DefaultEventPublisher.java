package io.github.event.publisher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import io.github.event.registry.EventRegistry;
import io.github.event.registry.HandlerMethod;

public class DefaultEventPublisher {

    private final EventRegistry registry;

    public DefaultEventPublisher(EventRegistry registry) {
        this.registry = registry;
    }

    public void publish(Object event) {
        List<HandlerMethod> handlers = registry.getHandlersForEvent(event);
        for (HandlerMethod handler : handlers) {
            Method method = handler.getMethod();
            try {
                method.invoke(handler.getInstance(), event);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to invoke event handler", e);
            }
        }
    }
} 