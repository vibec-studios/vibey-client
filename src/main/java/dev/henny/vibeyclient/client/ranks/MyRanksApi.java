package dev.henny.vibeyclient.client.ranks;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.VibeBackend;

import java.util.concurrent.CompletableFuture;

public final class MyRanksApi {
    private MyRanksApi() {}

    public static boolean available() {
        return Services.get(VibeBackend.class).map(VibeBackend::isReady).orElse(false) && Ranks.own().known();
    }

    public static CompletableFuture<Boolean> setStyle(String id) {
        return send("PUT", "/api/v1/me/name-style", body("id", id));
    }

    public static CompletableFuture<Boolean> setPrimary(String id) {
        return send("PUT", "/api/v1/me/rank", body("primary", id));
    }

    public static CompletableFuture<Boolean> setShowRank(boolean show) {
        JsonObject privacy = new JsonObject();
        privacy.addProperty("showRank", show);
        JsonObject o = new JsonObject();
        o.add("privacy", privacy);
        return send("PATCH", "/api/v1/me", o);
    }

    private static JsonObject body(String key, String value) {
        JsonObject o = new JsonObject();
        o.add(key, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
        return o;
    }

    private static CompletableFuture<Boolean> send(String method, String path, JsonObject body) {
        VibeBackend b = Services.get(VibeBackend.class).orElse(null);
        if (b == null || !b.isReady()) return CompletableFuture.completedFuture(false);
        return b.request(method, path, body).handle((JsonElement res, Throwable err) -> {
            if (err != null) {
                Vibey.LOG.info("{} {} failed: {}", method, path, err.toString());
                return false;
            }
            if (res instanceof JsonObject me) RanksSync.refreshOwn(me);
            return true;
        });
    }
}
