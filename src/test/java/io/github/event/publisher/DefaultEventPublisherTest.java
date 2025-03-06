package io.github.event.publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.event.annotation.AsyncProcessing;
import io.github.event.core.api.Event;
import io.github.event.core.api.EventListener;
import io.github.event.core.api.EventProcessorCallback;
import io.github.event.core.model.AbstractEvent;

class DefaultEventPublisherTest {

    private TestEventProcessorCallback processorCallback;
    private TestExecutor asyncExecutor;
    private DefaultEventPublisher publisher;
    private TestEvent testEvent;
    private TestAsyncEvent testAsyncEvent;
    private TestEventListener testListener;
    private TestAsyncEventListener testAsyncListener;

    @BeforeEach
    void setUp() {
        processorCallback = new TestEventProcessorCallback();
        asyncExecutor = new TestExecutor();
        publisher = new DefaultEventPublisher(processorCallback, asyncExecutor);
        testEvent = new TestEvent("test-message");
        testAsyncEvent = new TestAsyncEvent("test-async-message");
        testListener = new TestEventListener();
        testAsyncListener = new TestAsyncEventListener();
    }

    @Test
    void testPublish_Sync() {
        // 동기 이벤트 발행 테스트
        publisher.publish(testEvent);
        
        // 동기 처리 검증
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        assertThat(asyncExecutor.getExecutedRunnables()).isEmpty();
    }

    @Test
    void testPublish_Async() {
        // 비동기 이벤트 발행 테스트
        publisher.publish(testAsyncEvent);
        
        // 비동기 처리 검증
        assertThat(processorCallback.getProcessedEvents()).isEmpty();
        assertThat(asyncExecutor.getExecutedRunnables()).hasSize(1);
        
        // 비동기 작업 실행
        asyncExecutor.executeAll();
        
        // 실행 결과 검증
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testAsyncEvent);
    }

    @Test
    void testPublish_WithListener_Sync() {
        // 동기 리스너로 이벤트 발행 테스트
        publisher.publish(testEvent, testListener);
        
        // 동기 처리 검증
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        assertThat(processorCallback.getProcessedListeners()).hasSize(1);
        assertThat(processorCallback.getProcessedListeners().get(0)).isEqualTo(testListener);
        assertThat(asyncExecutor.getExecutedRunnables()).isEmpty();
    }

    @Test
    void testPublish_WithListener_Async() {
        // 비동기 리스너로 이벤트 발행 테스트
        publisher.publish(testEvent, testAsyncListener);
        
        // 비동기 처리 검증
        assertThat(processorCallback.getProcessedEvents()).isEmpty();
        assertThat(asyncExecutor.getExecutedRunnables()).hasSize(1);
        
        // 비동기 작업 실행
        asyncExecutor.executeAll();
        
        // 실행 결과 검증
        assertThat(processorCallback.getProcessedEvents()).hasSize(1);
        assertThat(processorCallback.getProcessedEvents().get(0)).isEqualTo(testEvent);
        assertThat(processorCallback.getProcessedListeners()).hasSize(1);
        assertThat(processorCallback.getProcessedListeners().get(0)).isEqualTo(testAsyncListener);
    }

    @Test
    void testPublish_AsyncWithoutExecutor() {
        // 실행기가 없는 발행자 생성
        DefaultEventPublisher publisherWithoutExecutor = new DefaultEventPublisher(processorCallback);
        
        // 비동기 이벤트 발행 시 예외 발생 검증
        assertThatThrownBy(() -> publisherWithoutExecutor.publish(testAsyncEvent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("비동기 실행기가 설정되지 않았습니다");
    }

    @Test
    void testPublish_WithError() {
        // 예외 발생 설정
        processorCallback.setThrowException(true);
        processorCallback.setExceptionMessage("테스트 예외");
        
        // 예외 전파 검증
        assertThatThrownBy(() -> publisher.publish(testEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("테스트 예외");
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

    // 테스트용 비동기 이벤트 클래스
    @AsyncProcessing
    static class TestAsyncEvent extends AbstractEvent {
        private final String message;
        
        public TestAsyncEvent(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }

    // 테스트용 이벤트 리스너 클래스
    static class TestEventListener implements EventListener<TestEvent> {
        @Override
        public void onEvent(TestEvent event) {
            // 테스트용 메서드
        }
        
        @Override
        public Class<TestEvent> getEventType() {
            return TestEvent.class;
        }
    }

    // 테스트용 비동기 이벤트 리스너 클래스
    @AsyncProcessing
    static class TestAsyncEventListener implements EventListener<TestEvent> {
        @Override
        public void onEvent(TestEvent event) {
            // 테스트용 메서드
        }
        
        @Override
        public Class<TestEvent> getEventType() {
            return TestEvent.class;
        }
    }

    // 테스트용 이벤트 프로세서 콜백 클래스
    static class TestEventProcessorCallback implements EventProcessorCallback {
        private final List<Event> processedEvents = new ArrayList<>();
        private final List<EventListener<?>> processedListeners = new ArrayList<>();
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
            processedListeners.add(listener);
        }
        
        public List<Event> getProcessedEvents() {
            return processedEvents;
        }
        
        public List<EventListener<?>> getProcessedListeners() {
            return processedListeners;
        }
        
        public void setThrowException(boolean throwException) {
            this.throwException = throwException;
        }
        
        public void setExceptionMessage(String exceptionMessage) {
            this.exceptionMessage = exceptionMessage;
        }
    }

    // 테스트용 실행기 클래스
    static class TestExecutor implements Executor {
        private final List<Runnable> executedRunnables = new ArrayList<>();
        
        @Override
        public void execute(Runnable command) {
            executedRunnables.add(command);
        }
        
        public List<Runnable> getExecutedRunnables() {
            return executedRunnables;
        }
        
        public void executeAll() {
            List<Runnable> runnables = new ArrayList<>(executedRunnables);
            executedRunnables.clear();
            runnables.forEach(Runnable::run);
        }
    }
} 