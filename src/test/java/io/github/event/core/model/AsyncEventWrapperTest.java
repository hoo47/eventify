package io.github.event.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import io.github.event.core.api.EventListener;

class AsyncEventWrapperTest {

    @Test
    void testBasicConstructor() {
        // 테스트 이벤트 및 리스너 생성
        TestEvent event = new TestEvent("test-event");
        TestEventListener listener = new TestEventListener();
        
        // 기본 생성자로 래퍼 생성
        AsyncEventWrapper<TestEvent> wrapper = new AsyncEventWrapper<>(event, listener);
        
        // 기본 속성 검증
        assertThat(wrapper.getEvent()).isEqualTo(event);
        assertThat(wrapper.getListener()).isEqualTo(listener);
        assertThat(wrapper.getListenerClassName()).isEqualTo(listener.getClass().getName());
        assertThat(wrapper.getListenerMethodName()).isEqualTo("onEvent");
        assertThat(wrapper.getTransactionPhase()).isNull();
    }
    
    @Test
    void testConstructorWithTransactionPhase() {
        // 테스트 이벤트 및 리스너 생성
        TestEvent event = new TestEvent("test-event");
        TestEventListener listener = new TestEventListener();
        TransactionPhase phase = TransactionPhase.AFTER_COMMIT;
        
        // 트랜잭션 단계가 있는 생성자로 래퍼 생성
        AsyncEventWrapper<TestEvent> wrapper = new AsyncEventWrapper<>(event, listener, phase);
        
        // 속성 검증
        assertThat(wrapper.getEvent()).isEqualTo(event);
        assertThat(wrapper.getListener()).isEqualTo(listener);
        assertThat(wrapper.getListenerClassName()).isEqualTo(listener.getClass().getName());
        assertThat(wrapper.getListenerMethodName()).isEqualTo("onEvent");
        assertThat(wrapper.getTransactionPhase()).isEqualTo(phase);
    }
    
    @Test
    void testConstructorWithMethodName() {
        // 테스트 이벤트 및 리스너 생성
        TestEvent event = new TestEvent("test-event");
        TestEventListener listener = new TestEventListener();
        String methodName = "customMethod";
        TransactionPhase phase = TransactionPhase.AFTER_ROLLBACK;
        
        // 메서드 이름과 트랜잭션 단계가 있는 생성자로 래퍼 생성
        AsyncEventWrapper<TestEvent> wrapper = new AsyncEventWrapper<>(event, listener, methodName, phase);
        
        // 속성 검증
        assertThat(wrapper.getEvent()).isEqualTo(event);
        assertThat(wrapper.getListener()).isEqualTo(listener);
        assertThat(wrapper.getListenerClassName()).isEqualTo(listener.getClass().getName());
        assertThat(wrapper.getListenerMethodName()).isEqualTo(methodName);
        assertThat(wrapper.getTransactionPhase()).isEqualTo(phase);
    }
    
    @Test
    void testJsonConstructor() {
        // 테스트 이벤트 생성
        TestEvent event = new TestEvent("test-event");
        String listenerClassName = "io.github.event.test.TestListener";
        String methodName = "handleEvent";
        TransactionPhase phase = TransactionPhase.BEFORE_COMMIT;
        
        // JSON 생성자로 래퍼 생성
        AsyncEventWrapper<TestEvent> wrapper = new AsyncEventWrapper<>(
                event, listenerClassName, methodName, phase);
        
        // 속성 검증
        assertThat(wrapper.getEvent()).isEqualTo(event);
        assertThat(wrapper.getListener()).isNull(); // JSON 생성자는 리스너 인스턴스를 설정하지 않음
        assertThat(wrapper.getListenerClassName()).isEqualTo(listenerClassName);
        assertThat(wrapper.getListenerMethodName()).isEqualTo(methodName);
        assertThat(wrapper.getTransactionPhase()).isEqualTo(phase);
    }
    
    @Test
    void testSetListener() {
        // 테스트 이벤트 및 리스너 생성
        TestEvent event = new TestEvent("test-event");
        TestEventListener listener1 = new TestEventListener();
        TestEventListener listener2 = new TestEventListener();
        
        // 래퍼 생성
        AsyncEventWrapper<TestEvent> wrapper = new AsyncEventWrapper<>(event, listener1);
        
        // 초기 리스너 검증
        assertThat(wrapper.getListener()).isEqualTo(listener1);
        
        // 리스너 변경
        wrapper.setListener(listener2);
        
        // 변경된 리스너 검증
        assertThat(wrapper.getListener()).isEqualTo(listener2);
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
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestEvent that = (TestEvent) obj;
            return message.equals(that.message) && getEventId().equals(that.getEventId());
        }
        
        @Override
        public int hashCode() {
            return message.hashCode() + getEventId().hashCode();
        }
    }
    
    // 테스트용 이벤트 리스너 클래스
    static class TestEventListener implements EventListener<TestEvent> {
        private boolean eventHandled = false;
        
        @Override
        public void onEvent(TestEvent event) {
            eventHandled = true;
        }
        
        @Override
        public Class<TestEvent> getEventType() {
            return TestEvent.class;
        }
        
        public boolean isEventHandled() {
            return eventHandled;
        }
    }
} 