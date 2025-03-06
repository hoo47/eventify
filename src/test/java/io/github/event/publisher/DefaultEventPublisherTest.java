package io.github.event.publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.event.core.api.Event;
import io.github.event.core.api.EventListener;
import io.github.event.core.api.EventProcessorCallback;
import io.github.event.core.model.AbstractEvent;

class DefaultEventPublisherTest {

    private TestEventProcessorCallback processorCallback;
    private DefaultEventPublisher publisher;
    private TestEvent testEvent;

    @BeforeEach
    void setUp() {
        processorCallback = new TestEventProcessorCallback();
        publisher = new DefaultEventPublisher(processorCallback);
        testEvent = new TestEvent("test-message");
    }

    @Test
    void testPublish() {
        // 이벤트 발행 테스트
        publisher.publish(testEvent);
        
        // processorCallback.multicast가 호출되었는지 검증
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishSync() {
        // 동기 이벤트 발행 테스트
        publisher.publishSync(testEvent);
        
        // processorCallback.multicast가 호출되었는지 검증
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
    }

    @Test
    void testPublishAsync_Success() throws ExecutionException, InterruptedException, TimeoutException {
        // 비동기 이벤트 발행 성공 테스트
        CompletableFuture<Void> future = publisher.publishAsync(testEvent);
        
        // processorCallback.multicast가 호출되었는지 검증
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        
        // Future가 완료되었는지 검증
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCompletedExceptionally()).isFalse();
        
        // Future의 결과 검증 (null이어야 함)
        assertThat(future.get(1, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void testPublishAsync_Failure() {
        // processorCallback이 예외를 던지도록 설정
        processorCallback.setThrowException(true);
        processorCallback.setExceptionMessage("테스트 예외");
        
        // 비동기 이벤트 발행 실패 테스트
        CompletableFuture<Void> future = publisher.publishAsync(testEvent);
        
        // Future가 예외로 완료되었는지 검증
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCompletedExceptionally()).isTrue();
        
        // Future의 예외 검증
        assertThatThrownBy(() -> future.get())
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(RuntimeException.class)
            .hasMessageContaining("테스트 예외");
    }

    // 테스트용 이벤트 프로세서 콜백 클래스
    static class TestEventProcessorCallback implements EventProcessorCallback {
        private final List<Event> processedEvents = new ArrayList<>();
        private boolean throwException = false;
        private String exceptionMessage = "테스트 예외";
        
        @Override
        public void multicast(Event event) {
            if (throwException) {
                throw new RuntimeException(exceptionMessage);
            }
            processedEvents.add(event);
        }
        
        @Override
        public <T extends Event> void processEvent(Event event, EventListener<?> listener) {
            if (throwException) {
                throw new RuntimeException(exceptionMessage);
            }
            processedEvents.add(event);
        }
        
        public List<Event> getProcessedEvents() {
            return processedEvents;
        }
        
        public void setThrowException(boolean throwException) {
            this.throwException = throwException;
        }
        
        public void setExceptionMessage(String exceptionMessage) {
            this.exceptionMessage = exceptionMessage;
        }
    }

    // 테스트용 이벤트 클래스
    static class TestEvent extends AbstractEvent {
        private final String message;
        
        public TestEvent(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
} 