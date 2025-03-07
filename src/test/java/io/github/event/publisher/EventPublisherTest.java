package io.github.event.publisher;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.Getter;
import org.junit.jupiter.api.Test;

import io.github.event.annotations.EventListener;
import io.github.event.registry.EventRegistry;

public class EventPublisherTest {

    static class TestEvent { }

    @Getter
    static class DummyListener {
        private boolean invoked = false;
        
        @EventListener
        public void handleEvent(TestEvent event) {
            invoked = true;
        }

    }

    @Test
    public void testPublishEvent() {
        EventRegistry registry = new EventRegistry();
        DummyListener listener = new DummyListener();
        registry.register(listener);
        
        DefaultEventPublisher publisher = new DefaultEventPublisher(registry);
        publisher.publish(new TestEvent());
        
        assertThat(listener.isInvoked())
            .as("Listener should have been invoked")
            .isTrue();
    }
} 
