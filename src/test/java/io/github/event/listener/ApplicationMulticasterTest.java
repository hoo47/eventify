package io.github.event.listener;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.event.core.api.Event;
import io.github.event.core.api.EventDispatcher;
import io.github.event.core.api.EventListener;
import io.github.event.core.model.AbstractEvent;
import io.github.event.core.model.TransactionPhase;
import io.github.event.idempotent.EventRepository;
import io.github.event.idempotent.IdempotentEventProcessor;

class ApplicationMulticasterTest {

    private TestEventDispatcher eventDispatcher;
    private TestEventRepository eventRepository;
    private IdempotentEventProcessor idempotentProcessor;
    private ApplicationMulticaster multicaster;
    private TestEvent testEvent;
    private TestEventListener testListener;
    private MethodEventListener<TestEvent> methodListener;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        eventDispatcher = new TestEventDispatcher();
        eventRepository = new TestEventRepository();
        idempotentProcessor = new IdempotentEventProcessor(eventRepository);
        multicaster = new ApplicationMulticaster(eventDispatcher, idempotentProcessor);
        testEvent = new TestEvent("test-message");
        testListener = new TestEventListener();
        
        // 메서드 리스너 생성
        TestEventHandler handler = new TestEventHandler();
        Method method = TestEventHandler.class.getMethod("handleEvent", TestEvent.class);
        methodListener = new MethodEventListener<>(
                handler,
                method,
                TestEvent.class,
                false,
                false,
                TransactionPhase.AFTER_COMMIT,
                false
        );
    }

    @Test
    void testAddListener() {
        // 리스너 등록 테스트
        multicaster.addListener(testListener);
        
        // 이벤트 멀티캐스트
        multicaster.multicast(testEvent);
        
        // 이벤트가 디스패치되었는지 검증
        assertThat(eventDispatcher.getDispatchedEvents()).hasSize(1);
        assertThat(eventDispatcher.getDispatchedEvents().get(0)).isEqualTo(testEvent);
        assertThat(eventDispatcher.getDispatchedListeners()).hasSize(1);
        assertThat(eventDispatcher.getDispatchedListeners().get(0)).isEqualTo(testListener);
    }

    @Test
    void testMulticast_WithoutListeners() {
        // 리스너가 없는 상태에서 멀티캐스트
        multicaster.multicast(testEvent);
        
        // 이벤트가 디스패치되지 않았는지 검증
        assertThat(eventDispatcher.getDispatchedEvents()).isEmpty();
        assertThat(eventDispatcher.getDispatchedListeners()).isEmpty();
    }

    @Test
    void testMulticast_WithMultipleListeners() {
        // 여러 리스너 등록
        multicaster.addListener(testListener);
        multicaster.addListener(methodListener);
        
        // 이벤트 멀티캐스트
        multicaster.multicast(testEvent);
        
        // 모든 리스너에게 이벤트가 디스패치되었는지 검증
        assertThat(eventDispatcher.getDispatchedEvents()).hasSize(2);
        assertThat(eventDispatcher.getDispatchedEvents()).containsExactly(testEvent, testEvent);
        assertThat(eventDispatcher.getDispatchedListeners()).hasSize(2);
        assertThat(eventDispatcher.getDispatchedListeners()).containsExactly(testListener, methodListener);
    }

    @Test
    void testProcessEvent_Sync() {
        // 동기 이벤트 처리 테스트
        multicaster.processEvent(testEvent, testListener);
        
        // 이벤트가 동기적으로 디스패치되었는지 검증
        assertThat(eventDispatcher.getDispatchedEvents()).hasSize(1);
        assertThat(eventDispatcher.getDispatchedEvents().get(0)).isEqualTo(testEvent);
        assertThat(eventDispatcher.getDispatchedListeners()).hasSize(1);
        assertThat(eventDispatcher.getDispatchedListeners().get(0)).isEqualTo(testListener);
        assertThat(eventDispatcher.getAsyncDispatches()).hasSize(0);
        assertThat(eventDispatcher.getTransactionDispatches()).hasSize(0);
    }

    @Test
    void testProcessEvent_Async() throws NoSuchMethodException {
        // 비동기 메서드 리스너 생성
        TestEventHandler handler = new TestEventHandler();
        Method method = TestEventHandler.class.getMethod("handleEvent", TestEvent.class);
        MethodEventListener<TestEvent> asyncListener = new MethodEventListener<>(
                handler,
                method,
                TestEvent.class,
                true,
                false,
                TransactionPhase.AFTER_COMMIT,
                false
        );
        
        // 비동기 이벤트 처리 테스트
        multicaster.processEvent(testEvent, asyncListener);
        
        // 이벤트가 비동기적으로 디스패치되었는지 검증
        assertThat(eventDispatcher.getDispatchedEvents()).hasSize(0);
        assertThat(eventDispatcher.getDispatchedListeners()).hasSize(0);
        assertThat(eventDispatcher.getAsyncDispatches()).hasSize(1);
        assertThat(eventDispatcher.getAsyncDispatches().get(0).getEvent()).isEqualTo(testEvent);
        assertThat(eventDispatcher.getAsyncDispatches().get(0).getListener()).isEqualTo(asyncListener);
    }

    @Test
    void testProcessEvent_Transactional() throws NoSuchMethodException {
        // 트랜잭션 메서드 리스너 생성
        TestEventHandler handler = new TestEventHandler();
        Method method = TestEventHandler.class.getMethod("handleEvent", TestEvent.class);
        MethodEventListener<TestEvent> txListener = new MethodEventListener<>(
                handler,
                method,
                TestEvent.class,
                false,
                true,
                TransactionPhase.AFTER_COMMIT,
                false
        );
        
        // 트랜잭션 이벤트 처리 테스트
        multicaster.processEvent(testEvent, txListener);
        
        // 이벤트가 트랜잭션 내에서 디스패치되었는지 검증
        assertThat(eventDispatcher.getDispatchedEvents()).hasSize(0);
        assertThat(eventDispatcher.getDispatchedListeners()).hasSize(0);
        assertThat(eventDispatcher.getAsyncDispatches()).hasSize(0);
        assertThat(eventDispatcher.getTransactionDispatches()).hasSize(1);
        assertThat(eventDispatcher.getTransactionDispatches().get(0).getEvent()).isEqualTo(testEvent);
        assertThat(eventDispatcher.getTransactionDispatches().get(0).getListener()).isEqualTo(txListener);
        assertThat(eventDispatcher.getTransactionDispatches().get(0).getPhase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    @Test
    void testProcessEvent_AsyncTransactional() throws NoSuchMethodException {
        // 비동기 트랜잭션 메서드 리스너 생성
        TestEventHandler handler = new TestEventHandler();
        Method method = TestEventHandler.class.getMethod("handleEvent", TestEvent.class);
        MethodEventListener<TestEvent> asyncTxListener = new MethodEventListener<>(
                handler,
                method,
                TestEvent.class,
                true,
                true,
                TransactionPhase.AFTER_COMMIT,
                false
        );
        
        // 비동기 트랜잭션 이벤트 처리 테스트
        multicaster.processEvent(testEvent, asyncTxListener);
        
        // 이벤트가 비동기 트랜잭션 내에서 디스패치되었는지 검증
        assertThat(eventDispatcher.getDispatchedEvents()).hasSize(0);
        assertThat(eventDispatcher.getDispatchedListeners()).hasSize(0);
        assertThat(eventDispatcher.getAsyncDispatches()).hasSize(0);
        assertThat(eventDispatcher.getTransactionDispatches()).hasSize(0);
        assertThat(eventDispatcher.getAsyncTransactionDispatches()).hasSize(1);
        assertThat(eventDispatcher.getAsyncTransactionDispatches().get(0).getEvent()).isEqualTo(testEvent);
        assertThat(eventDispatcher.getAsyncTransactionDispatches().get(0).getListener()).isEqualTo(asyncTxListener);
        assertThat(eventDispatcher.getAsyncTransactionDispatches().get(0).getPhase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    @Test
    void testProcessEvent_Idempotent() throws NoSuchMethodException {
        // 멱등성 메서드 리스너 생성
        TestEventHandler handler = new TestEventHandler();
        Method method = TestEventHandler.class.getMethod("handleEvent", TestEvent.class);
        MethodEventListener<TestEvent> idempotentListener = new MethodEventListener<>(
                handler,
                method,
                TestEvent.class,
                false,
                false,
                TransactionPhase.AFTER_COMMIT,
                true
        );
        
        // 멱등성 이벤트 처리 테스트
        multicaster.processEvent(testEvent, idempotentListener);
        
        // 이벤트가 처리되었는지 검증
        assertThat(eventDispatcher.getDispatchedEvents()).hasSize(1);
        assertThat(eventDispatcher.getDispatchedEvents().get(0)).isEqualTo(testEvent);
        
        // 멱등성 처리가 되었는지 검증
        assertThat(eventRepository.getProcessedEventIds()).hasSize(1);
        assertThat(eventRepository.getProcessedEventIds().get(0)).isEqualTo(testEvent.getEventId());
        
        // 이미 처리된 이벤트 재처리 시도
        eventRepository.setProcessed(true);
        multicaster.processEvent(testEvent, idempotentListener);
        
        // 이벤트가 다시 처리되지 않았는지 검증
        assertThat(eventDispatcher.getDispatchedEvents()).hasSize(1);
    }

    @Test
    void testSetEventDispatcher() {
        // 새 이벤트 디스패처 생성
        TestEventDispatcher newDispatcher = new TestEventDispatcher();
        
        // 이벤트 디스패처 변경
        multicaster.setEventDispatcher(newDispatcher);
        
        // 리스너 등록
        multicaster.addListener(testListener);
        
        // 이벤트 멀티캐스트
        multicaster.multicast(testEvent);
        
        // 새 디스패처로 이벤트가 디스패치되었는지 검증
        assertThat(newDispatcher.getDispatchedEvents()).hasSize(1);
        assertThat(newDispatcher.getDispatchedEvents().get(0)).isEqualTo(testEvent);
        
        // 기존 디스패처로는 이벤트가 디스패치되지 않았는지 검증
        assertThat(eventDispatcher.getDispatchedEvents()).isEmpty();
    }

    // 테스트용 이벤트 디스패처 클래스
    static class TestEventDispatcher implements EventDispatcher {
        private final List<Event> dispatchedEvents = new ArrayList<>();
        private final List<EventListener<?>> dispatchedListeners = new ArrayList<>();
        private final List<EventListenerPair<?>> asyncDispatches = new ArrayList<>();
        private final List<TransactionEventPair<?>> transactionDispatches = new ArrayList<>();
        private final List<TransactionEventPair<?>> asyncTransactionDispatches = new ArrayList<>();
        
        @Override
        public <T extends Event> void dispatch(Event event, EventListener<T> listener) {
            dispatchedEvents.add(event);
            dispatchedListeners.add(listener);
        }
        
        @Override
        public <T extends Event> void dispatchAsync(Event event, EventListener<T> listener) {
            asyncDispatches.add(new EventListenerPair<>(event, listener));
        }
        
        @Override
        public <T extends Event> void dispatchInTransaction(Event event, EventListener<T> listener, TransactionPhase phase) {
            transactionDispatches.add(new TransactionEventPair<>(event, listener, phase));
        }
        
        @Override
        public <T extends Event> void dispatchAsyncInTransaction(Event event, EventListener<T> listener, TransactionPhase phase) {
            asyncTransactionDispatches.add(new TransactionEventPair<>(event, listener, phase));
        }
        
        public List<Event> getDispatchedEvents() {
            return dispatchedEvents;
        }
        
        public List<EventListener<?>> getDispatchedListeners() {
            return dispatchedListeners;
        }
        
        public List<EventListenerPair<?>> getAsyncDispatches() {
            return asyncDispatches;
        }
        
        public List<TransactionEventPair<?>> getTransactionDispatches() {
            return transactionDispatches;
        }
        
        public List<TransactionEventPair<?>> getAsyncTransactionDispatches() {
            return asyncTransactionDispatches;
        }
        
        static class EventListenerPair<T extends Event> {
            private final Event event;
            private final EventListener<T> listener;
            
            public EventListenerPair(Event event, EventListener<T> listener) {
                this.event = event;
                this.listener = listener;
            }
            
            public Event getEvent() {
                return event;
            }
            
            public EventListener<T> getListener() {
                return listener;
            }
        }
        
        static class TransactionEventPair<T extends Event> extends EventListenerPair<T> {
            private final TransactionPhase phase;
            
            public TransactionEventPair(Event event, EventListener<T> listener, TransactionPhase phase) {
                super(event, listener);
                this.phase = phase;
            }
            
            public TransactionPhase getPhase() {
                return phase;
            }
        }
    }
    
    // 테스트용 이벤트 저장소 클래스
    static class TestEventRepository implements EventRepository {
        private final List<String> processedEventIds = new ArrayList<>();
        private boolean processed = false;
        
        @Override
        public boolean isProcessed(String eventId) {
            return processed || processedEventIds.contains(eventId);
        }
        
        @Override
        public void markAsProcessed(String eventId, Instant processedAt) {
            processedEventIds.add(eventId);
        }
        
        @Override
        public void cleanupOldEvents(Duration olderThan) {
            // 테스트에서는 구현하지 않음
        }
        
        public List<String> getProcessedEventIds() {
            return processedEventIds;
        }
        
        public void setProcessed(boolean processed) {
            this.processed = processed;
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
    
    // 테스트용 이벤트 핸들러 클래스
    static class TestEventHandler {
        private boolean eventHandled = false;
        
        public void handleEvent(TestEvent event) {
            eventHandled = true;
        }
        
        public boolean isEventHandled() {
            return eventHandled;
        }
    }
} 