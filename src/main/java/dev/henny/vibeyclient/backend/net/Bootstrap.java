package dev.henny.vibeyclient.backend.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import dev.henny.vibeyclient.backend.J;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The GETs the client makes after every gateway {@code ready}, in one request: {@code POST /api/v1/client/bootstrap
 * {"get": [paths]}} answers {@code {"responses": {"<path>": {"status": 200, "body": …}}}} with exactly what each
 * GET would have returned (docs/NETWORK.md). Only used when the backend announces {@code features.clientBootstrap}
 * in {@code ready}; older backends keep getting the single requests.
 * <p>Each path is handed out once ({@link #take}) within {@code ttlMs} of the ready: a later reload of the same data
 * is a normal request again. A path the answer does not cover, or that failed transiently (429/5xx), also falls back
 * to the normal request; a 4xx answer (or {@code 503 feature_disabled}) becomes the same {@code ApiException} the
 * single request would have thrown.
 */
public final class Bootstrap {
    public static final String PATH = "/api/v1/client/bootstrap";
    /** {@code features} flag in {@code ready} / meta. */
    public static final String FEATURE = "clientBootstrap";

    private final Set<String> pending;
    private final CompletableFuture<JsonElement> answer;
    private final long until;

    public Bootstrap(Collection<String> paths, CompletableFuture<JsonElement> answer, long ttlMs) {
        this.pending = java.util.Collections.synchronizedSet(new LinkedHashSet<>(paths));
        this.answer = answer;
        this.until = System.currentTimeMillis() + ttlMs;
    }

    /** The request body for {@code paths}. */
    public static JsonObject body(List<String> paths) {
        JsonArray a = new JsonArray();
        for (String p : paths) a.add(p);
        JsonObject o = new JsonObject();
        o.add("get", a);
        return o;
    }

    /**
     * The answer for {@code path}: null = not covered (make the normal request); else a future of the body, of null
     * when the normal request should be made after all, or failed with the path's error.
     */
    public CompletableFuture<JsonElement> take(String path) {
        if (System.currentTimeMillis() > until || !pending.remove(path)) return null;
        return answer.handle((o, e) -> e != null ? CompletableFuture.<JsonElement>completedFuture(null) : section(o, path)).thenCompose(r -> r);
    }

    static CompletableFuture<JsonElement> section(JsonElement answer, String path) {
        JsonObject r = J.obj(J.obj(J.obj(answer), "responses"), path);
        if (r == null) return CompletableFuture.completedFuture(null);
        int status = J.num(r, "status", 0);
        JsonElement body = r.get("body");
        if (status >= 200 && status < 300) return CompletableFuture.completedFuture(body == null ? JsonNull.INSTANCE : body);
        String text = body == null ? "" : body.toString();
        dev.henny.vibeyclient.api.service.VibeBackend.ApiException e = Rest.parseError(status, text, null);
        // transient (429, 5xx) → the single request with its retries; a switched-off feature is final (503 too)
        boolean fin = status >= 400 && status < 500 && status != 429 || "feature_disabled".equals(e.code);
        return fin ? CompletableFuture.failedFuture(e) : CompletableFuture.completedFuture(null);
    }

    public boolean expired() {
        return System.currentTimeMillis() > until;
    }
}
