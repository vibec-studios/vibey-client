package dev.henny.vui.dev;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class UiTasks {
    public static final long TIMEOUT_MS = 5000;

    private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
    private volatile Thread uiThread;

    public void drain() {
        uiThread = Thread.currentThread();
        Runnable r;
        while ((r = queue.poll()) != null) r.run();
    }

    public <T> T call(Callable<T> task) throws Exception {
        if (Thread.currentThread() == uiThread) return task.call();
        CompletableFuture<T> future = new CompletableFuture<>();
        queue.add(() -> {
            try {
                future.complete(task.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof Exception ex ? ex : new RuntimeException(e.getCause());
        } catch (TimeoutException e) {
            throw new IllegalStateException("UI thread did not run the task within " + TIMEOUT_MS + " ms (is Vui.tick() called every frame?)");
        }
    }

    public int pending() {
        return queue.size();
    }
}
