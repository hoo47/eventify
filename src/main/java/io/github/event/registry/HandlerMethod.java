package io.github.event.registry;

import lombok.Getter;

import java.lang.reflect.Method;

@Getter
public class HandlerMethod {
    private final Object instance;
    private final Method method;

    public HandlerMethod(Object instance, Method method) {
        this.instance = instance;
        this.method = method;
    }

}
