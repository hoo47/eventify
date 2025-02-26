package io.github.event.dispatcher;

import io.github.event.annotation.AsyncProcessing;
import io.github.event.core.api.EventDispatcher;
import io.github.event.core.api.EventListener;
import io.github.event.core.model.AsyncMode;
import io.github.event.listener.MethodEventListener;
import lombok.RequiredArgsConstructor;
import  io.github.event.core.api.Event;

import java.util.Map;

/**
 * 이벤트와 리스너에 따라 적절한 디스패처를 선택하는 전략 클래스
 */
@RequiredArgsConstructor
public class DispatcherSelectionStrategy {
    private final Map<AsyncMode, EventDispatcher> dispatchers;
    private final AsyncMode defaultMode;
    
    /**
     * 이벤트와 리스너에 따라 적절한 디스패처를 선택합니다.
     * 
     * @param event 이벤트
     * @param listener 리스너
     * @return 선택된 디스패처
     */
    public EventDispatcher selectDispatcher(Event event, EventListener<?> listener) {
        // 이벤트 클래스의 어노테이션 확인
        AsyncProcessing eventAnnotation = event.getClass().getAnnotation(AsyncProcessing.class);
        if (eventAnnotation != null) {
            return dispatchers.getOrDefault(eventAnnotation.mode(), 
                                           dispatchers.get(defaultMode));
        }
        
        // 리스너 클래스의 어노테이션 확인
        AsyncProcessing listenerAnnotation = listener.getClass().getAnnotation(AsyncProcessing.class);
        if (listenerAnnotation != null) {
            return dispatchers.getOrDefault(listenerAnnotation.mode(), 
                                           dispatchers.get(defaultMode));
        }
        
        // 메서드 리스너인 경우 메서드 어노테이션 확인
        if (listener instanceof MethodEventListener) {
            MethodEventListener<?> methodListener = (MethodEventListener<?>) listener;
            AsyncProcessing methodAnnotation = methodListener.getMethod().getAnnotation(AsyncProcessing.class);
            if (methodAnnotation != null) {
                return dispatchers.getOrDefault(methodAnnotation.mode(), 
                                               dispatchers.get(defaultMode));
            }
        }
        
        // 기본 디스패처 반환
        return dispatchers.get(defaultMode);
    }
} 
