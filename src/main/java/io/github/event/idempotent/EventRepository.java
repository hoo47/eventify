package io.github.event.idempotent;

import java.time.Duration;
import java.time.Instant;

/**
 * 이벤트 처리 상태를 저장하는 저장소 인터페이스
 */
public interface EventRepository {
    
    /**
     * 이벤트가 이미 처리되었는지 확인합니다.
     * 
     * @param eventId 확인할 이벤트 ID
     * @return 이미 처리된 경우 true, 그렇지 않으면 false
     */
    boolean isProcessed(String eventId);
    
    /**
     * 이벤트를 처리 완료로 표시합니다.
     * 
     * @param eventId 처리된 이벤트 ID
     * @param processedAt 처리 시간
     */
    void markAsProcessed(String eventId, Instant processedAt);
    
    /**
     * 오래된 이벤트 처리 기록을 정리합니다.
     * 
     * @param olderThan 이 기간보다 오래된 기록 제거
     */
    void cleanupOldEvents(Duration olderThan);
} 
