package io.github.event.publisher;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import io.github.event.annotations.Async;
import io.github.event.annotations.EventListener;
import io.github.event.async.AsyncExecutor;
import io.github.event.registry.EventRegistry;

public class AsyncEventPublisherTest {

    static class TestAsyncEvent { }

    static class DummyListener {
        CountDownLatch latch = new CountDownLatch(1);

        @EventListener
        @Async
        public void handleAsyncEvent(TestAsyncEvent event) {
            latch.countDown();
        }
    }

    @Test
    public void testAsyncPublish() throws InterruptedException {
        EventRegistry registry = new EventRegistry();
        DummyListener listener = new DummyListener();
        registry.register(listener);

        AsyncExecutor asyncExecutor = new AsyncExecutor();
        ApplicationEventPublisher publisher = new DefaultEventPublisher(registry, asyncExecutor);

        publisher.publish(new TestAsyncEvent());

        boolean invoked = listener.latch.await(5, TimeUnit.SECONDS);
        assertThat(invoked)
            .as("Async listener should have been invoked within 5 seconds")
            .isTrue();

        asyncExecutor.shutdown();
    }
} 