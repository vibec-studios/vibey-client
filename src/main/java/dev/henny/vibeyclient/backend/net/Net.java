package dev.henny.vibeyclient.backend.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

/**
 * The one HTTP client of Vibey Client (backend REST, gateway, catalog, assets, thumbnails, skins, announcements …),
 * see docs/NETWORK.md. HTTP/2 with keep-alive for https (one connection per host, requests multiplexed), HTTP/1.1
 * for cleartext (local dev backends; Node rejects h2c upgrades that carry a body), 5 s connect timeout, gzip.
 * <p>Threads: a small bounded pool (at most {@value #THREADS} daemon threads, gone after 30 s without traffic) is the
 * client's executor. <b>Nothing may block on it</b> – no {@code join()}, no blocking {@code send}, no disk-heavy or
 * CPU-heavy work in non-async continuations of {@link #send}: hop to another executor for that. Blocking callers
 * ({@link HttpClient#send}) are fine on their own threads because completions never need the caller's pool – the
 * old design (clients on the 3-thread {@code Tasks} pool, called from that pool) stalled every request for the full
 * connect timeout whenever three of them waited at once.
 * <p>Minecraft-free on purpose: the timing harness ({@code tools/net-bench}) and JUnit use these classes directly.
 */
public final class Net {
    private Net() {}

    /** Request timeouts by kind (the connect timeout is always {@link #CONNECT_TIMEOUT}). */
    public enum Kind {
        /** Small JSON calls (REST, meta, announcements, catalog). */
        API(Duration.ofSeconds(10)),
        /** Status checks where a quick "unreachable" beats waiting (title screen badge). */
        QUICK(Duration.ofSeconds(5)),
        /** Downloads and uploads (assets, skins, thumbnails, multipart). */
        BULK(Duration.ofSeconds(30));

        public final Duration timeout;

        Kind(Duration timeout) {
            this.timeout = timeout;
        }
    }

    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final int THREADS = 3;
    /** Larger decoded bodies are refused (protects against gzip bombs). */
    static final int MAX_BODY = 32 << 20;
    /**
     * An HTTP/2 connection idle longer than this may have been closed (the JDK closes idle connections after 30 s,
     * proxies sooner): the next burst goes through the warm-up gate again.
     */
    static final long IDLE_NS = TimeUnit.SECONDS.toNanos(25);
    /** Requests queued behind a connection that is being opened go anyway after this long. */
    static final long GATE_WAIT_MS = 2_000;

    private static final AtomicInteger THREAD_NO = new AtomicInteger();
    private static final ThreadPoolExecutor EXEC = new ThreadPoolExecutor(THREADS, THREADS, 30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), r -> {
        Thread t = new Thread(r, "VibeyClient-Http-" + THREAD_NO.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    static {
        EXEC.allowCoreThreadTimeOut(true);
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .executor(EXEC)
            .build();

    private static volatile String userAgent = "vibey-client";

    /** Requests sent and body bytes received (as on the wire), for the debug log, tests and the harness. */
    private static final AtomicLong REQUESTS = new AtomicLong(), BYTES = new AtomicLong();

    public static HttpClient client() {
        return CLIENT;
    }

    /** The client's executor: for light continuations only (see class doc). */
    public static Executor executor() {
        return EXEC;
    }

    /** {@code vibey-client/<version>}, set once at start. */
    public static void userAgent(String ua) {
        if (ua != null && !ua.isBlank()) userAgent = ua;
    }

    public static String userAgent() {
        return userAgent;
    }

    public static long requestCount() {
        return REQUESTS.get();
    }

    public static long byteCount() {
        return BYTES.get();
    }

    /** A request builder with timeout, User-Agent, {@code Accept-Encoding: gzip} and the HTTP version for the URL. */
    public static HttpRequest.Builder request(String url, Kind kind) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(kind.timeout)
                .header("User-Agent", userAgent)
                .header("Accept-Encoding", "gzip");
        if (url.startsWith("http://")) b.version(HttpClient.Version.HTTP_1_1);
        return b;
    }

    /** A received response, body already decoded (gzip); {@code wireBytes}: body size as received. */
    public record Response(int status, HttpHeaders headers, byte[] body, HttpClient.Version version, int wireBytes) {
        public String text() {
            return new String(body, StandardCharsets.UTF_8);
        }

        public String header(String name) {
            return headers.firstValue(name).orElse(null);
        }

        public boolean ok() {
            return status / 100 == 2;
        }
    }

    /** Thrown (inside the future) for a body that is too big or not decodable. */
    public static final class BodyException extends IOException {
        public BodyException(String message) {
            super(message);
        }
    }

    /**
     * Sends asynchronously. For https the first request to a host (or the first after a pause) opens the connection
     * alone and requests started meanwhile wait for its response headers, so a burst shares one HTTP/2 connection
     * instead of opening one TLS connection each (8 parallel cold requests = 8 handshakes without this).
     */
    public static CompletableFuture<Response> send(HttpRequest req) {
        URI u = req.uri();
        if (!"https".equalsIgnoreCase(u.getScheme())) return raw(req, null);
        Gate g = GATES.computeIfAbsent(u.getHost().toLowerCase(Locale.ROOT) + ":" + u.getPort(), k -> new Gate());
        CompletableFuture<Void> wait = null;
        CompletableFuture<Void> mine = null;
        synchronized (g) {
            long now = System.nanoTime();
            if (g.opening != null && !g.opening.isDone()) {
                wait = g.opening;
            } else if (g.lastUse == 0 || now - g.lastUse > IDLE_NS) {
                mine = new CompletableFuture<>();
                g.opening = mine;
            }
            g.lastUse = now;
        }
        if (mine != null) {
            CompletableFuture<Void> opened = mine;
            CompletableFuture<Response> f = raw(req, () -> opened.complete(null));
            f.whenComplete((r, e) -> {
                opened.complete(null);
                g.touch();
            });
            return f;
        }
        if (wait == null) return raw(req, null).whenComplete((r, e) -> g.touch());
        return wait.completeOnTimeout(null, GATE_WAIT_MS, TimeUnit.MILLISECONDS)
                .thenCompose(x -> raw(req, null)).whenComplete((r, e) -> g.touch());
    }

    private static final Map<String, Gate> GATES = new ConcurrentHashMap<>();

    private static final class Gate {
        CompletableFuture<Void> opening;
        long lastUse;

        synchronized void touch() {
            lastUse = System.nanoTime();
        }
    }

    /** Tests: forget which hosts have a warm connection. */
    static void resetGates() {
        GATES.clear();
    }

    private static CompletableFuture<Response> raw(HttpRequest req, Runnable onHeaders) {
        REQUESTS.incrementAndGet();
        // ofByteArray completes once the whole body is in (never an InputStream here: reading it would block a
        // client thread while the rest of the body still needs one to arrive)
        HttpResponse.BodyHandler<byte[]> handler = info -> {
            if (onHeaders != null) onHeaders.run();
            return HttpResponse.BodySubscribers.ofByteArray();
        };
        return CLIENT.sendAsync(req, handler).thenApply(r -> {
            try (InputStream in = new java.io.ByteArrayInputStream(r.body())) {
                String enc = r.headers().firstValue("Content-Encoding").orElse("").trim().toLowerCase(Locale.ROOT);
                byte[] body = enc.isEmpty() || enc.equals("identity") ? r.body() : decode(in, enc);
                if (body.length > MAX_BODY) throw new BodyException("body larger than " + MAX_BODY + " bytes");
                BYTES.addAndGet(r.body().length);
                return new Response(r.statusCode(), r.headers(), body, r.version(), r.body().length);
            } catch (IOException e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        });
    }

    /** Reads and decodes a body ({@code gzip} or identity), at most {@link #MAX_BODY} bytes. */
    static byte[] decode(InputStream in, String encoding) throws IOException {
        InputStream src = switch (encoding) {
            case "", "identity" -> in;
            case "gzip", "x-gzip" -> new GZIPInputStream(in, 16 << 10);
            default -> throw new BodyException("unsupported Content-Encoding " + encoding);
        };
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16 << 10];
        int n;
        while ((n = src.read(buf)) > 0) {
            if (out.size() + n > MAX_BODY) throw new BodyException("body larger than " + MAX_BODY + " bytes");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /** Unwraps {@code CompletionException}/{@code ExecutionException} layers. */
    public static Throwable cause(Throwable t) {
        while ((t instanceof java.util.concurrent.CompletionException || t instanceof java.util.concurrent.ExecutionException) && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    /** Exponential backoff with jitter: {@code base·2^attempt} capped at {@code cap}, then a random half to full. */
    public static long backoff(int attempt, long baseMs, long capMs) {
        long full = Math.min(capMs, baseMs << Math.min(Math.max(attempt, 0), 20));
        return full / 2 + (long) (Math.random() * (full / 2 + 1));
    }

    /** Runs {@code next} after {@code delayMs} without holding a thread meanwhile. */
    public static <T> CompletableFuture<T> later(long delayMs, java.util.function.Supplier<CompletableFuture<T>> next) {
        return CompletableFuture.runAsync(() -> {}, CompletableFuture.delayedExecutor(Math.max(0, delayMs), TimeUnit.MILLISECONDS, EXEC))
                .thenCompose(x -> next.get());
    }
}
