package dev.henny.vibeyclient.module.waypoints.map;

import dev.henny.vibeyclient.Vibey;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

final class TileWorker {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Vibey waypoints map");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private final AtomicInteger queued = new AtomicInteger();

    void execute(Runnable task) {
        queued.incrementAndGet();
        executor.execute(() -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                Vibey.LOG.warn("Waypoints map task failed", e);
            } finally {
                queued.decrementAndGet();
            }
        });
    }

    int queued() {
        return queued.get();
    }
}
