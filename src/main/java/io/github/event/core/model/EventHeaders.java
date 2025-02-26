package io.github.event.core.model;

/**
 * 이벤트 관련 헤더 상수
 */
public final class EventHeaders {
    
    /**
     * 이벤트 ID 헤더 이름
     */
    public static final String EVENT_ID = "X-Event-ID";
    
    /**
     * 이벤트 발행 시간 헤더 이름
     */
    public static final String EVENT_TIMESTAMP = "X-Event-Timestamp";
    
    /**
     * 이벤트 타입 헤더 이름
     */
    public static final String EVENT_TYPE = "X-Event-Type";
    
    /**
     * 이벤트 소스 헤더 이름
     */
    public static final String EVENT_SOURCE = "X-Event-Source";
    
    private EventHeaders() {
        // 인스턴스화 방지
    }
} 