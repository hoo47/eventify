package io.github.event.async;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

public class AsyncExecutorTest {

    @Test
    public void testSubmitTask() throws InterruptedException {
        AsyncExecutor executor = new AsyncExecutor();
        CountDownLatch latch = new CountDownLatch(1);

        executor.submit(latch::countDown);

        // Wait up to 5 seconds for the task to complete
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed)
            .as("Task should complete within 5 seconds")
            .isTrue();

        executor.shutdown();
    }
} 
