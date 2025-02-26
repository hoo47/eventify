package io.github.event.idempotent;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 메모리 기반 이벤트 저장소 구현
 */
public class InMemoryEventRepository implements EventRepository {
    
    private final ConcurrentMap<String, Instant> processedEvents = new ConcurrentHashMap<>();
    
    @Override
    public boolean isProcessed(String eventId) {
        return processedEvents.containsKey(eventId);
    }
    
    @Override
    public void markAsProcessed(String eventId, Instant processedAt) {
        processedEvents.put(eventId, processedAt);
    }
    
    @Override
    public void cleanupOldEvents(Duration olderThan) {
        Instant cutoff = Instant.now().minus(olderThan);
        processedEvents.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }
} 
