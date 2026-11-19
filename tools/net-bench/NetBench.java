import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Network timing harness for Vibey Client, without Minecraft (see docs/NETWORK.md "Measuring").
 * <p>One run = one fresh JVM (like a game start: new connections, new DNS lookups). {@code tools/net-bench/run.mjs}
 * starts it N times and prints medians. Scenarios:
 * <ul>
 *   <li>{@code public <before|after> <cold|warm> [base] [busy]}: what the client fetches without a login at start
 *   (meta, announcements, cosmetics catalog + assets, then the wardrobe's thumbnails). cold = empty disk cache,
 *   warm = cache from the previous run. {@code busy}: number of {@code Tasks} workers kept busy for 2 s at start
 *   (built-in cosmetics parsing / skin heads in the real client).</li>
 *   <li>{@code auth <before|after> <cold|warm> <base>}: the logged-in startup against a LOCAL backend with
 *   AUTH_DEV_BYPASS (cold = login via challenge/verify, warm = stored refresh token), gateway hello → identify →
 *   ready, then everything the client loads after ready.</li>
 * </ul>
 * The "before" flows reproduce the client code as it was (own HttpClient per class, executors, blocking sends);
 * the "after" flows ({@code NetBenchAfter}) call the real {@code dev.henny.vibeyclient.backend.net} classes.
 * Output: one {@code RESULT {json}} line.
 */
public class NetBench {
    static final long T0 = System.nanoTime();
    static final List<JsonObject> RECS = Collections.synchronizedList(new ArrayList<>());
    static final JsonObject MARKS = new JsonObject();

    static double ms() {
        return (System.nanoTime() - T0) / 1e6;
    }

    static synchronized void mark(String name) {
        if (!MARKS.has(name)) MARKS.addProperty(name, Math.round(ms()));
    }

    /** Records one request: label, start, time to headers, end, status, bytes, HTTP version. */
    static JsonObject rec(String label, double start, double ttfb, double end, int status, long bytes, String version) {
        JsonObject o = new JsonObject();
        o.addProperty("label", label);
        o.addProperty("start", Math.round(start));
        o.addProperty("ttfb", Math.round(ttfb - start));
        o.addProperty("total", Math.round(end - start));
        o.addProperty("status", status);
        o.addProperty("bytes", bytes);
        o.addProperty("version", version);
        RECS.add(o);
        return o;
    }

    /** A body handler that notes when the headers arrived. */
    static <T> HttpResponse.BodyHandler<T> timed(HttpResponse.BodyHandler<T> h, double[] ttfb) {
        return info -> {
            ttfb[0] = ms();
            return h.apply(info);
        };
    }

    static String label(String path) {
        String p = path.replaceAll("https?://[^/]+", "");
        p = p.replaceAll("/assets/[0-9a-f]{64}/thumbnail.png", "/assets/<hash>/thumbnail.png").replaceAll("/assets/[0-9a-f]{64}", "/assets/<hash>");
        int q = p.indexOf('?');
        return q > 0 ? p.substring(0, q) : p;
    }

    static String sha256(byte[] b) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
    }

    static void write(Path f, byte[] b) throws IOException {
        Files.createDirectories(f.getParent());
        Files.write(f, b);
    }

    public static void main(String[] a) throws Exception {
        String scenario = a[0], variant = a[1], cache = a[2];
        String base = a.length > 3 ? a[3] : "https://vibe.axolite.app";
        int busy = a.length > 4 ? Integer.parseInt(a[4]) : 0;
        Path dir = Path.of(System.getProperty("bench.cache", "build/net-bench")).resolve(variant + "-" + scenario);
        if (cache.equals("cold")) deleteTree(dir);
        Files.createDirectories(dir);
        JsonObject result = new JsonObject();
        result.addProperty("scenario", scenario);
        result.addProperty("variant", variant);
        result.addProperty("cache", cache);
        result.addProperty("busy", busy);
        if (variant.equals("before")) {
            if (scenario.equals("public")) Before.publicFlow(base, dir, busy);
            else Before.authFlow(base, dir, cache.equals("warm"));
        } else {
            Class.forName("NetBenchAfter").getMethod(scenario + "Flow", String.class, Path.class, int.class, boolean.class)
                    .invoke(null, base, dir, busy, cache.equals("warm"));
        }
        result.add("marks", MARKS);
        JsonArray reqs = new JsonArray();
        synchronized (RECS) {
            RECS.sort((x, y) -> Long.compare(x.get("start").getAsLong(), y.get("start").getAsLong()));
            RECS.forEach(reqs::add);
        }
        result.addProperty("requests", reqs.size());
        long bytes = 0;
        for (JsonElement r : reqs) bytes += r.getAsJsonObject().get("bytes").getAsLong();
        result.addProperty("bytes", bytes);
        if (variant.equals("after")) {
            // the shared client's own counters: every request it sent and the body bytes as received (gzip)
            Class<?> net = Class.forName("dev.henny.vibeyclient.backend.net.Net");
            result.addProperty("netRequests", (long) net.getMethod("requestCount").invoke(null));
            result.addProperty("bytes", (long) net.getMethod("byteCount").invoke(null));
        }
        result.addProperty("connections", connections(base));
        result.addProperty("threads", Thread.activeCount());
        result.add("reqs", reqs);
        System.out.println("RESULT " + result);
        System.exit(0);
    }

    /** Established TCP connections of this process to the backend's port (netstat; Windows and Linux). */
    static int connections(String base) {
        try {
            URI u = URI.create(base);
            int port = u.getPort() > 0 ? u.getPort() : u.getScheme().equals("https") ? 443 : 80;
            long pid = ProcessHandle.current().pid();
            boolean win = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
            Process p = new ProcessBuilder(win ? List.of("netstat", "-ano") : List.of("ss", "-tnp")).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.ISO_8859_1);
            int n = 0;
            for (String line : out.split("\\R")) {
                String l = line.trim();
                // netstat prints the state in the system language (ESTABLISHED, HERGESTELLT, …)
                if (!(l.contains("ESTAB") || l.contains("HERGESTELLT"))) continue;
                if (win ? !l.endsWith(" " + pid) : !l.contains("pid=" + pid + ",")) continue;
                String[] f = l.split("\\s+");
                String remote = win ? f[2] : f[4];
                if (remote.endsWith(":" + port)) n++;
            }
            return n;
        } catch (Exception e) {
            return -1;
        }
    }

    static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (var s = Files.walk(dir)) {
            s.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    // ================================================================================================ before

    /** The client code as of 0.3.1 (a14324f), request for request. */
    static final class Before {
        static ExecutorService daemonPool(String name, int n) {
            AtomicInteger c = new AtomicInteger();
            return Executors.newFixedThreadPool(n, r -> {
                Thread t = new Thread(r, name + "-" + c.incrementAndGet());
                t.setDaemon(true);
                return t;
            });
        }

        /** api/Tasks: 3 workers, also the executor of most HttpClients. */
        static final ExecutorService TASKS = daemonPool("Tasks", 3);
        /** ItemLoader.CATALOG_IO: 5 threads. */
        static final ExecutorService CATALOG_IO = daemonPool("Catalog", 5);
        /** ThumbnailCache.LOADER: 1 thread. */
        static final ExecutorService THUMBS = daemonPool("Thumbnails", 1);

        static HttpResponse<byte[]> send(HttpClient c, HttpRequest req) throws Exception {
            double s = ms();
            double[] t = {0};
            HttpResponse<byte[]> r = c.send(req, timed(HttpResponse.BodyHandlers.ofByteArray(), t));
            rec(label(req.uri().toString()), s, t[0], ms(), r.statusCode(), r.body().length, r.version().toString());
            return r;
        }

        static void publicFlow(String base, Path dir, int busy) throws Exception {
            for (int i = 0; i < busy; i++) TASKS.execute(() -> sleep(2000));
            // BackendStatus.refresh(): Tasks.run + blocking send, own client (connect 4 s) on the Tasks executor
            HttpClient statusHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).executor(TASKS).build();
            CompletableFuture<Void> meta = CompletableFuture.runAsync(() -> {
                try {
                    send(statusHttp, HttpRequest.newBuilder(URI.create(base + "/api/v1/meta")).timeout(Duration.ofSeconds(5))
                            .header("Accept", "application/json").GET().build());
                } catch (Exception e) {
                    rec("/api/v1/meta FAILED " + e.getClass().getSimpleName(), 0, ms(), ms(), 0, 0, "-");
                }
                mark("meta");
            }, TASKS);
            // Announcements.fetch(): sendAsync, own client (connect 6 s) on the Tasks executor
            HttpClient annHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).executor(TASKS).build();
            double as = ms();
            double[] at = {0};
            CompletableFuture<Void> ann = annHttp.sendAsync(HttpRequest.newBuilder(URI.create(base + "/api/v1/announcements?app=client&version=0.3.1&lang=en"))
                            .timeout(Duration.ofSeconds(10)).header("Accept", "application/json").GET().build(), timed(HttpResponse.BodyHandlers.ofString(), at))
                    .handle((r, e) -> {
                        if (r != null) rec("/api/v1/announcements", as, at[0], ms(), r.statusCode(), r.body().length(), r.version().toString());
                        else rec("/api/v1/announcements FAILED", as, ms(), ms(), 0, 0, "-");
                        mark("announcements");
                        return null;
                    });
            // ItemLoader: own client (connect 6 s, Tasks executor); catalog + assets on CATALOG_IO, blocking sends
            HttpClient itemHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).executor(TASKS).build();
            CompletableFuture<List<String>> catalog = CompletableFuture.supplyAsync(() -> {
                try {
                    return loadCatalog(itemHttp, base, dir);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, CATALOG_IO);
            List<String> thumbs = catalog.join();
            mark("catalogReady");
            // the wardrobe opens: ThumbnailCache, one loader thread, ItemLoader.thumbnail() blocking per thumbnail
            List<CompletableFuture<Void>> t = new ArrayList<>();
            for (String url : thumbs) {
                t.add(CompletableFuture.runAsync(() -> {
                    try {
                        Path f = dir.resolve("thumbs").resolve(sha256(url.getBytes(StandardCharsets.UTF_8)) + ".png");
                        if (Files.isRegularFile(f)) return;
                        HttpResponse<byte[]> r = send(itemHttp, HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build());
                        if (r.statusCode() == 200) write(f, r.body());
                    } catch (Exception e) {
                        rec("thumbnail FAILED " + e.getClass().getSimpleName(), 0, ms(), ms(), 0, 0, "-");
                    }
                }, THUMBS));
            }
            CompletableFuture.allOf(t.toArray(new CompletableFuture[0])).join();
            mark("thumbsReady");
            CompletableFuture.allOf(meta, ann).join();
            mark("done");
        }

        /** ItemLoader.loadCatalog + apply (download part): returns the thumbnail URLs. */
        static List<String> loadCatalog(HttpClient http, String base, Path dir) throws Exception {
            Path cached = dir.resolve("catalog.json"), etagFile = dir.resolve("catalog.etag");
            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(base + "/api/v1/cosmetics/catalog"))
                    .timeout(Duration.ofSeconds(10)).header("Accept", "application/json").GET();
            if (Files.isRegularFile(cached) && Files.isRegularFile(etagFile)) rb.header("If-None-Match", Files.readString(etagFile).trim());
            HttpResponse<byte[]> res = send(http, rb.build());
            byte[] body;
            if (res.statusCode() == 304) {
                body = Files.readAllBytes(cached);
            } else {
                body = res.body();
                write(cached, body);
                String etag = res.headers().firstValue("ETag").orElse(null);
                if (etag != null) write(etagFile, etag.getBytes(StandardCharsets.UTF_8));
            }
            mark("catalog");
            JsonObject cat = JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
            List<String> thumbs = new ArrayList<>();
            List<String[]> missing = new ArrayList<>();
            for (JsonElement el : cat.getAsJsonArray("items")) {
                JsonObject o = el.getAsJsonObject();
                JsonObject asset = o.getAsJsonObject("asset");
                String hash = asset.get("hash").getAsString();
                String url = asset.has("url") ? asset.get("url").getAsString() : "/api/v1/cosmetics/assets/" + hash;
                if (o.get("thumbnailUrl") instanceof com.google.gson.JsonPrimitive p) thumbs.add(abs(base, p.getAsString()));
                Path f = dir.resolve("assets").resolve(hash + ".vibey");
                if (Files.isRegularFile(f) && sha256(Files.readAllBytes(f)).equals(hash)) continue;
                missing.add(new String[]{hash, abs(base, url)});
            }
            Semaphore slots = new Semaphore(4);
            List<CompletableFuture<Void>> jobs = new ArrayList<>();
            for (String[] m : missing) {
                jobs.add(CompletableFuture.runAsync(() -> {
                    slots.acquireUninterruptibly();
                    try {
                        HttpResponse<byte[]> r = send(http, HttpRequest.newBuilder(URI.create(m[1])).timeout(Duration.ofSeconds(30)).GET().build());
                        if (r.statusCode() == 200 && sha256(r.body()).equals(m[0])) write(dir.resolve("assets").resolve(m[0] + ".vibey"), r.body());
                    } catch (Exception e) {
                        rec("asset FAILED " + e.getClass().getSimpleName(), 0, ms(), ms(), 0, 0, "-");
                    } finally {
                        slots.release();
                    }
                }, CATALOG_IO));
            }
            CompletableFuture.allOf(jobs.toArray(new CompletableFuture[0])).join();
            return thumbs;
        }

        static String abs(String base, String url) {
            return url.startsWith("http") ? url : base + (url.startsWith("/") ? url : "/" + url);
        }

        // -------------------------------------------------------------------------------------------- auth

        /** BackendClient: cached thread pool, also the HttpClient's executor; every call blocks one of its threads. */
        static final ExecutorService EXEC = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "Backend");
            t.setDaemon(true);
            return t;
        });
        static HttpClient http;
        static String base;

        static JsonElement call(String method, String path, JsonElement body, String token) {
            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json").header("User-Agent", "vibey-client/0.3.1");
            if (base.startsWith("http://")) rb.version(HttpClient.Version.HTTP_1_1);
            if (token != null) rb.header("Authorization", "Bearer " + token);
            if (body != null) {
                rb.header("Content-Type", "application/json");
                rb.method(method, HttpRequest.BodyPublishers.ofString(body.toString()));
            } else if (method.equals("POST") || method.equals("PUT")) {
                rb.header("Content-Type", "application/json");
                rb.method(method, HttpRequest.BodyPublishers.ofString("{}"));
            } else {
                rb.method(method, HttpRequest.BodyPublishers.noBody());
            }
            try {
                double s = ms();
                double[] t = {0};
                HttpResponse<String> r = http.send(rb.build(), timed(HttpResponse.BodyHandlers.ofString(), t));
                rec(method + " " + label(path), s, t[0], ms(), r.statusCode(), r.body().length(), r.version().toString());
                if (r.statusCode() / 100 != 2) throw new IllegalStateException(method + " " + path + " → " + r.statusCode() + " " + r.body());
                return r.body().isBlank() ? null : JsonParser.parseString(r.body());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        static CompletableFuture<JsonElement> request(String method, String path, JsonElement body, String token) {
            return CompletableFuture.supplyAsync(() -> call(method, path, body, token), EXEC);
        }

        static void authFlow(String b, Path dir, boolean warm) throws Exception {
            base = b;
            http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).executor(EXEC).build();
            String access = Auth.login(dir, warm, (m, p, body) -> CompletableFuture.supplyAsync(() -> call(m, p, body, null), EXEC).join());
            mark("loggedIn");
            JsonObject ready = Auth.gateway(http, base, access);
            mark("ready");
            // after ready: SocialService.onReady → reloadRest(); LoadoutSync.tick → loadMe() (+ fetchMeta)
            List<CompletableFuture<?>> all = new ArrayList<>();
            for (String p : Auth.SOCIAL_PATHS) all.add(request("GET", p, null, access));
            all.add(request("GET", "/api/v1/hosting/friends", null, access));
            all.add(request("GET", "/api/v1/hosting/sessions", null, access));
            all.add(request("GET", "/api/v1/meta", null, null)); // SocialService publicGet
            all.add(request("GET", "/api/v1/meta", null, access)); // LoadoutSync.fetchMeta (authenticated)
            CompletableFuture<JsonElement> me = request("GET", "/api/v1/cosmetics/me", null, access);
            all.add(me.thenCompose(x -> {
                mark("cosmeticsMe");
                // ownedKnown → fetchToken(); updateSubscriptions → lookup (ourselves)
                JsonObject lookup = new JsonObject();
                JsonArray ids = new JsonArray();
                ids.add(ready.getAsJsonObject("user").get("id").getAsString());
                lookup.add("userIds", ids);
                return CompletableFuture.allOf(request("GET", "/api/v1/cosmetics/loadout/signed", null, access),
                        request("POST", "/api/v1/cosmetics/lookup", lookup, access));
            }));
            CompletableFuture.allOf(all.toArray(new CompletableFuture[0])).join();
            mark("allLoaded");
        }
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    // ================================================================================================ shared auth bits

    interface Caller {
        JsonElement call(String method, String path, JsonElement body);
    }

    static final class Auth {
        static final String[] SOCIAL_PATHS = {"/api/v1/friends/requests", "/api/v1/blocks", "/api/v1/chats",
                "/api/v1/invites?direction=incoming&pendingOnly=true", "/api/v1/notifications?limit=50"};

        /** cold: challenge → verify (dev bypass); warm: refresh with the stored refresh token. Returns the access token. */
        static String login(Path dir, boolean warm, Caller c) throws Exception {
            Path f = dir.resolve("refresh.txt");
            JsonObject pair;
            if (warm && Files.isRegularFile(f)) {
                JsonObject body = new JsonObject();
                body.addProperty("refreshToken", Files.readString(f).trim());
                pair = c.call("POST", "/api/v1/auth/refresh", body).getAsJsonObject();
            } else {
                JsonObject ch = c.call("POST", "/api/v1/auth/challenge", new JsonObject()).getAsJsonObject();
                JsonObject body = new JsonObject();
                body.addProperty("name", System.getProperty("bench.user", "NetBench"));
                body.addProperty("serverId", ch.get("serverId").getAsString());
                JsonObject client = new JsonObject();
                client.addProperty("name", "vibey-client");
                client.addProperty("version", "0.3.1 fabric 1.21.11");
                client.addProperty("platform", "windows");
                body.add("client", client);
                pair = c.call("POST", "/api/v1/auth/verify", body).getAsJsonObject();
            }
            write(f, pair.get("refreshToken").getAsString().getBytes(StandardCharsets.UTF_8));
            return pair.get("accessToken").getAsString();
        }

        /** Gateway: connect, hello → identify → ready. Returns the ready payload. */
        static JsonObject gateway(HttpClient http, String base, String token) throws Exception {
            String url = (base.startsWith("https://") ? "wss://" + base.substring(8) : "ws://" + base.substring(7)) + "/ws";
            CompletableFuture<JsonObject> ready = new CompletableFuture<>();
            double s = ms();
            double[] opened = {0};
            WebSocket ws = http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10)).buildAsync(URI.create(url), new WebSocket.Listener() {
                final StringBuilder buf = new StringBuilder();

                @Override
                public void onOpen(WebSocket w) {
                    opened[0] = ms();
                    w.request(1);
                }

                @Override
                public CompletionStage<?> onText(WebSocket w, CharSequence data, boolean last) {
                    buf.append(data);
                    if (last) {
                        JsonObject msg = JsonParser.parseString(buf.toString()).getAsJsonObject();
                        buf.setLength(0);
                        String op = msg.get("op").getAsString();
                        if (op.equals("hello")) {
                            mark("hello");
                            JsonObject d = new JsonObject();
                            d.addProperty("token", token);
                            JsonObject id = new JsonObject();
                            id.addProperty("op", "identify");
                            id.add("d", d);
                            w.sendText(id.toString(), true);
                        } else if (op.equals("ready")) {
                            ready.complete(msg.getAsJsonObject("d"));
                        }
                    }
                    w.request(1);
                    return null;
                }
            }).get(15, TimeUnit.SECONDS);
            JsonObject d = ready.get(15, TimeUnit.SECONDS);
            rec("WS /ws → ready", s, opened[0], ms(), 101, d.toString().length(), "ws");
            GATEWAY = ws;
            return d;
        }

        static WebSocket GATEWAY;
    }
}
