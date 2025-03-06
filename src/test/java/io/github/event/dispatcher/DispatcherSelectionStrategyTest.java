package io.github.event.dispatcher;

import java.lang.reflect.Method;
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
import io.github.event.listener.MethodEventListener;

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
        // 어노테이션이 없는 경우 기본 디스패처 선택
        TestEvent event = new TestEvent("test");
        TestEventListener listener = new TestEventListener();
        
        EventDispatcher selectedDispatcher = strategy.selectDispatcher(event, listener);
        
        assertThat(selectedDispatcher).isEqualTo(executorDispatcher);
    }

    @Test
    void testSelectDispatcher_EventAnnotation() {
        // 이벤트 클래스에 어노테이션이 있는 경우
        RabbitMQEvent event = new RabbitMQEvent("test");
        TestEventListener listener = new TestEventListener();
        
        EventDispatcher selectedDispatcher = strategy.selectDispatcher(event, listener);
        
        assertThat(selectedDispatcher).isEqualTo(rabbitMQDispatcher);
    }

    @Test
    void testSelectDispatcher_ListenerAnnotation() {
        // 리스너 클래스에 어노테이션이 있는 경우
        TestEvent event = new TestEvent("test");
        RabbitMQEventListener listener = new RabbitMQEventListener();
        
        EventDispatcher selectedDispatcher = strategy.selectDispatcher(event, listener);
        
        assertThat(selectedDispatcher).isEqualTo(rabbitMQDispatcher);
    }

    @Test
    void testSelectDispatcher_MethodAnnotation() throws NoSuchMethodException {
        // 메서드에 어노테이션이 있는 경우
        TestEvent event = new TestEvent("test");
        TestEventHandler handler = new TestEventHandler();
        Method method = TestEventHandler.class.getMethod("handleWithRabbitMQ", TestEvent.class);
        MethodEventListener<TestEvent> listener = new MethodEventListener<>(
                handler, method, TestEvent.class, true, false, null, false);
        
        EventDispatcher selectedDispatcher = strategy.selectDispatcher(event, listener);
        
        assertThat(selectedDispatcher).isEqualTo(rabbitMQDispatcher);
    }

    @Test
    void testSelectDispatcher_UnsupportedMode() {
        // 지원하지 않는 모드인 경우 기본 디스패처 선택
        dispatchers.remove(AsyncMode.RABBITMQ);
        RabbitMQEvent event = new RabbitMQEvent("test");
        TestEventListener listener = new TestEventListener();
        
        EventDispatcher selectedDispatcher = strategy.selectDispatcher(event, listener);
        
        assertThat(selectedDispatcher).isEqualTo(executorDispatcher);
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

    // RabbitMQ 모드 이벤트 클래스
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

    // RabbitMQ 모드 리스너 클래스
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

    // 테스트용 이벤트 핸들러 클래스
    static class TestEventHandler {
        @AsyncProcessing(mode = AsyncMode.RABBITMQ)
        public void handleWithRabbitMQ(TestEvent event) {
            // 테스트용 메서드
        }
    }

    // 테스트용 Executor 디스패처
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
        public <T extends Event> void dispatchInTransaction(Event event, EventListener<T> listener, io.github.event.core.model.TransactionPhase phase) {
            // 테스트용 메서드
        }
        
        @Override
        public <T extends Event> void dispatchAsyncInTransaction(Event event, EventListener<T> listener, io.github.event.core.model.TransactionPhase phase) {
            // 테스트용 메서드
        }
    }

    // 테스트용 RabbitMQ 디스패처
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
        public <T extends Event> void dispatchInTransaction(Event event, EventListener<T> listener, io.github.event.core.model.TransactionPhase phase) {
            // 테스트용 메서드
        }
        
        @Override
        public <T extends Event> void dispatchAsyncInTransaction(Event event, EventListener<T> listener, io.github.event.core.model.TransactionPhase phase) {
            // 테스트용 메서드
        }
    }
} 