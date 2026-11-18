package dev.henny.vibeyclient.api;

import dev.henny.vibeyclient.Vibey;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Background work. Never block the render thread with I/O or network: run it here and hop back with
 * {@link #onMain(Runnable)} to touch game state. All threads are daemons; pending debounced work is flushed on exit.
 */
public final class Tasks {
    private Tasks() {}

    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(3, factory("VibeyClient-Worker"));
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(factory("VibeyClient-Timer"));
    private static final Map<Object, Pending> DEBOUNCED = new ConcurrentHashMap<>();

    private record Pending(ScheduledFuture<?> future, Runnable task) {}

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(Tasks::flush, "VibeyClient-Flush"));
    }

    /** Runs {@code task} on a worker thread. */
    public static CompletableFuture<Void> run(Runnable task) {
        return CompletableFuture.runAsync(logged(task), WORKERS);
    }

    /** Computes a value on a worker thread. */
    public static <T> CompletableFuture<T> supply(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, WORKERS);
    }

    /** Runs {@code task} on the client (render) thread, at the next opportunity. */
    public static void onMain(Runnable task) {
        Minecraft.getInstance().execute(logged(task));
    }

    /** Runs {@code task} on a worker thread after {@code delayMs}. */
    public static ScheduledFuture<?> later(long delayMs, Runnable task) {
        return TIMER.schedule(() -> WORKERS.execute(logged(task)), delayMs, TimeUnit.MILLISECONDS);
    }

    /** Coalesces calls with the same {@code key}: {@code task} runs once, {@code delayMs} after the last call. */
    public static void debounce(Object key, long delayMs, Runnable task) {
        Pending old = DEBOUNCED.get(key);
        if (old != null) old.future.cancel(false);
        ScheduledFuture<?> f = TIMER.schedule(() -> {
            DEBOUNCED.remove(key);
            WORKERS.execute(logged(task));
        }, delayMs, TimeUnit.MILLISECONDS);
        DEBOUNCED.put(key, new Pending(f, task));
    }

    /** Runs all pending debounced tasks now on the calling thread (shutdown). */
    public static void flush() {
        for (Map.Entry<Object, Pending> e : DEBOUNCED.entrySet()) {
            if (e.getValue().future.cancel(false)) logged(e.getValue().task).run();
        }
        DEBOUNCED.clear();
    }

    /** The worker pool, e.g. for {@code HttpClient.newBuilder().executor(...)}. */
    public static ExecutorService executor() {
        return WORKERS;
    }

    private static Runnable logged(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                Vibey.LOG.error("Background task failed", t);
            }
        };
    }

    private static ThreadFactory factory(String name) {
        AtomicInteger n = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, name + "-" + n.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        };
    }
}
