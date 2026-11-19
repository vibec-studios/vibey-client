package dev.henny.vibeyclient.backend.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * {@code GET /api/v1/meta} (public) for everybody who needs it – title screen status/motd, the social module (limits,
 * features, motd) and signed loadouts (signing key). One request serves all of them: answers are kept for the
 * caller's {@code maxAgeMs} (the backend says {@code max-age=300}), and concurrent callers share one request.
 * Before, the same document was fetched up to three times at every start.
 */
public final class Meta {
    private Meta() {}

    private record Cached(JsonObject value, long at, String etag) {}

    private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();
    private static final SingleFlight<String, JsonObject> FLIGHT = new SingleFlight<>();
    private static final List<BiConsumer<String, JsonObject>> LISTENERS = new CopyOnWriteArrayList<>();

    /** Called (on an HTTP thread – keep it light) with every fresh meta document: (base URL, meta). */
    public static void onUpdate(BiConsumer<String, JsonObject> listener) {
        LISTENERS.add(listener);
    }

    /** The last meta of {@code base} (any age), or null. */
    public static JsonObject cached(String base) {
        Cached c = CACHE.get(norm(base));
        return c == null ? null : c.value;
    }

    /** Milliseconds since the last successful fetch of {@code base}, or -1. */
    public static long age(String base) {
        Cached c = CACHE.get(norm(base));
        return c == null ? -1 : System.currentTimeMillis() - c.at;
    }

    /** Meta not older than {@code maxAgeMs} (0 = always fetch, unless a fetch is already running). */
    public static CompletableFuture<JsonObject> get(String base, long maxAgeMs) {
        String b = norm(base);
        Cached c = CACHE.get(b);
        if (c != null && maxAgeMs > 0 && System.currentTimeMillis() - c.at < maxAgeMs) return CompletableFuture.completedFuture(c.value);
        return FLIGHT.run(b, () -> {
            java.net.http.HttpRequest.Builder rb = Net.request(b + "/api/v1/meta", Net.Kind.QUICK).header("Accept", "application/json").GET();
            Cached old = CACHE.get(b);
            if (old != null && old.etag != null) rb.header("If-None-Match", old.etag); // backends with ETag: 304
            return Net.send(rb.build()).thenApply(r -> {
                    if (r.status() == 304 && old != null) {
                        CACHE.put(b, new Cached(old.value, System.currentTimeMillis(), old.etag));
                        return old.value;
                    }
                    if (!r.ok()) throw new java.util.concurrent.CompletionException(new HttpCache.StatusException(r.status()));
                    JsonElement e = JsonParser.parseString(r.text());
                    if (!(e instanceof JsonObject o)) throw new java.util.concurrent.CompletionException(new Net.BodyException("meta is not an object"));
                    CACHE.put(b, new Cached(o, System.currentTimeMillis(), r.header("ETag")));
                    for (BiConsumer<String, JsonObject> l : LISTENERS) {
                        try {
                            l.accept(b, o);
                        } catch (RuntimeException ignored) {
                            // a listener must not break the others
                        }
                    }
                    return o;
                });
        });
    }

    /** Tests. */
    static void clear() {
        CACHE.clear();
    }

    static String norm(String url) {
        String s = url == null ? "" : url.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }
}
