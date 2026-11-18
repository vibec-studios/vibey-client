package dev.henny.vibeyclient.backend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.api.service.VibeBackend.ApiException;
import dev.henny.vibeyclient.backend.AuthStore.Tokens;

import java.net.URI;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Realtime gateway ({@code /ws}) like the reference SDK: hello → identify (token in the message, never in the URL)
 * → ready / resumed; heartbeat ping/pong; resume within 2 minutes with the last sequence number; invalid_session →
 * fresh ready; reconnect with exponential, jittered backoff. Close codes: 4001 refresh the token and reconnect,
 * 4010 session revoked (no reconnect), 4012 server restart (reconnect soon), 4029 slow down.
 * All state lives on one single-threaded executor, so no locking is needed inside.
 */
final class Gateway {
    private static final int MAX_BACKOFF_MS = 30_000;
    private static final long RESUME_WINDOW_MS = 115_000;

    private final BackendClient client;
    private final ScheduledExecutorService loop = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "VibeyClient-Gateway");
        t.setDaemon(true);
        return t;
    });

    // ---- only touched on the loop thread
    private WebSocket ws;
    private boolean opening;
    private boolean wanted;
    private boolean ready;
    private String sessionId;
    private long seq;
    private long disconnectedAt;
    private int heartbeatMs = 30_000;
    private boolean awaitingPong;
    private ScheduledFuture<?> heartbeat;
    private ScheduledFuture<?> reconnect;
    private int attempts;
    private boolean refreshBeforeConnect;
    private JsonObject presence;
    private JsonObject cosmeticsSubscription;
    private CompletableFuture<WebSocket> sendChain = CompletableFuture.completedFuture(null);
    private int connectionId;

    Gateway(BackendClient client) {
        this.client = client;
    }

    // ============================================================================================ public (any thread)

    void connect() {
        loop.execute(() -> {
            wanted = true;
            if (ws != null || opening || reconnect != null) return;
            open();
        });
    }

    void disconnect() {
        loop.execute(() -> {
            wanted = false;
            ready = false;
            cancelTimers();
            sessionId = null;
            seq = 0;
            attempts = 0;
            WebSocket w = ws;
            ws = null;
            connectionId++;
            if (w != null) {
                try {
                    w.sendClose(1000, "client");
                } catch (RuntimeException ignored) {
                }
                loop.schedule(() -> w.abort(), 2, TimeUnit.SECONDS);
            }
        });
    }

    /** Sends a client op; {@code presence} and {@code cosmetics.subscribe} are remembered and re-sent after reconnects. */
    void send(String op, JsonObject d) {
        JsonObject copy = d == null ? new JsonObject() : d.deepCopy();
        loop.execute(() -> {
            if ("presence".equals(op)) {
                if (presence == null) presence = new JsonObject();
                for (var e : copy.entrySet()) presence.add(e.getKey(), e.getValue());
            } else if ("cosmetics.subscribe".equals(op)) {
                cosmeticsSubscription = copy;
            }
            if (ready) sendNow(J.of("op", op, "d", copy));
        });
    }

    /** Tests: drops the socket like a network failure (the gateway reconnects and resumes). */
    void debugDrop() {
        loop.execute(() -> {
            WebSocket w = ws;
            if (w == null) return;
            int id = connectionId;
            w.abort();
            closed(id, 1006, "debug drop");
        });
    }

    // ============================================================================================ connection

    private void open() {
        opening = true;
        int id = ++connectionId;
        boolean refresh = refreshBeforeConnect;
        refreshBeforeConnect = false;
        if (sessionId != null && disconnectedAt > 0 && System.currentTimeMillis() - disconnectedAt > RESUME_WINDOW_MS) {
            sessionId = null; // too late to resume, start fresh
            seq = 0;
        }
        // Token (may refresh = network) on a backend worker, then back on the loop.
        CompletableFuture.supplyAsync(() -> {
            Tokens t = client.validTokens();
            if (refresh) t = client.refresh(t);
            return t;
        }, client.executor()).whenCompleteAsync((tokens, err) -> {
            if (id != connectionId || !wanted) {
                opening = false;
                return;
            }
            if (err != null) {
                opening = false;
                ApiException e = BackendClient.unwrap(err);
                if (e.status == 401 || e.status == 403) {
                    wanted = false; // login lost; BackendClient decides (re-login / logged out)
                    return;
                }
                scheduleReconnect();
                return;
            }
            String token = tokens.accessToken();
            client.http.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(URI.create(client.gatewayUrl()), new Listener(id, token))
                    .whenCompleteAsync((w, e2) -> {
                        opening = false;
                        if (id != connectionId || !wanted) {
                            if (w != null) w.abort();
                            return;
                        }
                        if (e2 != null) {
                            BackendClient.LOG.debug("Gateway connect failed: {}", e2.toString());
                            scheduleReconnect();
                            return;
                        }
                        ws = w;
                    }, loop);
        }, loop);
    }

    private final class Listener implements WebSocket.Listener {
        private final int id;
        private final String token;
        private final StringBuilder buffer = new StringBuilder();

        Listener(int id, String token) {
            this.id = id;
            this.token = token;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String text = buffer.toString();
                buffer.setLength(0);
                loop.execute(() -> handle(id, webSocket, text, token));
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            loop.execute(() -> closed(id, statusCode, reason));
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            loop.execute(() -> closed(id, 1006, String.valueOf(error.getMessage())));
        }
    }

    private void handle(int id, WebSocket w, String text, String token) {
        if (id != connectionId) return;
        if (ws == null) ws = w; // first message can beat the buildAsync completion
        JsonObject msg;
        try {
            msg = J.obj(JsonParser.parseString(text));
        } catch (RuntimeException e) {
            return;
        }
        if (msg == null) return;
        String op = J.str(msg, "op", "");
        JsonObject d = J.obj(msg, "d");
        switch (op) {
            case "hello" -> {
                heartbeatMs = Math.max(5_000, J.num(d, "heartbeatInterval", 30_000));
                JsonObject identify = J.of("token", token);
                if (sessionId != null) {
                    identify.add("resume", J.of("sessionId", sessionId, "seq", seq));
                } else if (presence != null) {
                    identify.add("presence", presence.deepCopy());
                }
                sendNow(J.of("op", "identify", "d", identify));
            }
            case "ready" -> {
                sessionId = J.str(d, "sessionId");
                seq = 0;
                attempts = 0;
                disconnectedAt = 0;
                ready = true;
                if (d != null) heartbeatMs = Math.max(5_000, J.num(d, "heartbeatInterval", heartbeatMs));
                startHeartbeat(id);
                if (cosmeticsSubscription != null) sendNow(J.of("op", "cosmetics.subscribe", "d", cosmeticsSubscription));
                client.onGatewayReady(d == null ? new JsonObject() : d);
            }
            case "resumed" -> {
                attempts = 0;
                disconnectedAt = 0;
                ready = true;
                startHeartbeat(id);
                if (presence != null) sendNow(J.of("op", "presence", "d", presence));
                // Subscriptions belong to the connection: send them again.
                if (cosmeticsSubscription != null) sendNow(J.of("op", "cosmetics.subscribe", "d", cosmeticsSubscription));
                client.onGatewayResumed(d == null ? new JsonObject() : d);
            }
            case "invalid_session" -> {
                // a fresh `ready` follows right away
                sessionId = null;
                seq = 0;
            }
            case "pong" -> awaitingPong = false;
            case "error" -> client.onGatewayError(d == null ? new JsonObject() : d);
            case "event" -> {
                long s = J.lng(msg, "s", 0);
                if (s > seq) seq = s; // ephemeral events repeat the current s: never treat equal s as duplicate
                String t = J.str(msg, "t");
                if (t == null) return;
                if ("session.revoked".equals(t)) {
                    revokedReason = J.str(d, "reason");
                }
                JsonElement raw = msg.get("d");
                client.onGatewayEvent(t, raw != null && raw.isJsonObject() ? raw.getAsJsonObject() : new JsonObject());
            }
            default -> {
            }
        }
    }

    private String revokedReason;

    private void closed(int id, int code, String reason) {
        if (id != connectionId) return;
        connectionId++; // late callbacks of this socket (abort → onError) are ignored from now on
        ws = null;
        opening = false;
        boolean wasReady = ready;
        ready = false;
        if (heartbeat != null) heartbeat.cancel(false);
        heartbeat = null;
        if (wasReady) disconnectedAt = System.currentTimeMillis();
        boolean fatal = code == 4010 || !wanted;
        BackendClient.LOG.info("Gateway closed {} {}{}", code, reason, fatal ? " (no reconnect)" : "");
        client.onGatewayDown(code, reason, !fatal);
        if (code == 4010) {
            wanted = false;
            sessionId = null;
            String why = revokedReason != null ? revokedReason : reason;
            revokedReason = null;
            client.authLost("session_revoked", why, true);
            return;
        }
        if (fatal) return;
        if (code == 4001) refreshBeforeConnect = true;
        if (code == 4012) attempts = 0; // server restart: come back quickly
        if (code == 4029) attempts = Math.max(attempts, 3);
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        if (!wanted || reconnect != null) return;
        int base = Math.min(MAX_BACKOFF_MS, 1000 * (1 << Math.min(attempts, 5)));
        long delay = Math.round(base / 2.0 + Math.random() * (base / 2.0));
        attempts++;
        client.onGatewayDown(0, "reconnecting", true);
        reconnect = loop.schedule(() -> {
            reconnect = null;
            if (wanted && ws == null && !opening) open();
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void startHeartbeat(int id) {
        if (heartbeat != null) heartbeat.cancel(false);
        awaitingPong = false;
        heartbeat = loop.scheduleAtFixedRate(() -> {
            if (id != connectionId || ws == null) return;
            if (awaitingPong) {
                // no answer to the last ping: the connection is dead
                WebSocket w = ws;
                w.abort();
                closed(id, 4000, "heartbeat");
                return;
            }
            awaitingPong = true;
            sendNow(J.of("op", "ping", "d", J.of("seq", seq)));
        }, heartbeatMs, heartbeatMs, TimeUnit.MILLISECONDS);
    }

    private void cancelTimers() {
        if (heartbeat != null) heartbeat.cancel(false);
        if (reconnect != null) reconnect.cancel(false);
        heartbeat = null;
        reconnect = null;
    }

    /** WebSocket allows one outstanding send: chain them. */
    private void sendNow(JsonObject msg) {
        WebSocket w = ws;
        if (w == null) return;
        String text = msg.toString();
        sendChain = sendChain.handle((r, e) -> null).thenCompose(x -> w.sendText(text, true));
    }
}
