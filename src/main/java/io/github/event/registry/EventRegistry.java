package io.github.event.registry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.github.event.annotations.EventListener;
import io.github.event.annotations.TransactionalEventListener;

public class EventRegistry {
    private final List<HandlerMethod> handlerMethods = new ArrayList<>();

    public void register(Object listener) {
        // Scan all public methods of the listener
        Method[] methods = listener.getClass().getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(EventListener.class) || method.isAnnotationPresent(TransactionalEventListener.class)) {
                handlerMethods.add(new HandlerMethod(listener, method));
            }
        }
    }

    public List<HandlerMethod> getHandlersForEvent(Object event) {
        List<HandlerMethod> matching = new ArrayList<>();
        for (HandlerMethod hm : handlerMethods) {
            Class<?>[] params = hm.getMethod().getParameterTypes();
            if (params.length > 0 && params[0].isAssignableFrom(event.getClass())) {
                matching.add(hm);
            }
        }
        return matching;
    }
} 