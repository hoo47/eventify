package io.github.event.dispatcher;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.event.annotation.AsyncProcessing;
import io.github.event.core.api.Event;
import io.github.event.core.api.EventDispatcher;
import io.github.event.core.api.EventListener;
import io.github.event.core.model.AbstractEvent;
import io.github.event.core.model.AsyncMode;
import io.github.event.core.model.TransactionPhase;

class DispatcherSelectionStrategyTest {

    private Map<AsyncMode, EventDispatcher> dispatchers;
    private DispatcherSelectionStrategy strategy;
    private TestExecutorDispatcher executorDispatcher;
    private TestRabbitMQDispatcher rabbitMQDispatcher;

    @BeforeEach
    void setUp() {
        dispatchers = new HashMap<>();
        executorDispatcher = new TestExecutorDispatcher();
        rabbitMQDispatcher = new TestRabbitMQDispatcher();
        
        dispatchers.put(AsyncMode.EXECUTOR, executorDispatcher);
        dispatchers.put(AsyncMode.RABBITMQ, rabbitMQDispatcher);
        
        strategy = new DispatcherSelectionStrategy(dispatchers, AsyncMode.EXECUTOR);
    }

    @Test
    void testSelectDispatcher_DefaultMode() {
        // 기본 모드 테스트
        TestEvent event = new TestEvent("test-message");
        TestEventListener listener = new TestEventListener();
        
        EventDispatcher dispatcher = strategy.selectDispatcher(event, listener);
        assertThat(dispatcher).isEqualTo(executorDispatcher);
    }

    @Test
    void testSelectDispatcher_EventAnnotation() {
        // 이벤트 어노테이션 테스트
        RabbitMQEvent event = new RabbitMQEvent("test-message");
        TestEventListener listener = new TestEventListener();
        
        EventDispatcher dispatcher = strategy.selectDispatcher(event, listener);
        assertThat(dispatcher).isEqualTo(rabbitMQDispatcher);
    }

    @Test
    void testSelectDispatcher_ListenerAnnotation() {
        // 리스너 어노테이션 테스트
        TestEvent event = new TestEvent("test-message");
        RabbitMQEventListener listener = new RabbitMQEventListener();
        
        EventDispatcher dispatcher = strategy.selectDispatcher(event, listener);
        assertThat(dispatcher).isEqualTo(rabbitMQDispatcher);
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

    // RabbitMQ 이벤트 클래스
    @AsyncProcessing(mode = AsyncMode.RABBITMQ)
    static class RabbitMQEvent extends AbstractEvent {
        private final String message;
        
        public RabbitMQEvent(String message) {
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

    // RabbitMQ 이벤트 리스너 클래스
    @AsyncProcessing(mode = AsyncMode.RABBITMQ)
    static class RabbitMQEventListener implements EventListener<TestEvent> {
        @Override
        public void onEvent(TestEvent event) {
            // 테스트용 메서드
        }
        
        @Override
        public Class<TestEvent> getEventType() {
            return TestEvent.class;
        }
    }

    // 테스트용 실행기 디스패처 클래스
    static class TestExecutorDispatcher implements EventDispatcher {
        @Override
        public <T extends Event> void dispatch(Event event, EventListener<T> listener) {
            // 테스트용 메서드
        }
        
        @Override
        public <T extends Event> void dispatchAsync(Event event, EventListener<T> listener) {
            // 테스트용 메서드
        }
        
        @Override
        public <T extends Event> void dispatchInTransaction(Event event, EventListener<T> listener, TransactionPhase phase) {
            // 테스트용 메서드
        }
        
        @Override
        public <T extends Event> void dispatchAsyncInTransaction(Event event, EventListener<T> listener, TransactionPhase phase) {
            // 테스트용 메서드
        }
    }

    // RabbitMQ 디스패처 클래스
    static class TestRabbitMQDispatcher implements EventDispatcher {
        @Override
        public <T extends Event> void dispatch(Event event, EventListener<T> listener) {
            // 테스트용 메서드
        }
        
        @Override
        public <T extends Event> void dispatchAsync(Event event, EventListener<T> listener) {
            // 테스트용 메서드
        }
        
        @Override
        public <T extends Event> void dispatchInTransaction(Event event, EventListener<T> listener, TransactionPhase phase) {
            // 테스트용 메서드
        }
        
        @Override
        public <T extends Event> void dispatchAsyncInTransaction(Event event, EventListener<T> listener, TransactionPhase phase) {
            // 테스트용 메서드
        }
    }
} 