package dev.henny.vibeyclient.backend.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.api.service.VibeBackend.ApiException;
import dev.henny.vibeyclient.backend.J;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Backend REST calls, fully asynchronous on the shared {@link Net} client: no thread is held while a request is in
 * flight or while waiting for a retry (the old client blocked one thread of an unbounded pool per call, ~15 threads
 * right after login). Same rules as before (and as the reference SDK): token refresh once on
 * {@code 401 token_expired/invalid_token}; idempotent calls retry twice on network errors and 502/503/504 with
 * jittered backoff, calls that never reached the server ({@code connect} failed) always; one short wait for a
 * {@code 429} with {@code Retry-After ≤ 5 s}. Errors are {@link ApiException}s inside the future.
 */
public final class Rest {
    /** Token handling, implemented by the backend client (refreshes are strictly serialised there). */
    public interface Auth {
        /** A usable access token (refreshes first when it is about to expire); fails with 401 when not logged in. */
        CompletableFuture<String> token();

        /** {@code failed} was rejected as expired: refresh unless someone already did, then the current token. */
        CompletableFuture<String> refreshed(String failed);

        /** An authenticated call answered {@code banned}. */
        void banned(ApiException e);
    }

    /** One call. {@code raw}/{@code rawType}: a non-JSON body (multipart). */
    public record Call(String method, String path, JsonElement body, byte[] raw, String rawType, boolean auth, boolean idempotent, Net.Kind kind) {
        public static Call of(String method, String path, JsonElement body, boolean auth) {
            return new Call(method, path, body, null, null, auth, !method.equals("POST") && !method.equals("PATCH"), Net.Kind.API);
        }

        public Call idempotent(boolean v) {
            return new Call(method, path, body, raw, rawType, auth, v, kind);
        }
    }

    static final int MAX_RETRIES = 2;

    private final Supplier<String> base;
    private final Auth auth;

    public Rest(Supplier<String> base, Auth auth) {
        this.base = base;
        this.auth = auth;
    }

    public CompletableFuture<JsonElement> call(Call c) {
        return attempt(c, 0, false);
    }

    private CompletableFuture<JsonElement> attempt(Call c, int attempt, boolean refreshed) {
        CompletableFuture<String> token = c.auth() ? auth.token() : CompletableFuture.completedFuture(null);
        return token.thenCompose(t -> Net.send(build(c, t))
                .handle((res, err) -> next(c, attempt, refreshed, t, res, err))
                .thenCompose(f -> f));
    }

    private HttpRequest build(Call c, String token) {
        HttpRequest.Builder rb = Net.request(base.get() + c.path(), c.kind()).header("Accept", "application/json");
        if (token != null) rb.header("Authorization", "Bearer " + token);
        if (c.raw() != null) {
            rb.header("Content-Type", c.rawType());
            rb.method(c.method(), HttpRequest.BodyPublishers.ofByteArray(c.raw()));
        } else if (c.body() != null && !(c.body() instanceof JsonNull)) {
            rb.header("Content-Type", "application/json");
            rb.method(c.method(), HttpRequest.BodyPublishers.ofString(c.body().toString(), StandardCharsets.UTF_8));
        } else if (c.method().equals("POST") || c.method().equals("PUT") || c.method().equals("PATCH")) {
            rb.header("Content-Type", "application/json");
            rb.method(c.method(), HttpRequest.BodyPublishers.ofString("{}"));
        } else {
            rb.method(c.method(), HttpRequest.BodyPublishers.noBody());
        }
        return rb.build();
    }

    private CompletableFuture<JsonElement> next(Call c, int attempt, boolean refreshed, String used, Net.Response res, Throwable err) {
        if (err != null) {
            Throwable t = Net.cause(err);
            if (t instanceof ApiException a) return CompletableFuture.failedFuture(a);
            // Connection refused/timed out before sending is safe to retry for any method.
            boolean notSent = t instanceof ConnectException || t instanceof HttpConnectTimeoutException;
            if (t instanceof IOException && (c.idempotent() || notSent) && attempt < MAX_RETRIES) {
                return Net.later(backoff(attempt), () -> attempt(c, attempt + 1, refreshed));
            }
            return CompletableFuture.failedFuture(new ApiException(0, "network", t.getMessage() == null ? t.toString() : t.getMessage()));
        }
        int status = res.status();
        String text = res.text();
        if (status >= 200 && status < 300) {
            if (text.isBlank()) return CompletableFuture.completedFuture(JsonNull.INSTANCE);
            try {
                return CompletableFuture.completedFuture(JsonParser.parseString(text));
            } catch (RuntimeException e) {
                return CompletableFuture.failedFuture(new ApiException(status, "bad_response", "Invalid JSON"));
            }
        }
        ApiException e = parseError(status, text, res.header("Retry-After"));
        if (status == 401 && c.auth() && !refreshed && ("token_expired".equals(e.code) || "invalid_token".equals(e.code))) {
            // fails when the login is gone; otherwise the same attempt again with the new token
            return auth.refreshed(used).thenCompose(x -> attempt(c, attempt, true));
        }
        if (status == 429 && c.idempotent() && attempt == 0 && e.retryAfter >= 0 && e.retryAfter <= 5) {
            return Net.later(Math.max(250, e.retryAfter * 1000L), () -> attempt(c, attempt + 1, refreshed));
        }
        boolean transientError = status == 502 || status == 504 || (status == 503 && !"feature_disabled".equals(e.code));
        if (transientError && c.idempotent() && attempt < MAX_RETRIES) {
            return Net.later(backoff(attempt), () -> attempt(c, attempt + 1, refreshed));
        }
        if ("banned".equals(e.code) && c.auth()) auth.banned(e);
        return CompletableFuture.failedFuture(e);
    }

    /** 500 ms · 2^attempt, ± half (as before). */
    static long backoff(int attempt) {
        long base = 500L << Math.min(attempt, 4);
        return base / 2 + (long) (Math.random() * base);
    }

    public static ApiException parseError(int status, String text, String retryHeader) {
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
}
