package dev.henny.vibeyclient.backend.net;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

/**
 * A revalidating disk cache for GET resources with ETags (the cosmetics catalog): {@code <key>.json} holds the
 * body, {@code <key>.etag} the validator – the same files the client used before, so existing caches keep working.
 * The cached copy is used right away at start ({@link #load}), then revalidated with {@code If-None-Match}: a 304 is a
 * few hundred bytes instead of the whole body. Weak ETags ({@code W/"…"}, Cloudflare when it compresses) are sent
 * back as received. Not thread-safe per key: one fetch per key at a time (callers serialise, e.g. with
 * {@link SingleFlight}).
 */
public final class HttpCache {
    private final Path dir;

    public HttpCache(Path dir) {
        this.dir = dir;
    }

    /** What is on disk: body and ETag (null = none, then the body is used but never revalidated by ETag). */
    public record Entry(byte[] body, String etag) {}

    /** Result of a revalidation: the body to use and whether it changed. */
    public record Result(byte[] body, String etag, boolean notModified) {}

    /** Thrown (inside the future) for an answer other than 200/304. */
    public static final class StatusException extends IOException {
        public final int status;

        public StatusException(int status) {
            super("HTTP " + status);
            this.status = status;
        }
    }

    public Path body(String key) {
        return dir.resolve(key + ".json");
    }

    private Path etagFile(String key) {
        return dir.resolve(key + ".etag");
    }

    /** The cached entry, or null (nothing cached / unreadable). Disk I/O: not on the render thread. */
    public Entry load(String key) {
        try {
            Path b = body(key);
            if (!Files.isRegularFile(b)) return null;
            Path e = etagFile(key);
            String etag = Files.isRegularFile(e) ? Files.readString(e, StandardCharsets.UTF_8).trim() : null;
            return new Entry(Files.readAllBytes(b), etag == null || etag.isEmpty() ? null : etag);
        } catch (IOException ex) {
            return null;
        }
    }

    public void store(String key, byte[] body, String etag) throws IOException {
        Files.createDirectories(dir);
        write(body(key), body);
        if (etag != null && !etag.isBlank()) write(etagFile(key), etag.trim().getBytes(StandardCharsets.UTF_8));
        else Files.deleteIfExists(etagFile(key));
    }

    /** Adds {@code If-None-Match} when the entry has an ETag. */
    public static HttpRequest.Builder conditional(HttpRequest.Builder b, Entry e) {
        if (e != null && e.etag() != null) b.header("If-None-Match", e.etag());
        return b;
    }

    /**
     * What a response means for the cache (pure, unit-tested): 304 with an entry → the cached body; 200 → the new
     * body (to be stored); anything else → error. A 304 without a cached body is an error too (cannot happen unless
     * the files vanished between load and answer).
     */
    public static Result resolve(Entry cached, int status, String etag, byte[] body) throws StatusException {
        if (status == 304) {
            if (cached == null) throw new StatusException(304);
            return new Result(cached.body(), etag != null ? etag : cached.etag(), true);
        }
        if (status == 200) return new Result(body, etag, false);
        throw new StatusException(status);
    }

    /** Conditional GET of {@code url}; stores a 200 (disk I/O on the calling continuation's thread). */
    public CompletableFuture<Result> fetch(String key, String url, Entry cached, Net.Kind kind, java.util.concurrent.Executor io) {
        HttpRequest req = conditional(Net.request(url, kind).header("Accept", "application/json").GET(), cached).build();
        return Net.send(req).thenApplyAsync(r -> {
            try {
                Result res = resolve(cached, r.status(), r.header("ETag"), r.body());
                if (!res.notModified()) store(key, res.body(), res.etag());
                return res;
            } catch (IOException e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        }, io);
    }

    static void write(Path file, byte[] bytes) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, bytes);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
