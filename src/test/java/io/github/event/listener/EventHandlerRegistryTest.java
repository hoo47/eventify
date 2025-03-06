package io.github.event.listener;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.event.annotation.AsyncListener;
import io.github.event.annotation.AsyncTransactionalListener;
import io.github.event.annotation.EventHandler;
import io.github.event.annotation.ListenerMethod;
import io.github.event.annotation.TransactionalListener;
import io.github.event.core.api.Event;
import io.github.event.core.api.EventListener;
import io.github.event.core.model.AbstractEvent;
import io.github.event.core.model.TransactionPhase;

class EventHandlerRegistryTest {

    private TestApplicationMulticaster multicaster;
    private EventHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        multicaster = new TestApplicationMulticaster();
        registry = new EventHandlerRegistry(multicaster);
    }

    @Test
    void testRegisterHandler_WithoutAnnotation() {
        // @EventHandler 어노테이션이 없는 클래스 등록 테스트
        NonAnnotatedHandler handler = new NonAnnotatedHandler();
        registry.registerHandler(handler);
        
        // 리스너가 등록되지 않았는지 검증
        assertThat(multicaster.getListeners()).isEmpty();
    }

    @Test
    void testRegisterHandler_WithListenerMethod() {
        // @ListenerMethod 어노테이션이 있는 핸들러 등록 테스트
        TestEventHandler handler = new TestEventHandler();
        registry.registerHandler(handler);
        
        // 리스너가 등록되었는지 검증
        assertThat(multicaster.getListeners()).hasSize(4);
        
        // 리스너 설정 검증
        MethodEventListener<?> listener = findListenerByMethodName("handleDefaultEvent");
        assertThat(listener).isNotNull();
        assertThat(listener.getEventType()).isEqualTo(TestEvent.class);
        assertThat(listener.isAsync()).isFalse();
        assertThat(listener.isTransactional()).isFalse();
        assertThat(listener.isIdempotent()).isFalse();
    }

    @Test
    void testRegisterHandler_WithAsyncListener() {
        // @AsyncListener 어노테이션이 있는 핸들러 등록 테스트
        TestEventHandler handler = new TestEventHandler();
        registry.registerHandler(handler);
        
        // 리스너 설정 검증
        MethodEventListener<?> listener = findListenerByMethodName("handleAsyncEvent");
        assertThat(listener).isNotNull();
        assertThat(listener.getEventType()).isEqualTo(TestEvent.class);
        assertThat(listener.isAsync()).isTrue();
        assertThat(listener.isTransactional()).isFalse();
    }

    @Test
    void testRegisterHandler_WithTransactionalListener() {
        // @TransactionalListener 어노테이션이 있는 핸들러 등록 테스트
        TestEventHandler handler = new TestEventHandler();
        registry.registerHandler(handler);
        
        // 리스너 설정 검증
        MethodEventListener<?> listener = findListenerByMethodName("handleTransactionalEvent");
        assertThat(listener).isNotNull();
        assertThat(listener.getEventType()).isEqualTo(TestEvent.class);
        assertThat(listener.isAsync()).isFalse();
        assertThat(listener.isTransactional()).isTrue();
        assertThat(listener.getTransactionPhase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    @Test
    void testRegisterHandler_WithAsyncTransactionalListener() {
        // @AsyncTransactionalListener 어노테이션이 있는 핸들러 등록 테스트
        TestEventHandler handler = new TestEventHandler();
        registry.registerHandler(handler);
        
        // 리스너 설정 검증
        MethodEventListener<?> listener = findListenerByMethodName("handleAsyncTransactionalEvent");
        assertThat(listener).isNotNull();
        assertThat(listener.getEventType()).isEqualTo(TestEvent.class);
        assertThat(listener.isAsync()).isTrue();
        assertThat(listener.isTransactional()).isTrue();
        assertThat(listener.getTransactionPhase()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
    }

    @Test
    void testGetHandler() {
        // 핸들러 인스턴스 조회 테스트
        TestEventHandler handler = new TestEventHandler();
        registry.registerHandler(handler);
        
        // 등록된 핸들러 인스턴스 조회
        Object retrievedHandler = registry.getHandler(TestEventHandler.class);
        assertThat(retrievedHandler).isEqualTo(handler);
        
        // 등록되지 않은 핸들러 인스턴스 조회
        Object nonExistingHandler = registry.getHandler(NonAnnotatedHandler.class);
        assertThat(nonExistingHandler).isNull();
    }

    private MethodEventListener<?> findListenerByMethodName(String methodName) {
        return multicaster.getListeners().stream()
                .filter(listener -> listener instanceof MethodEventListener)
                .map(listener -> (MethodEventListener<?>) listener)
                .filter(listener -> listener.getMethodName().equals(methodName))
                .findFirst()
                .orElse(null);
    }

    // 테스트용 멀티캐스터 클래스
    static class TestApplicationMulticaster extends ApplicationMulticaster {
        private final List<EventListener<?>> listeners = new ArrayList<>();
        
        public TestApplicationMulticaster() {
            super(null, null);
        }
        
        @Override
        public <T extends Event> void addListener(EventListener<T> listener) {
            listeners.add(listener);
        }
        
        public List<EventListener<?>> getListeners() {
            return listeners;
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
    
    // 테스트용 이벤트 핸들러 클래스
    @EventHandler
    static class TestEventHandler {
        
        @ListenerMethod(TestEvent.class)
        public void handleDefaultEvent(TestEvent event) {
            // 기본 리스너 메서드
        }
        
        @AsyncListener
        public void handleAsyncEvent(TestEvent event) {
            // 비동기 리스너 메서드
        }
        
        @TransactionalListener
        public void handleTransactionalEvent(TestEvent event) {
            // 트랜잭션 리스너 메서드
        }
        
        @AsyncTransactionalListener(TransactionPhase.BEFORE_COMMIT)
        public void handleAsyncTransactionalEvent(TestEvent event) {
            // 비동기 트랜잭션 리스너 메서드
        }
    }
    
    // 어노테이션이 없는 핸들러 클래스
    static class NonAnnotatedHandler {
        
        public void handleEvent(TestEvent event) {
            // 어노테이션이 없는 메서드
        }
    }
} 