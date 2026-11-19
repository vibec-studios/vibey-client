package dev.henny.vibeyclient.backend.net;

import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * At most {@code max} asynchronous tasks at a time, the rest queued in order – without any thread of its own and
 * without blocking (a semaphore would park a thread per waiting task). Used for asset and thumbnail downloads.
 */
public final class Limiter {
    private final int max;
    private int running;
    private final ArrayDeque<Runnable> queue = new ArrayDeque<>();

    public Limiter(int max) {
        if (max < 1) throw new IllegalArgumentException("max < 1");
        this.max = max;
    }

    public <T> CompletableFuture<T> submit(Supplier<CompletableFuture<T>> task) {
        CompletableFuture<T> out = new CompletableFuture<>();
        Runnable start = () -> {
            CompletableFuture<T> f;
            try {
                f = task.get();
            } catch (Throwable t) {
                f = CompletableFuture.failedFuture(t);
            }
            f.whenComplete((v, e) -> {
                release();
                if (e != null) out.completeExceptionally(Net.cause(e));
                else out.complete(v);
            });
        };
        synchronized (this) {
            if (running >= max) {
                queue.add(start);
                return out;
            }
            running++;
        }
        start.run();
        return out;
    }

    private void release() {
        Runnable next;
        synchronized (this) {
            next = queue.poll();
            if (next == null) running--;
        }
        if (next != null) next.run();
    }

    /** Tasks running now (tests). */
    public synchronized int running() {
        return running;
    }

    /** Tasks waiting (tests). */
    public synchronized int queued() {
        return queue.size();
    }
}
