package io.github.event.idempotent;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryEventRepositoryTest {

    private InMemoryEventRepository repository;
    private String eventId;
    private Instant processedAt;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEventRepository();
        eventId = UUID.randomUUID().toString();
        processedAt = Instant.now();
    }

    @Test
    void testIsProcessed_NotProcessed() {
        // 처리되지 않은 이벤트 ID 테스트
        boolean processed = repository.isProcessed(eventId);
        
        // 처리되지 않았는지 검증
        assertThat(processed).isFalse();
    }

    @Test
    void testMarkAsProcessed() {
        // 이벤트 처리 완료 표시
        repository.markAsProcessed(eventId, processedAt);
        
        // 처리되었는지 검증
        boolean processed = repository.isProcessed(eventId);
        assertThat(processed).isTrue();
    }

    @Test
    void testCleanupOldEvents() {
        // 이벤트 처리 완료 표시 (현재 시간)
        String recentEventId = UUID.randomUUID().toString();
        repository.markAsProcessed(recentEventId, Instant.now());
        
        // 이벤트 처리 완료 표시 (1시간 전)
        String oldEventId = UUID.randomUUID().toString();
        repository.markAsProcessed(oldEventId, Instant.now().minus(1, ChronoUnit.HOURS));
        
        // 이벤트 처리 완료 표시 (2시간 전)
        String veryOldEventId = UUID.randomUUID().toString();
        repository.markAsProcessed(veryOldEventId, Instant.now().minus(2, ChronoUnit.HOURS));
        
        // 모든 이벤트가 처리되었는지 검증
        assertThat(repository.isProcessed(recentEventId)).isTrue();
        assertThat(repository.isProcessed(oldEventId)).isTrue();
        assertThat(repository.isProcessed(veryOldEventId)).isTrue();
        
        // 90분보다 오래된 이벤트 정리
        repository.cleanupOldEvents(Duration.ofMinutes(90));
        
        // 정리 후 상태 검증
        assertThat(repository.isProcessed(recentEventId)).isTrue(); // 최근 이벤트는 유지
        assertThat(repository.isProcessed(oldEventId)).isTrue(); // 1시간 전 이벤트는 유지
        assertThat(repository.isProcessed(veryOldEventId)).isFalse(); // 2시간 전 이벤트는 제거
    }

    @Test
    void testCleanupOldEvents_NoEvents() {
        // 이벤트가 없는 상태에서 정리
        repository.cleanupOldEvents(Duration.ofHours(1));
        
        // 예외가 발생하지 않고 정상 처리되는지 검증
        assertThat(repository.isProcessed(eventId)).isFalse();
    }

    @Test
    void testMultipleEvents() {
        // 여러 이벤트 처리 완료 표시
        String eventId1 = UUID.randomUUID().toString();
        String eventId2 = UUID.randomUUID().toString();
        String eventId3 = UUID.randomUUID().toString();
        
        repository.markAsProcessed(eventId1, Instant.now());
        repository.markAsProcessed(eventId2, Instant.now());
        
        // 처리 상태 검증
        assertThat(repository.isProcessed(eventId1)).isTrue();
        assertThat(repository.isProcessed(eventId2)).isTrue();
        assertThat(repository.isProcessed(eventId3)).isFalse();
    }
} 