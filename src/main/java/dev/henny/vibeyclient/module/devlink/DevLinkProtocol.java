package dev.henny.vibeyclient.module.devlink;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.avatar.DevItems;
import dev.henny.vibeyclient.module.devlink.WebSocketServer.Connection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * The live link protocol of SPEC §8 on top of {@link WebSocketServer}: {@code hello} (token) → {@code welcome},
 * {@code push} → {@code pushed}/{@code error}, {@code remove}, {@code ping} → {@code pong}. Unknown types are ignored.
 * Free of Minecraft classes: the game side comes in through {@link Game} and the {@code main} executor (the client
 * thread), so it can be tested without the game.
 */
public final class DevLinkProtocol implements WebSocketServer.Handler {
    public static final String ENGINE_NOT_READY = "Cosmetics engine not ready";
    static final int HELLO_TIMEOUT_MS = 5000;
    /** The plugin gives up after 10 s; answer before that even if the client thread hangs. */
    static final long PUSH_TIMEOUT_MS = 9000;

    /** The game side. {@link #devItems()} and the DevItems calls happen on the {@code main} executor. */
    public interface Game {
        String token();

        String gameVersion();

        String modVersion();

        String playerName();

        Optional<DevItems> devItems();

        /** Notifications (toasts); any thread. */
        default void connected(String client, String version) {}

        default void disconnected() {}

        default void pushed(JsonObject bundle, DevItems.PushResult result) {}

        default void rejected(String id, JsonObject bundle, String message) {}
    }

    /** Per connection state, stored in {@link Connection#attachment}. */
    static final class Session {
        volatile boolean authed;
        volatile String client = "";
        volatile String version = "";
    }

    private final Game game;
    private final Executor main;

    public DevLinkProtocol(Game game, Executor main) {
        this.game = game;
        this.main = main;
    }

    public static boolean isAuthed(Connection c) {
        return c.attachment instanceof Session s && s.authed;
    }

    @Override
    public void onOpen(Connection c) {
        Session s = new Session();
        c.attachment = s;
        c.setIdleTimeoutMs(HELLO_TIMEOUT_MS + 1000);
        c.schedule(HELLO_TIMEOUT_MS, () -> {
            if (!s.authed) c.close(WebSocketServer.UNAUTHORIZED, "no hello");
        });
    }

    @Override
    public void onClose(Connection c, int code, String reason) {
        if (isAuthed(c)) game.disconnected();
    }

    @Override
    public void onMessage(Connection c, String text) {
        Session s = (Session) c.attachment;
        JsonObject msg = null;
        try {
            JsonElement e = JsonParser.parseString(text);
            if (e.isJsonObject()) msg = e.getAsJsonObject();
        } catch (RuntimeException ignored) {
            // handled below
        }
        String type = msg == null ? null : str(msg, "type");
        if (!s.authed) {
            if (!"hello".equals(type) || !tokenMatches(str(msg, "token"))) {
                c.close(WebSocketServer.UNAUTHORIZED, "hello".equals(type) ? "wrong token" : "hello expected");
                return;
            }
            s.authed = true;
            s.client = orEmpty(str(msg, "client"));
            s.version = orEmpty(str(msg, "version"));
            c.setIdleTimeoutMs(30_000);
            JsonObject welcome = new JsonObject();
            welcome.addProperty("type", "welcome");
            welcome.addProperty("game", game.gameVersion());
            welcome.addProperty("mod", game.modVersion());
            welcome.addProperty("player", orEmpty(game.playerName()));
            send(c, welcome);
            game.connected(s.client, s.version);
            return;
        }
        if (msg == null) {
            send(c, error(null, "invalid JSON"));
            return;
        }
        if (type == null) return;
        switch (type) {
            case "ping" -> {
                JsonObject pong = new JsonObject();
                pong.addProperty("type", "pong");
                send(c, pong);
            }
            case "push" -> push(c, msg);
            case "remove" -> remove(c, str(msg, "id"));
            default -> { /* unknown types are ignored (forward compatible) */ }
        }
    }

    private void push(Connection c, JsonObject msg) {
        JsonElement b = msg.get("bundle");
        JsonObject bundle = b != null && b.isJsonObject() ? b.getAsJsonObject() : null;
        String id = bundle == null ? null : str(bundle, "id");
        if (bundle == null) {
            send(c, error(null, "push without a bundle"));
            return;
        }
        String actionName = str(msg, "action");
        DevItems.Action action;
        try {
            action = actionName == null ? DevItems.Action.NONE : DevItems.Action.valueOf(actionName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            reject(c, id, bundle, "unknown action " + actionName);
            return;
        }
        CompletableFuture.supplyAsync(() -> {
                    Optional<DevItems> engine = game.devItems();
                    return engine.isEmpty() ? null : engine.get().push(bundle, action);
                }, main)
                .orTimeout(PUSH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .whenComplete((result, err) -> {
                    if (err != null) {
                        Throwable cause = err instanceof java.util.concurrent.CompletionException && err.getCause() != null ? err.getCause() : err;
                        reject(c, id, bundle, cause instanceof java.util.concurrent.TimeoutException
                                ? "The game did not answer in time" : "Internal error: " + cause);
                    } else if (result == null) {
                        reject(c, id, bundle, ENGINE_NOT_READY);
                    } else if (!result.ok()) {
                        reject(c, result.id() != null ? result.id() : id, bundle, result.error() != null ? result.error() : "rejected");
                    } else {
                        JsonObject ok = new JsonObject();
                        ok.addProperty("type", "pushed");
                        ok.addProperty("id", result.id() != null ? result.id() : id);
                        ok.addProperty("ok", true);
                        JsonArray warnings = new JsonArray();
                        List<String> w = result.warnings();
                        if (w != null) w.forEach(warnings::add);
                        ok.add("warnings", warnings);
                        send(c, ok);
                        game.pushed(bundle, result);
                    }
                });
    }

    private void reject(Connection c, String id, JsonObject bundle, String message) {
        send(c, error(id, message));
        game.rejected(id, bundle, message);
    }

    private void remove(Connection c, String id) {
        if (id == null) return;
        CompletableFuture.supplyAsync(() -> game.devItems().map(d -> d.remove(id) ? 1 : 0).orElse(-1), main)
                .orTimeout(PUSH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .whenComplete((r, err) -> {
                    if (err != null || r == null) send(c, log("warn", "Could not remove dev item " + id));
                    else if (r < 0) send(c, log("warn", ENGINE_NOT_READY));
                    else send(c, log("info", (r == 1 ? "Removed dev item " : "No dev item ") + id));
                });
    }

    private boolean tokenMatches(String given) {
        String token = game.token();
        if (given == null || token == null || token.isEmpty()) return false;
        return MessageDigest.isEqual(given.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }

    // ================================================================================================ messages

    static JsonObject error(String id, String message) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "error");
        if (id == null) o.add("id", com.google.gson.JsonNull.INSTANCE);
        else o.addProperty("id", id);
        o.addProperty("message", message);
        return o;
    }

    static JsonObject log(String level, String message) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "log");
        o.addProperty("level", level);
        o.addProperty("message", message);
        return o;
    }

    private static void send(Connection c, JsonObject o) {
        c.sendText(o.toString());
    }

    private static String str(JsonObject o, String key) {
        if (o == null) return null;
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}
