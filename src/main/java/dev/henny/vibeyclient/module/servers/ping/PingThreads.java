package dev.henny.vibeyclient.module.servers.ping;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

final class PingThreads {
    static final int THREADS = 2;
    private static final AtomicInteger COUNT = new AtomicInteger();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(THREADS, r -> {
        Thread t = new Thread(r, "Vibey Server Ping #" + COUNT.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    private PingThreads() {}

    static void run(Runnable task) {
        POOL.execute(task);
    }
}
