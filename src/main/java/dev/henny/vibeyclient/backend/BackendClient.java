package dev.henny.vibeyclient.backend;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.service.VibeBackend;
import dev.henny.vibeyclient.backend.AuthStore.Tokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * The Vibe backend connection: Mojang-verified login, rotating tokens (refreshes strictly serialised – a reused
 * refresh token ends the whole session), REST with retries/backoff/rate limits, and the realtime {@link Gateway}.
 * Mirrors the reference SDK ({@code vibe-backend/packages/sdk}). Everything blocking runs on its own daemon threads;
 * listeners are called on the client thread.
 */
public final class BackendClient implements VibeBackend {
    static final Logger LOG = LoggerFactory.getLogger("VibeyClient/backend");

    public enum State {
        /** Online features or the social module are off: no connection at all. */
        OFF,
        /** Not logged in (never, logged out, or the login was lost). */
        LOGGED_OUT,
        LOGGING_IN,
        CONNECTING,
        READY,
        RECONNECTING,
        /** Login failed / banned / registration closed – see {@link #errorCode()}, {@link #errorMessage()}. */
        ERROR
    }

    /** Pseudo event types for {@link #onEvent} in addition to the gateway's {@code t} names. */
    public static final String EV_READY = "$ready";
    public static final String EV_RESUMED = "$resumed";
    public static final String EV_STATE = "$state";
    public static final String EV_GATEWAY_ERROR = "$error";

    private static final int TIMEOUT_MS = 15_000;

    private final ExecutorService exec = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "VibeyClient-Backend-" + THREADS.incrementAndGet());
        t.setDaemon(true);
        return t;
    });
    private static final AtomicInteger THREADS = new AtomicInteger();

    final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .executor(exec)
            .build();

    private final AuthStore store;
    private final Gateway gateway;
    private final ReentrantLock refreshLock = new ReentrantLock();
    private final Map<String, List<Consumer<JsonObject>>> listeners = new ConcurrentHashMap<>();

    /** Starts at the official backend (official builds only; forks set their own default_backend, see SERVICES.md). */
    private volatile String baseUrl = BuildInfo.DEFAULT_BACKEND;
    private volatile String accountKey;
    private volatile String accountName;
    private volatile Tokens tokens;
    private volatile State state = State.OFF;
    private volatile String errorCode;
    private volatile String errorMessage;
    private volatile UUID selfId;
    private volatile JsonObject me;
    private volatile JsonObject readyPayload;
    private volatile boolean running;
    private volatile boolean autoLogin;
    /** Bumped by start/stop so late results of an old run are ignored. */
    private final AtomicInteger generation = new AtomicInteger();

    public BackendClient(AuthStore store) {
        this.store = store;
        this.gateway = new Gateway(this);
    }

    // ============================================================================================ config

    /** {@code -Dvibeyclient.api=...} wins over the global setting (development). */
    public static String configuredBaseUrl() {
        String prop = System.getProperty("vibeyclient.api");
        String s = prop != null && !prop.isBlank() ? prop.trim() : GlobalSettings.backend();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    /**
     * Dev login without Mojang (the backend must run with AUTH_DEV_BYPASS): only in a dev workspace and with
     * {@code -Dvibeyclient.devAuth=true}. Release builds are never a development environment.
     */
    public static boolean devAuth() {
        return Vibey.platform().isDevelopment() && Boolean.getBoolean("vibeyclient.devAuth");
    }

    static String platformName() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "macos";
        if (os.contains("linux")) return "linux";
        return "other";
    }

    static JsonObject clientInfo() {
        String v = BuildInfo.VERSION + " " + Vibey.platform().loader().toLowerCase(Locale.ROOT) + " " + BuildInfo.MINECRAFT;
        if (v.length() > 40) v = v.substring(0, 40);
        return J.of("name", "vibey-client", "version", v, "platform", platformName());
    }

    // ============================================================================================ lifecycle

    /** Starts (or restarts) for the current game account. {@code autoLogin}: log in by itself when needed. */
    public synchronized void start(boolean autoLogin) {
        stopInternal(State.OFF);
        int gen = generation.incrementAndGet();
        this.running = true;
        this.autoLogin = autoLogin;
        this.baseUrl = configuredBaseUrl();
        UUID profile = MojangJoin.profileId();
        this.accountName = MojangJoin.name();
        String key = AuthStore.key(baseUrl, profile == null ? accountName : profile.toString());
        // Same account: keep the in-memory pair (a refresh may just have rotated it); read the file only for a new
        // account. No refreshLock here: this runs on the client thread and refresh() may hold it during a request.
        if (!key.equals(accountKey)) {
            authEpoch.incrementAndGet(); // late results for the old account are dropped (key/epoch check)
            accountKey = key;
            tokens = store.get(key);
        } else if (tokens == null) {
            Tokens stored = store.get(key);
            if (tokens == null) tokens = stored;
        }
        LOG.info("Backend {} for {} ({})", baseUrl, accountName, tokens != null ? "stored login" : "no login yet");
        if (tokens != null && tokens.valid()) {
            setState(State.CONNECTING, null, null);
            gateway.connect(gen);
        } else if (autoLogin && !store.loggedOut(accountKey) && (devAuth() || MojangJoin.hasOnlineSession())) {
            loginAsync(gen);
        } else {
            setState(State.LOGGED_OUT, null, null);
        }
    }

    /** Disconnects (online features or module off). Keeps the stored login. */
    public synchronized void stop() {
        stopInternal(State.OFF);
    }

    private void stopInternal(State to) {
        generation.incrementAndGet();
        boolean was = running;
        running = false;
        gateway.disconnect();
        if (was || state != to) setState(to, null, null);
    }

    public boolean running() {
        return running;
    }

    public State state() {
        return state;
    }

    public String errorCode() {
        return errorCode;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public boolean hasLogin() {
        return tokens != null;
    }

    /** The {@code Me} of the last {@code ready} (or {@code me.updated}), or null. */
    public JsonObject me() {
        return me;
    }

    public void setMe(JsonObject me) {
        this.me = me;
    }

    public JsonObject readyPayload() {
        return readyPayload;
    }

    public String accountName() {
        return accountName;
    }

    synchronized void setState(State s, String code, String message) {
        boolean changed = state != s || !java.util.Objects.equals(code, errorCode);
        state = s;
        errorCode = code;
        errorMessage = message;
        if (changed) dispatch(EV_STATE, J.of("state", s.name(), "code", code, "message", message));
    }

    // ============================================================================================ login

    /** Logs in with the game account (Mojang join, or the dev bypass). Result via state / {@link #EV_STATE}. */
    public void login() {
        synchronized (this) {
            if (!running) return;
            store.setLoggedOut(accountKey, false);
            loginAsync(generation.get());
        }
    }

    private void loginAsync(int gen) {
        setState(State.LOGGING_IN, null, null);
        CompletableFuture.runAsync(() -> {
            try {
                loginBlocking();
                if (generation.get() != gen) return;
                LOG.info("Logged in as {}", accountName);
                loginRetries.set(0);
                setState(State.CONNECTING, null, null);
                gateway.connect(gen);
            } catch (ApiException e) {
                if (generation.get() != gen) return;
                LOG.warn("Login failed: {} {}", e.code, e.getMessage());
                setState(State.ERROR, e.code, e.getMessage());
                // Server unreachable / Mojang hiccup: try again later by itself (not for bans, closed registration …)
                boolean transientError = "network".equals(e.code) || "mojang_failed".equals(e.code) || e.status >= 500 && !"feature_disabled".equals(e.code)
                        || "rate_limited".equals(e.code);
                if (transientError) {
                    int attempt = loginRetries.getAndIncrement();
                    long delay = Math.min(300_000L, 10_000L << Math.min(attempt, 5));
                    if (e.retryAfter > 0) delay = Math.max(delay, e.retryAfter * 1000L);
                    LOG.info("Retrying the login in {} s", delay / 1000);
                    Tasks.later(delay, () -> {
                        synchronized (this) {
                            if (generation.get() == gen && running && tokens == null && state == State.ERROR) loginAsync(gen);
                        }
                    });
                }
            } catch (RuntimeException e) {
                if (generation.get() != gen) return;
                LOG.warn("Login failed", e);
                setState(State.ERROR, "network", e.toString());
            }
        }, exec);
    }

    private Tokens loginBlocking() {
        int epoch = authEpoch.get();
        String key = accountKey;
        JsonObject challenge = J.obj(call("POST", "/api/v1/auth/challenge", new JsonObject(), false));
        String serverId = J.str(challenge, "serverId");
        if (serverId == null) throw new ApiException(502, "bad_response", "No serverId");
        if (devAuth()) {
            if (baseUrl.contains("vibe.henny.dev")) {
                throw new ApiException(0, "dev_auth_production", "Dev login is only for a local backend");
            }
            LOG.warn("DEV login without Mojang (vibeyclient.devAuth) against {}", baseUrl);
        } else {
            String err = MojangJoin.joinServer(serverId);
            if (err != null) throw new ApiException(0, "mojang_join_failed", err);
        }
        JsonObject body = J.of("name", accountName, "serverId", serverId, "client", clientInfo());
        JsonObject res = J.obj(call("POST", "/api/v1/auth/verify", body, false));
        Tokens t = Tokens.fromPair(res);
        if (!t.valid()) throw new ApiException(502, "bad_response", "No tokens");
        refreshLock.lock();
        try {
            // logged out / account switched while we were waiting: drop this login
            if (authEpoch.get() != epoch || !key.equals(accountKey)) throw new ApiException(0, "cancelled", "Login cancelled");
            tokens = t;
            store.put(key, t, accountName);
        } finally {
            refreshLock.unlock();
        }
        me = J.obj(res, "user");
        return t;
    }

    /** Ends this device session on the backend and forgets the tokens; no automatic login afterwards. */
    public CompletableFuture<Void> logout() {
        Tokens t = tokens;
        String key = accountKey;
        // Close the gateway first: the server answers the logout with close 4010, which must not read as "revoked".
        gateway.disconnect();
        CompletableFuture<Void> f = t == null ? CompletableFuture.completedFuture(null)
                : request("POST", "/api/v1/auth/logout", new JsonObject()).handle((r, e) -> null);
        return f.thenRunAsync(() -> {
            authEpoch.incrementAndGet(); // a refresh/login still in flight must not bring the tokens back
            synchronized (this) {
                gateway.disconnect();
                tokens = null;
                me = null;
                selfId = null;
                if (key != null) {
                    store.put(key, null, null);
                    store.setLoggedOut(key, true);
                }
                if (running) setState(State.LOGGED_OUT, null, null);
            }
        }, exec);
    }

    /** The login is gone for good (refresh rejected, banned, session revoked). */
    void authLost(String code, String message, boolean revoked) {
        authLost(-1, code, message, revoked);
    }

    /** {@code gen}: the run the gateway reported it for (-1 = current); stale reports are ignored. */
    void authLost(int gen, String code, String message, boolean revoked) {
        synchronized (this) {
            if (gen != -1 && gen != generation.get()) return;
            LOG.warn("Backend login lost: {} {}", code, message);
            authEpoch.incrementAndGet();
            tokens = null;
            selfId = null;
            if (accountKey != null) {
                store.put(accountKey, null, null);
                if (revoked) store.setLoggedOut(accountKey, true);
            }
            gateway.disconnect();
            if (!running) return;
            boolean relogin = autoLogin && !revoked && !"banned".equals(code) && !store.loggedOut(accountKey)
                    && (devAuth() || MojangJoin.hasOnlineSession());
            if (relogin) {
                loginAsync(generation.get());
            } else if ("banned".equals(code)) {
                setState(State.ERROR, code, message);
            } else {
                setState(State.LOGGED_OUT, code, message);
            }
        }
    }

    // ============================================================================================ tokens

    /** A usable access token (refreshes when it expires within 30 s). Blocking. */
    Tokens validTokens() {
        Tokens t = tokens;
        if (t == null) throw new ApiException(401, "unauthorized", "Not logged in");
        if (t.expiresAt() - System.currentTimeMillis() > 30_000) return t;
        return refresh(t);
    }

    /**
     * Rotates the refresh token. Strictly one at a time: if another thread already refreshed while we waited (the
     * token that failed is not the current one), the current pair is used instead of refreshing again.
     */
    Tokens refresh(Tokens failed) {
        refreshLock.lock();
        try {
            Tokens current = tokens;
            if (current == null) throw new ApiException(401, "unauthorized", "Not logged in");
            int epoch = authEpoch.get();
            String key = accountKey;
            if (failed != null && !current.accessToken().equals(failed.accessToken())
                    && current.expiresAt() - System.currentTimeMillis() > 30_000) {
                return current;
            }
            JsonObject pair;
            try {
                pair = J.obj(call("POST", "/api/v1/auth/refresh", J.of("refreshToken", current.refreshToken()), false));
            } catch (ApiException e) {
                if ((e.status == 401 || e.status == 403) && authEpoch.get() == epoch) {
                    authLost(e.code, e.getMessage(), false);
                }
                throw e;
            }
            Tokens next = Tokens.fromPair(pair);
            if (!next.valid()) throw new ApiException(502, "bad_response", "No tokens");
            // logged out / account switched meanwhile: the new pair belongs to nobody any more
            if (authEpoch.get() != epoch || !key.equals(accountKey)) throw new ApiException(401, "unauthorized", "Logged out");
            tokens = next;
            store.put(key, next, null); // persisted before anyone can use it
            refreshCount.incrementAndGet();
            return next;
        } finally {
            refreshLock.unlock();
        }
    }

    /** Tests: marks the access token as expired (the next call must refresh first). Returns the refresh count so far. */
    public int debugExpireAccessToken() {
        refreshLock.lock();
        try {
            Tokens t = tokens;
            if (t != null) tokens = new Tokens(t.accessToken(), t.refreshToken(), 0, t.sessionId());
            return refreshCount.get();
        } finally {
            refreshLock.unlock();
        }
    }

    public int refreshCount() {
        return refreshCount.get();
    }

    private final AtomicInteger refreshCount = new AtomicInteger();
    private final AtomicInteger loginRetries = new AtomicInteger();
    /** Bumped when a login is abandoned (logout, login lost, other account): late refresh/login results are dropped. */
    private final AtomicInteger authEpoch = new AtomicInteger();

    // ============================================================================================ REST

    @Override
    public String baseUrl() {
        return baseUrl;
    }

    @Override
    public boolean isReady() {
        return state == State.READY;
    }

    @Override
    public UUID selfId() {
        return selfId;
    }

    @Override
    public CompletableFuture<JsonElement> request(String method, String path, JsonElement body) {
        return CompletableFuture.supplyAsync(() -> call(method, path, body, true), exec);
    }

    /** Unauthenticated GET (e.g. {@code /api/v1/meta}). */
    public CompletableFuture<JsonElement> publicGet(String path) {
        return CompletableFuture.supplyAsync(() -> call("GET", path, null, false), exec);
    }

    public static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static final String CRLF = "\r\n";

    @Override
    public CompletableFuture<JsonElement> requestMultipart(String path, java.util.List<Part> parts) {
        String boundary = "----vibey" + java.util.UUID.randomUUID().toString().replace("-", "");
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (Part p : parts) {
            StringBuilder h = new StringBuilder("--").append(boundary).append(CRLF)
                    .append("Content-Disposition: form-data; name=\"").append(p.name().replace("\"", "")).append('"');
            if (p.filename() != null) h.append("; filename=\"").append(p.filename().replace("\"", "")).append('"');
            h.append(CRLF);
            if (p.contentType() != null) h.append("Content-Type: ").append(p.contentType()).append(CRLF);
            h.append(CRLF);
            out.writeBytes(h.toString().getBytes(StandardCharsets.UTF_8));
            out.writeBytes(p.bytes());
            out.writeBytes(CRLF.getBytes(StandardCharsets.UTF_8));
        }
        out.writeBytes(("--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
        byte[] body = out.toByteArray();
        return CompletableFuture.supplyAsync(() -> call("POST", path, null, body, "multipart/form-data; boundary=" + boundary, true), exec);
    }

    /** One REST call, blocking, with token refresh and retries. Throws {@link ApiException}. */
    JsonElement call(String method, String path, JsonElement body, boolean auth) {
        return call(method, path, body, null, null, auth);
    }

    /** Same with a raw body ({@code rawBody} + {@code rawType}, e.g. multipart) instead of JSON. */
    JsonElement call(String method, String path, JsonElement body, byte[] rawBody, String rawType, boolean auth) {
        boolean idempotent = !method.equals("POST") && !method.equals("PATCH");
        boolean refreshed = false;
        for (int attempt = 0; ; attempt++) {
            Tokens used = auth ? validTokens() : null;
            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofMillis(TIMEOUT_MS))
                    .header("Accept", "application/json")
                    .header("User-Agent", "vibey-client/" + BuildInfo.VERSION);
            // Cleartext (local dev): no h2c upgrade – Node rejects upgrade requests that carry a body.
            if (baseUrl.startsWith("http://")) rb.version(HttpClient.Version.HTTP_1_1);
            if (used != null) rb.header("Authorization", "Bearer " + used.accessToken());
            if (rawBody != null) {
                rb.header("Content-Type", rawType);
                rb.method(method, HttpRequest.BodyPublishers.ofByteArray(rawBody));
            } else if (body != null && !(body instanceof JsonNull)) {
                rb.header("Content-Type", "application/json");
                rb.method(method, HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
            } else if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) {
                rb.header("Content-Type", "application/json");
                rb.method(method, HttpRequest.BodyPublishers.ofString("{}"));
            } else {
                rb.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> res;
            try {
                res = http.send(rb.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (IOException e) {
                // Connection refused/timed out before sending is safe to retry for any method.
                boolean notSent = e instanceof ConnectException || e instanceof HttpConnectTimeoutException;
                if ((idempotent || notSent) && attempt < 2) {
                    sleep(backoff(attempt));
                    continue;
                }
                throw new ApiException(0, "network", e.getMessage() == null ? e.toString() : e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ApiException(0, "network", "interrupted");
            }
            int status = res.statusCode();
            String text = res.body();
            if (status >= 200 && status < 300) {
                if (text == null || text.isBlank()) return JsonNull.INSTANCE;
                try {
                    return JsonParser.parseString(text);
                } catch (RuntimeException e) {
                    throw new ApiException(status, "bad_response", "Invalid JSON");
                }
            }
            ApiException err = parseError(status, text, res.headers().firstValue("Retry-After").orElse(null));
            if (status == 401 && auth && !refreshed && ("token_expired".equals(err.code) || "invalid_token".equals(err.code))) {
                refreshed = true;
                refresh(used); // throws when the login is gone
                attempt--;
                continue;
            }
            if (status == 429 && idempotent && attempt == 0 && err.retryAfter >= 0 && err.retryAfter <= 5) {
                sleep(Math.max(250, err.retryAfter * 1000L));
                continue;
            }
            boolean transientError = (status == 502 || status == 504 || (status == 503 && !"feature_disabled".equals(err.code)));
            if (transientError && idempotent && attempt < 2) {
                sleep(backoff(attempt));
                continue;
            }
            if ("banned".equals(err.code) && auth) authLost(err.code, err.getMessage(), false);
            throw err;
        }
    }

    static ApiException parseError(int status, String text, String retryHeader) {
        String code = "http_error";
        String message = "HTTP " + status;
        int retryAfter = -1;
        JsonElement details = null;
        try {
            JsonObject err = J.obj(J.obj(JsonParser.parseString(text)), "error");
            if (err != null) {
                code = J.str(err, "code", code);
                message = J.str(err, "message", message);
                retryAfter = J.num(err, "retryAfter", -1);
                details = err.get("details");
            }
        } catch (RuntimeException ignored) {
            // not JSON (proxy error page)
        }
        if (retryAfter < 0 && retryHeader != null) {
            try {
                retryAfter = Integer.parseInt(retryHeader.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return new ApiException(status, code, message, retryAfter, details);
    }

    private static long backoff(int attempt) {
        long base = 500L << Math.min(attempt, 4);
        return base / 2 + (long) (Math.random() * base);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** The {@link ApiException} inside a failed future's exception, or a generic one. */
    public static ApiException unwrap(Throwable t) {
        while (t instanceof CompletionException || t instanceof java.util.concurrent.ExecutionException) {
            if (t.getCause() == null) break;
            t = t.getCause();
        }
        if (t instanceof ApiException a) return a;
        return new ApiException(0, "network", t == null ? "?" : String.valueOf(t.getMessage()));
    }

    // ============================================================================================ gateway

    @Override
    public void sendOp(String op, JsonObject d) {
        gateway.send(op, d);
    }

    @Override
    public AutoCloseable onEvent(String type, Consumer<JsonObject> handler) {
        List<Consumer<JsonObject>> list = listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());
        list.add(handler);
        return () -> list.remove(handler);
    }

    /** Delivers to the listeners of {@code type} on the client thread. */
    void dispatch(String type, JsonObject d) {
        List<Consumer<JsonObject>> list = listeners.get(type);
        if (list == null || list.isEmpty()) return;
        Tasks.onMain(() -> {
            for (Consumer<JsonObject> h : list) {
                try {
                    h.accept(d);
                } catch (Throwable t) {
                    LOG.error("Handler for {} failed", type, t);
                }
            }
        });
    }

    String gatewayUrl() {
        String b = baseUrl;
        if (b.startsWith("https://")) b = "wss://" + b.substring(8);
        else if (b.startsWith("http://")) b = "ws://" + b.substring(7);
        return b + "/ws";
    }

    /** A callback of an old run (restart/stop happened meanwhile)? */
    private boolean stale(int gen) {
        return gen != generation.get() || !running;
    }

    void onGatewayReady(int gen, JsonObject d) {
        synchronized (this) {
            if (stale(gen)) return;
            setState(State.READY, null, null);
        }
        readyPayload = d;
        JsonObject user = J.obj(d, "user");
        if (user != null) me = user;
        String id = J.str(user, "id");
        try {
            selfId = id == null ? null : UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            selfId = null;
        }
        LOG.info("Gateway ready as {} ({} friends)", J.str(user, "name"), J.arr(d, "friends") == null ? 0 : J.arr(d, "friends").size());
        dispatch(EV_READY, d);
        UUID self = selfId;
        Tasks.onMain(() -> {
            Events.post(new VibeBackend.Ready(self));
            Events.post(new VibeyEvents.BackendConnected());
        });
    }

    /** Tests: simulates a network drop of the gateway. */
    public void debugDropGateway() {
        gateway.debugDrop();
    }

    /** Number of successful resumes and the events replayed by the last one. */
    public int resumes() {
        return resumes.get();
    }

    public int lastReplayed() {
        return lastReplayed;
    }

    private final AtomicInteger resumes = new AtomicInteger();
    private volatile int lastReplayed;

    void onGatewayResumed(int gen, JsonObject d) {
        synchronized (this) {
            if (stale(gen)) return;
            setState(State.READY, null, null);
        }
        lastReplayed = J.num(d, "replayed", 0);
        resumes.incrementAndGet();
        LOG.info("Gateway resumed, {} events replayed", lastReplayed);
        dispatch(EV_RESUMED, d);
    }

    synchronized void onGatewayDown(int gen, int code, String reason, boolean willReconnect) {
        if (stale(gen)) return;
        if (state == State.READY) {
            Tasks.onMain(() -> Events.post(new VibeyEvents.BackendDisconnected(reason == null ? "" : reason)));
        }
        // authLost clears the tokens and sets LOGGED_OUT/ERROR under the same lock: never overwrite that
        boolean loginGone = tokens == null || state == State.LOGGED_OUT || state == State.ERROR || state == State.LOGGING_IN;
        if (willReconnect && !loginGone) setState(State.RECONNECTING, null, null);
    }

    void onGatewayEvent(int gen, String t, JsonObject d) {
        if (gen != generation.get()) return;
        if ("me.updated".equals(t)) me = d;
        dispatch(t, d);
    }

    void onGatewayError(JsonObject d) {
        LOG.debug("Gateway error op: {}", d);
        dispatch(EV_GATEWAY_ERROR, d);
    }

    ExecutorService executor() {
        return exec;
    }

    // ============================================================================================ helpers for UI

    /** Joins a query string from key/value pairs (null values skipped). */
    public static String query(String path, Object... kv) {
        StringBuilder sb = new StringBuilder(path);
        char sep = '?';
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i + 1] == null) continue;
            sb.append(sep).append(enc(String.valueOf(kv[i]))).append('=').append(enc(String.valueOf(kv[i + 1])));
            sep = '&';
        }
        return sb.toString();
    }
}
