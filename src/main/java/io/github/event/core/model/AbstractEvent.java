package io.github.event.core.model;

import io.github.event.core.api.Event;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 이벤트 기본 구현 클래스
 */
public abstract class AbstractEvent implements Event {
    
    private final String eventId;
    private final Instant issuedAt;
    private final Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 기본 생성자
     * 이벤트 ID와 발행 시간을 자동으로 생성합니다.
     */
    protected AbstractEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.issuedAt = Instant.now();
    }
    
    /**
     * 이벤트 ID를 지정하는 생성자
     * 
     * @param eventId 이벤트 ID
     */
    protected AbstractEvent(String eventId) {
        this.eventId = eventId;
        this.issuedAt = Instant.now();
    }
    
    /**
     * 이벤트 ID와 발행 시간을 지정하는 생성자
     * 
     * @param eventId 이벤트 ID
     * @param issuedAt 발행 시간
     */
    protected AbstractEvent(String eventId, Instant issuedAt) {
        this.eventId = eventId;
        this.issuedAt = issuedAt;
    }
    
    @Override
    public String getEventId() {
        return eventId;
    }
    
    @Override
    public Instant getIssuedAt() {
        return issuedAt;
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }
    
    @Override
    public void addMetadata(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("메타데이터 키는 null이 될 수 없습니다.");
        }
        metadata.put(key, value);
    }
} 