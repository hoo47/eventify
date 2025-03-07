package io.github.event.registry;

import io.github.event.annotations.EventListener;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

public class EventRegistryTest {

    // Dummy event type
    static class TestEvent { }

    // Dummy listener with a method annotated with @EventListener
    static class DummyListener {
        @EventListener
        public void handleEvent(TestEvent event) {
            // 이벤트 처리 로직
        }
    }

    @Test
    public void testRegisterAndRetrieveHandler() {
        EventRegistry registry = new EventRegistry();
        DummyListener listener = new DummyListener();
        registry.register(listener);
        
        List<HandlerMethod> handlers = registry.getHandlersForEvent(new TestEvent());
        assertThat(handlers)
            .as("핸들러 목록은 비어 있어서는 안 됩니다.")
            .isNotEmpty();
        
        HandlerMethod handler = handlers.get(0);
        assertThat(handler.getInstance()).isEqualTo(listener);
        assertThat(handler.getMethod().getParameterTypes()[0]).isEqualTo(TestEvent.class);
    }
} 