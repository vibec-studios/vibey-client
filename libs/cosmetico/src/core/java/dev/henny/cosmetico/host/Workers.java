package dev.henny.cosmetico.host;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class Workers {
    private Workers() {}

    private static final ExecutorService SHARED = create("cosmetico-worker", 2);

    public static ExecutorService shared() {
        return SHARED;
    }

    public static ExecutorService create(String name, int threads) {
        AtomicInteger n = new AtomicInteger();
        ThreadPoolExecutor pool = new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), r -> {
            Thread t = new Thread(r, name + "-" + n.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        pool.allowCoreThreadTimeOut(true);
        return pool;
    }
}
