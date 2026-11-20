package dev.henny.vibeyclient.module.cosmetics.catalog;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.backend.net.HttpCache;
import dev.henny.vibeyclient.backend.net.Limiter;
import dev.henny.vibeyclient.backend.net.Net;
import dev.henny.vibeyclient.backend.net.SingleFlight;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class CatalogIo {
    private CatalogIo() {}

    private static final int PARALLEL_DOWNLOADS = 4;
    static final ThreadPoolExecutor EXECUTOR = executor();
    static final Limiter DOWNLOADS = new Limiter(PARALLEL_DOWNLOADS);
    static final Limiter THUMB_DOWNLOADS = new Limiter(PARALLEL_DOWNLOADS);
    static final SingleFlight<String, byte[]> ASSET_FLIGHT = new SingleFlight<>();
    static final SingleFlight<String, byte[]> THUMB_FLIGHT = new SingleFlight<>();

    private static ThreadPoolExecutor executor() {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), r -> {
            Thread t = new Thread(r, "VibeyClient-Catalog");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        pool.allowCoreThreadTimeOut(true);
        return pool;
    }

    static Path cacheDir() {
        return Vibey.platform().gameDir().resolve("vibeyclient").resolve("cache");
    }

    static CompletableFuture<byte[]> fetch(String url, Net.Kind kind) {
        return Net.send(Net.request(url, kind).GET().build()).thenApply(r -> {
            if (r.status() != 200) throw new CompletionException(new HttpCache.StatusException(r.status()));
            return r.body();
        });
    }

    static void write(Path file, byte[] bytes) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, bytes);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
