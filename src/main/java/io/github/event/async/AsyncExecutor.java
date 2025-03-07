package io.github.event.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncExecutor {

    private final ExecutorService executor;

    public AsyncExecutor() {
        this.executor = Executors.newCachedThreadPool();
    }

    public void submit(Runnable task) {
        executor.submit(task);
    }

    public void shutdown() {
        executor.shutdown();
    }
} 