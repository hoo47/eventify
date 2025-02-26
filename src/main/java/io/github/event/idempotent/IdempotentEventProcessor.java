package io.github.event.idempotent;

import io.github.event.core.api.Event;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 이벤트 ID를 기반으로 멱등성을 보장하는 이벤트 처리기
 */
public class IdempotentEventProcessor {

    private final ConcurrentMap<String, Boolean> processedEvents = new ConcurrentHashMap<>();
    private final EventRepository eventRepository;

    /**
     * 커스텀 이벤트 저장소를 사용하는 생성자
     *
     * @param eventRepository 이벤트 저장소
     */
    public IdempotentEventProcessor(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * 기본 생성자 - 메모리 기반 저장소 사용
     */
    public IdempotentEventProcessor() {
        this(new InMemoryEventRepository());
    }

    /**
     * 이벤트가 이미 처리되었는지 확인합니다.
     *
     * @param event 확인할 이벤트
     * @return 이미 처리된 경우 true, 그렇지 않으면 false
     */
    public boolean isProcessed(Event event) {
        String eventId = event.getEventId();

        // 메모리 캐시 확인
        if (processedEvents.containsKey(eventId)) {
            return true;
        }

        // 저장소 확인
        boolean processed = eventRepository.isProcessed(eventId);
        if (processed) {
            processedEvents.put(eventId, true);
        }

        return processed;
    }

    /**
     * 이벤트를 처리 완료로 표시합니다.
     *
     * @param event 처리된 이벤트
     */
    public void markAsProcessed(Event event) {
        String eventId = event.getEventId();
        processedEvents.put(eventId, true);
        eventRepository.markAsProcessed(eventId, event.getIssuedAt());
    }

    /**
     * 오래된 이벤트 ID를 메모리 캐시에서 제거합니다.
     *
     * @param olderThan 이 기간보다 오래된 이벤트 제거
     */
    public void cleanupOldEvents(Duration olderThan) {
        eventRepository.cleanupOldEvents(olderThan);
        // 메모리 캐시도 주기적으로 정리 필요
    }
} 
