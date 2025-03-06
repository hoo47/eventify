package io.github.event.listener;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.event.core.model.AbstractEvent;
import io.github.event.core.model.TransactionPhase;

class MethodEventListenerTest {

    private TestEventHandler handler;
    private TestEvent testEvent;
    private Method defaultMethod;
    private MethodEventListener<TestEvent> listener;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        handler = new TestEventHandler();
        testEvent = new TestEvent("test-message");
        defaultMethod = TestEventHandler.class.getMethod("handleEvent", TestEvent.class);
        
        // 기본 리스너 생성
        listener = new MethodEventListener<>(
                handler,
                defaultMethod,
                TestEvent.class,
                false,
                false,
                TransactionPhase.AFTER_COMMIT,
                false
        );
    }

    @Test
    void testOnEvent() {
        // 이벤트 처리 테스트
        listener.onEvent(testEvent);
        
        // 이벤트가 처리되었는지 검증
        assertThat(handler.isEventHandled()).isTrue();
        assertThat(handler.getLastEvent()).isEqualTo(testEvent);
    }

    @Test
    void testOnEvent_WithException() {
        // 예외를 던지는 메서드로 리스너 생성
        try {
            Method exceptionMethod = TestEventHandler.class.getMethod("handleEventWithException", TestEvent.class);
            MethodEventListener<TestEvent> exceptionListener = new MethodEventListener<>(
                    handler,
                    exceptionMethod,
                    TestEvent.class,
                    false,
                    false,
                    TransactionPhase.AFTER_COMMIT,
                    false
            );
            
            // 예외가 발생하는지 검증
            assertThatThrownBy(() -> exceptionListener.onEvent(testEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("이벤트 처리 중 오류 발생");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetEventType() {
        // 이벤트 타입 반환 테스트
        assertThat(listener.getEventType()).isEqualTo(TestEvent.class);
    }

    @Test
    void testGetMethod() {
        // 메서드 반환 테스트
        assertThat(listener.getMethod()).isEqualTo(defaultMethod);
    }

    @Test
    void testGetTargetClass() {
        // 대상 클래스 반환 테스트
        assertThat(listener.getTargetClass()).isEqualTo(TestEventHandler.class);
    }

    @Test
    void testGetMethodName() {
        // 메서드 이름 반환 테스트
        assertThat(listener.getMethodName()).isEqualTo("handleEvent");
    }

    @Test
    void testIsAsync() {
        // 비동기 플래그 테스트
        assertThat(listener.isAsync()).isFalse();
        
        // 비동기 리스너 생성
        MethodEventListener<TestEvent> asyncListener = new MethodEventListener<>(
                handler,
                defaultMethod,
                TestEvent.class,
                true,
                false,
                TransactionPhase.AFTER_COMMIT,
                false
        );
        
        assertThat(asyncListener.isAsync()).isTrue();
    }

    @Test
    void testIsTransactional() {
        // 트랜잭션 플래그 테스트
        assertThat(listener.isTransactional()).isFalse();
        
        // 트랜잭션 리스너 생성
        MethodEventListener<TestEvent> txListener = new MethodEventListener<>(
                handler,
                defaultMethod,
                TestEvent.class,
                false,
                true,
                TransactionPhase.AFTER_COMMIT,
                false
        );
        
        assertThat(txListener.isTransactional()).isTrue();
    }

    @Test
    void testGetTransactionPhase() {
        // 트랜잭션 단계 테스트
        assertThat(listener.getTransactionPhase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        
        // 다른 트랜잭션 단계로 리스너 생성
        MethodEventListener<TestEvent> beforeCommitListener = new MethodEventListener<>(
                handler,
                defaultMethod,
                TestEvent.class,
                false,
                true,
                TransactionPhase.BEFORE_COMMIT,
                false
        );
        
        assertThat(beforeCommitListener.getTransactionPhase()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
    }

    @Test
    void testIsIdempotent() {
        // 멱등성 플래그 테스트
        assertThat(listener.isIdempotent()).isFalse();
        
        // 멱등성 리스너 생성
        MethodEventListener<TestEvent> idempotentListener = new MethodEventListener<>(
                handler,
                defaultMethod,
                TestEvent.class,
                false,
                false,
                TransactionPhase.AFTER_COMMIT,
                true
        );
        
        assertThat(idempotentListener.isIdempotent()).isTrue();
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
    
    // 테스트용 이벤트 핸들러 클래스
    static class TestEventHandler {
        private boolean eventHandled = false;
        private TestEvent lastEvent = null;
        
        public void handleEvent(TestEvent event) {
            eventHandled = true;
            lastEvent = event;
        }
        
        public void handleEventWithException(TestEvent event) {
            throw new IllegalStateException("테스트 예외");
        }
        
        public boolean isEventHandled() {
            return eventHandled;
        }
        
        public TestEvent getLastEvent() {
            return lastEvent;
        }
    }
} 