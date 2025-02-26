package io.github.event.core.api;

/**
 * 이벤트 리스너 인터페이스
 * 
 * @param <T> 처리할 이벤트 타입
 */
public interface EventListener<T extends Event> {
    
    /**
     * 처리할 이벤트 타입을 반환합니다.
     * 
     * @return 이벤트 타입 클래스
     */
    Class<T> getEventType();
    
    /**
     * 이벤트를 처리합니다.
     * 
     * @param event 처리할 이벤트
     */
    void onEvent(T event);
}
