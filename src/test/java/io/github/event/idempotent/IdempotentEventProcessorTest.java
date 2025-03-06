package io.github.event.idempotent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.event.core.model.AbstractEvent;

class IdempotentEventProcessorTest {

    private TestEventRepository eventRepository;
    private IdempotentEventProcessor processor;
    private TestEvent testEvent;

    @BeforeEach
    void setUp() {
        eventRepository = new TestEventRepository();
        processor = new IdempotentEventProcessor(eventRepository);
        testEvent = new TestEvent("test-message");
    }

    @Test
    void testIsProcessed_NotProcessed() {
        // 처리되지 않은 이벤트 테스트
        boolean processed = processor.isProcessed(testEvent);
        
        // 처리되지 않았는지 검증
        assertThat(processed).isFalse();
        
        // 저장소 호출 검증
        assertThat(eventRepository.getCheckedEventIds()).hasSize(1);
        assertThat(eventRepository.getCheckedEventIds().get(0)).isEqualTo(testEvent.getEventId());
    }

    @Test
    void testIsProcessed_AlreadyProcessed() {
        // 이미 처리된 이벤트 테스트
        eventRepository.setProcessed(true);
        boolean processed = processor.isProcessed(testEvent);
        
        // 처리되었는지 검증
        assertThat(processed).isTrue();
        
        // 저장소 호출 검증
        assertThat(eventRepository.getCheckedEventIds()).hasSize(1);
        assertThat(eventRepository.getCheckedEventIds().get(0)).isEqualTo(testEvent.getEventId());
    }

    @Test
    void testMarkAsProcessed() {
        // 이벤트 처리 완료 표시 테스트
        processor.markAsProcessed(testEvent);
        
        // 저장소 호출 검증
        assertThat(eventRepository.getProcessedEventIds()).hasSize(1);
        assertThat(eventRepository.getProcessedEventIds().get(0)).isEqualTo(testEvent.getEventId());
        assertThat(eventRepository.getProcessedTimestamps()).hasSize(1);
        assertThat(eventRepository.getProcessedTimestamps().get(0)).isEqualTo(testEvent.getIssuedAt());
    }

    @Test
    void testIsProcessed_CacheHit() {
        // 이벤트 처리 완료 표시
        processor.markAsProcessed(testEvent);
        
        // 저장소 초기화
        eventRepository.reset();
        
        // 캐시에서 확인
        boolean processed = processor.isProcessed(testEvent);
        
        // 처리되었는지 검증
        assertThat(processed).isTrue();
        
        // 저장소가 호출되지 않았는지 검증 (캐시 히트)
        assertThat(eventRepository.getCheckedEventIds()).isEmpty();
    }

    @Test
    void testCleanupOldEvents() {
        // 오래된 이벤트 정리 테스트
        Duration olderThan = Duration.ofHours(1);
        processor.cleanupOldEvents(olderThan);
        
        // 저장소 호출 검증
        assertThat(eventRepository.getCleanupCalls()).hasSize(1);
        assertThat(eventRepository.getCleanupCalls().get(0)).isEqualTo(olderThan);
    }

    @Test
    void testDefaultConstructor() {
        // 기본 생성자 테스트
        IdempotentEventProcessor defaultProcessor = new IdempotentEventProcessor();
        
        // 기본 생성자로 생성된 프로세서가 정상 동작하는지 검증
        boolean processed = defaultProcessor.isProcessed(testEvent);
        assertThat(processed).isFalse();
        
        defaultProcessor.markAsProcessed(testEvent);
        processed = defaultProcessor.isProcessed(testEvent);
        assertThat(processed).isTrue();
    }

    // 테스트용 이벤트 저장소 클래스
    static class TestEventRepository implements EventRepository {
        private final List<String> checkedEventIds = new ArrayList<>();
        private final List<String> processedEventIds = new ArrayList<>();
        private final List<Instant> processedTimestamps = new ArrayList<>();
        private final List<Duration> cleanupCalls = new ArrayList<>();
        private boolean processed = false;
        
        @Override
        public boolean isProcessed(String eventId) {
            checkedEventIds.add(eventId);
            return processed;
        }
        
        @Override
        public void markAsProcessed(String eventId, Instant processedAt) {
            processedEventIds.add(eventId);
            processedTimestamps.add(processedAt);
        }
        
        @Override
        public void cleanupOldEvents(Duration olderThan) {
            cleanupCalls.add(olderThan);
        }
        
        public void setProcessed(boolean processed) {
            this.processed = processed;
        }
        
        public void reset() {
            checkedEventIds.clear();
            processedEventIds.clear();
            processedTimestamps.clear();
            cleanupCalls.clear();
            processed = false;
        }
        
        public List<String> getCheckedEventIds() {
            return checkedEventIds;
        }
        
        public List<String> getProcessedEventIds() {
            return processedEventIds;
        }
        
        public List<Instant> getProcessedTimestamps() {
            return processedTimestamps;
        }
        
        public List<Duration> getCleanupCalls() {
            return cleanupCalls;
        }
    }
    
    // 테스트용 이벤트 클래스
    static class TestEvent extends AbstractEvent {
        private final String message;
        
        public TestEvent(String message) {
            this.message = message;
        }
        
        public TestEvent(String message, String eventId) {
            super(eventId);
            this.message = message;
        }
        
        public TestEvent(String message, String eventId, Instant issuedAt) {
            super(eventId, issuedAt);
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
} 