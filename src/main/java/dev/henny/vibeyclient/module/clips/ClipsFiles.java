package dev.henny.vibeyclient.module.clips;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.clips.store.ClipStore;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The clips folder of this game ({@code <gameDir>/vibeyclient/clips/}) and the one background thread that touches it
 * (saves, cleanup, edits, deletes run there in order; never the shared worker pool, which other features keep busy).
 */
public final class ClipsFiles {
    private ClipsFiles() {}

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Vibey clips io");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });
    private static volatile ClipStore store;
    private static volatile boolean keptOverLimit;
    private static volatile long totalBytes = -1;

    public static Path dir() {
        return FabricLoader.getInstance().getGameDir().resolve("vibeyclient").resolve("clips");
    }

    public static Path encoderCache() {
        return FabricLoader.getInstance().getConfigDir().resolve("vibeyclient").resolve("clips-encoder.json");
    }

    public static ClipStore store() {
        ClipStore s = store;
        if (s == null) store = s = new ClipStore(dir());
        return s;
    }

    /** Runs {@code r} on the clips I/O thread. */
    public static void io(Runnable r) {
        IO.execute(() -> {
            try {
                r.run();
            } catch (RuntimeException e) {
                Vibey.LOG.warn("Clips: background work failed", e);
            }
        });
    }

    /** Waits for queued I/O (tests). */
    public static void drain(long ms) {
        Object lock = new Object();
        boolean[] done = {false};
        io(() -> {
            synchronized (lock) {
                done[0] = true;
                lock.notifyAll();
            }
        });
        long end = System.currentTimeMillis() + ms;
        synchronized (lock) {
            while (!done[0] && System.currentTimeMillis() < end) {
                try {
                    lock.wait(50);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    /** At module start: remove what an interrupted save left behind, then apply the folder limit. */
    static void startup(ClipsModule m) {
        io(() -> {
            int orphans = store().removeOrphans(System.currentTimeMillis());
            if (orphans > 0) Vibey.LOG.info("Clips: removed {} unfinished clip files", orphans);
            cleanup(m);
        });
    }

    /** Applies the folder limit (I/O thread). Never deletes kept clips. */
    static void cleanup(ClipsModule m) {
        long limit = m.autoDelete.on() ? m.folderLimitBytes() : Long.MAX_VALUE;
        ClipStore.Cleanup c = store().cleanup(limit);
        keptOverLimit = c.keptOverLimit() && m.autoDelete.on();
        totalBytes = c.totalBytes();
        if (c.deleted() > 0) Vibey.LOG.info("Clips: folder limit, deleted {} old clips", c.deleted());
    }

    public static boolean keptOverLimit() {
        return keptOverLimit;
    }

    /** Last known size of all clips (bytes), -1 until the first scan. */
    public static long totalBytes() {
        return totalBytes;
    }

    static void totalBytes(long b) {
        totalBytes = b;
    }

    public static void openFolder() {
        Path d = dir();
        try {
            Files.createDirectories(d);
        } catch (java.io.IOException ignored) {
            // opening shows the error
        }
        ClientCompat.openFolder(d);
    }

    /** Stops the I/O thread at exit after the queued work (a running save finishes). */
    static void shutdown() {
        IO.shutdown();
        try {
            IO.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
