package io.github.event.listener;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.event.core.api.Event;
import io.github.event.core.api.EventListener;
import io.github.event.core.model.AbstractEvent;
import io.github.event.idempotent.IdempotentEventProcessor;

class IdempotentEventListenerTest {

    private TestEventListener delegate;
    private TestEventProcessor idempotentProcessor;
    private IdempotentEventListener<TestEvent> listener;
    private TestEvent testEvent;

    @BeforeEach
    void setUp() {
        delegate = new TestEventListener();
        idempotentProcessor = new TestEventProcessor();
        listener = new IdempotentEventListener<>(delegate, idempotentProcessor);
        testEvent = new TestEvent("test-message");
    }

    @Test
    void testGetEventType() {
        // 이벤트 타입 반환 테스트
        assertThat(listener.getEventType()).isEqualTo(TestEvent.class);
    }

    @Test
    void testOnEvent_NotProcessed() {
        // 처리되지 않은 이벤트 테스트
        listener.onEvent(testEvent);
        
        // 이벤트가 처리되었는지 검증
        assertThat(delegate.getHandledEvents()).hasSize(1);
        assertThat(delegate.getHandledEvents().get(0)).isEqualTo(testEvent);
        
        // 멱등성 처리가 되었는지 검증
        assertThat(idempotentProcessor.getProcessedEvents()).hasSize(1);
        assertThat(idempotentProcessor.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testOnEvent_AlreadyProcessed() {
        // 이미 처리된 이벤트로 설정
        idempotentProcessor.setProcessed(true);
        
        // 이벤트 처리 시도
        listener.onEvent(testEvent);
        
        // 이벤트가 다시 처리되지 않았는지 검증
        assertThat(delegate.getHandledEvents()).isEmpty();
        assertThat(idempotentProcessor.getProcessedEvents()).isEmpty();
    }

    @Test
    void testOnEvent_WithException() {
        // 예외를 던지는 리스너로 설정
        delegate.setThrowException(true);
        
        // 예외가 전파되는지 검증
        assertThatThrownBy(() -> listener.onEvent(testEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("테스트 예외");
        
        // 이벤트가 처리되지 않았는지 검증
        assertThat(idempotentProcessor.getProcessedEvents()).isEmpty();
    }

    @Test
    void testOnEvent_MultipleEvents() {
        // 여러 이벤트 처리 테스트
        TestEvent event1 = new TestEvent("message-1");
        TestEvent event2 = new TestEvent("message-2");
        
        // 첫 번째 이벤트 처리
        listener.onEvent(event1);
        assertThat(delegate.getHandledEvents()).hasSize(1);
        assertThat(delegate.getHandledEvents().get(0)).isEqualTo(event1);
        assertThat(idempotentProcessor.getProcessedEvents()).hasSize(1);
        assertThat(idempotentProcessor.getProcessedEvents().get(0)).isEqualTo(event1);
        
        // 두 번째 이벤트 처리
        listener.onEvent(event2);
        assertThat(delegate.getHandledEvents()).hasSize(2);
        assertThat(delegate.getHandledEvents().get(1)).isEqualTo(event2);
        assertThat(idempotentProcessor.getProcessedEvents()).hasSize(2);
        assertThat(idempotentProcessor.getProcessedEvents().get(1)).isEqualTo(event2);
    }

    // 테스트용 이벤트 클래스
    @Getter
    static class TestEvent extends AbstractEvent {
        private final String message;
        
        public TestEvent(String message) {
            this.message = message;
        }

    }

    // 테스트용 이벤트 리스너 클래스
    static class TestEventListener implements EventListener<TestEvent> {
        @Getter
        private final List<TestEvent> handledEvents = new ArrayList<>();
        @Setter
        private boolean throwException = false;
        
        @Override
        public void onEvent(TestEvent event) {
            if (throwException) {
                throw new RuntimeException("테스트 예외");
            }
            handledEvents.add(event);
        }
        
        @Override
        public Class<TestEvent> getEventType() {
            return TestEvent.class;
        }

    }

    // 테스트용 멱등성 프로세서 클래스
    static class TestEventProcessor extends IdempotentEventProcessor {
        private final List<Event> processedEvents = new ArrayList<>();
        private boolean processed = false;
        
        @Override
        public boolean isProcessed(Event event) {
            return processed;
        }
        
        @Override
        public void markAsProcessed(Event event) {
            processedEvents.add(event);
        }
        
        public List<Event> getProcessedEvents() {
            return processedEvents;
        }
        
        public void setProcessed(boolean processed) {
            this.processed = processed;
        }
    }
} 
