package io.github.event.registry;

import java.lang.reflect.Method;

public class HandlerMethod {
    private final Object instance;
    private final Method method;

    public HandlerMethod(Object instance, Method method) {
        this.instance = instance;
        this.method = method;
    }

    public Object getInstance() {
        return instance;
    }

    public Method getMethod() {
        return method;
    }
} 