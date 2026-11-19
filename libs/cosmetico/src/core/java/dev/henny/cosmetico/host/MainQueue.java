package dev.henny.cosmetico.host;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MainQueue {
    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public void post(Runnable r) {
        tasks.add(r);
    }

    public int drain(Log log) {
        int n = 0;
        for (Runnable r; (r = tasks.poll()) != null; n++) {
            try {
                r.run();
            } catch (RuntimeException e) {
                log.error("cosmetico main-thread task failed", e);
            }
        }
        return n;
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }
}
