package io.github.event.core.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractEventTest {

    @Test
    void testEventCreation() {
        // 테스트용 이벤트 생성
        TestEvent event = new TestEvent("test-message");
        
        // 기본 속성 검증
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getIssuedAt()).isNotNull();
        assertThat(event.getIssuedAt()).isBefore(Instant.now());
        assertThat(event.getMetadata()).isNotNull();
    }
    
    @Test
    void testCustomEventId() {
        // 커스텀 이벤트 ID 생성
        String customId = UUID.randomUUID().toString();
        TestEvent event = new TestEvent("test-message", customId);
        
        // 커스텀 ID 검증
        assertThat(event.getEventId()).isEqualTo(customId);
    }
    
    @Test
    void testEventMetadata() {
        // 테스트용 이벤트 생성
        TestEvent event = new TestEvent("test-message");
        
        // 메타데이터 설정
        event.addMetadata("testKey", "testValue");
        event.addMetadata("numericKey", 123);
        
        // 메타데이터 검증
        assertThat(event.getMetadata()).containsKey("testKey");
        assertThat(event.getMetadata().get("testKey")).isEqualTo("testValue");
        assertThat(event.getMetadata().get("numericKey")).isEqualTo(123);
        assertThat(event.getMetadata()).doesNotContainKey("nonExistentKey");
    }
    
    @Test
    void testCustomTimestamp() {
        // 커스텀 타임스탬프 생성
        Instant customTime = Instant.now().minusSeconds(3600); // 1시간 전
        String customId = UUID.randomUUID().toString();
        TestEvent event = new TestEvent("test-message", customId, customTime);
        
        // 커스텀 타임스탬프 검증
        assertThat(event.getIssuedAt()).isEqualTo(customTime);
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