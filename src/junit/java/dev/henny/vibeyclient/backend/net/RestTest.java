package dev.henny.vibeyclient.backend.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.api.service.VibeBackend.ApiException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The asynchronous REST core: retries, token refresh, errors – and the bootstrap's per-path answers. */
class RestTest {
    /** Token "t1" until refreshed, then "t2". */
    static final class FakeAuth implements Rest.Auth {
        final AtomicReference<String> token = new AtomicReference<>("t1");
        final AtomicInteger refreshes = new AtomicInteger();
        volatile ApiException banned;

        @Override
        public CompletableFuture<String> token() {
            return CompletableFuture.completedFuture(token.get());
        }

        @Override
        public CompletableFuture<String> refreshed(String failed) {
            if (failed.equals(token.get())) {
                refreshes.incrementAndGet();
                token.set("t2");
            }
            return CompletableFuture.completedFuture(token.get());
        }

        @Override
        public void banned(ApiException e) {
            banned = e;
        }
    }

    static ApiException err(CompletableFuture<?> f) {
        CompletionException e = assertThrows(CompletionException.class, f::join);
        return (ApiException) Net.cause(e);
    }

    @Test
    void retriesTransientErrorsForIdempotentCallsOnly() throws Exception {
        try (LocalServer s = new LocalServer()) {
            s.on("/flaky", (ex, n) -> {
                if (n < 3) LocalServer.send(ex, 503, "{\"error\":{\"code\":\"unavailable\",\"message\":\"later\"}}");
                else LocalServer.send(ex, 200, "{\"ok\":true}");
            });
            s.on("/post", (ex, n) -> LocalServer.send(ex, 503, "{}"));
            s.on("/off", (ex, n) -> LocalServer.send(ex, 503, "{\"error\":{\"code\":\"feature_disabled\",\"message\":\"off\"}}"));
            Rest rest = new Rest(s::base, new FakeAuth());
            JsonElement ok = rest.call(Rest.Call.of("GET", "/flaky", null, false)).join();
            assertTrue(ok.getAsJsonObject().get("ok").getAsBoolean());
            assertEquals(3, s.hits("/flaky"));
            assertEquals(503, err(rest.call(Rest.Call.of("POST", "/post", new JsonObject(), false))).status);
            assertEquals(1, s.hits("/post"));
            assertEquals("feature_disabled", err(rest.call(Rest.Call.of("GET", "/off", null, false))).code);
            assertEquals(1, s.hits("/off"));
            // a POST marked idempotent (bootstrap) is retried
            rest.call(Rest.Call.of("POST", "/post", new JsonObject(), false).idempotent(true)).handle((r, e) -> null).join();
            assertEquals(4, s.hits("/post"));
        }
    }

    @Test
    void refreshesOnceOnExpiredToken() throws Exception {
        try (LocalServer s = new LocalServer()) {
            s.on("/me", (ex, n) -> {
                if ("Bearer t2".equals(ex.getRequestHeaders().getFirst("Authorization"))) LocalServer.send(ex, 200, "{\"id\":\"u\"}");
                else LocalServer.send(ex, 401, "{\"error\":{\"code\":\"token_expired\",\"message\":\"exp\"}}");
            });
            s.on("/always401", (ex, n) -> LocalServer.send(ex, 401, "{\"error\":{\"code\":\"token_expired\",\"message\":\"exp\"}}"));
            FakeAuth auth = new FakeAuth();
            Rest rest = new Rest(s::base, auth);
            assertEquals("u", rest.call(Rest.Call.of("GET", "/me", null, true)).join().getAsJsonObject().get("id").getAsString());
            assertEquals(1, auth.refreshes.get());
            assertEquals(List.of("GET /me auth=Bearer t1", "GET /me auth=Bearer t2"), s.requests);
            // only one refresh per call: a second 401 is the caller's error
            assertEquals(401, err(rest.call(Rest.Call.of("GET", "/always401", null, true))).status);
            assertEquals(2, s.hits("/always401"));
        }
    }

    @Test
    void mapsErrorsAndBans() throws Exception {
        try (LocalServer s = new LocalServer()) {
            s.on("/banned", (ex, n) -> LocalServer.send(ex, 403, "{\"error\":{\"code\":\"banned\",\"message\":\"no\"}}"));
            s.on("/html", (ex, n) -> LocalServer.send(ex, 404, "<html>proxy</html>"));
            s.on("/empty", (ex, n) -> LocalServer.send(ex, 204, null));
            s.on("/limited", (ex, n) -> {
                if (n == 1) LocalServer.send(ex, 429, "{\"error\":{\"code\":\"rate_limited\",\"message\":\"slow\",\"retryAfter\":0}}");
                else LocalServer.send(ex, 200, "[1]");
            });
            FakeAuth auth = new FakeAuth();
            Rest rest = new Rest(s::base, auth);
            assertEquals("banned", err(rest.call(Rest.Call.of("GET", "/banned", null, true))).code);
            assertEquals("banned", auth.banned.code);
            ApiException html = err(rest.call(Rest.Call.of("GET", "/html", null, false)));
            assertEquals("http_error", html.code);
            assertEquals(404, html.status);
            assertTrue(rest.call(Rest.Call.of("DELETE", "/empty", null, false)).join().isJsonNull());
            assertEquals(1, rest.call(Rest.Call.of("GET", "/limited", null, false)).join().getAsJsonArray().get(0).getAsInt());
        }
    }

    @Test
    void networkErrorsBecomeNetworkCode() {
        // nothing listens on this port: connect fails (retried, then "network")
        Rest rest = new Rest(() -> "http://127.0.0.1:9", new FakeAuth());
        assertEquals("network", err(rest.call(Rest.Call.of("GET", "/x", null, false))).code);
    }

    @Test
    void bootstrapHandsEachPathOutOnce() {
        JsonElement answer = JsonParser.parseString("""
                {"responses": {
                  "/api/v1/blocks": {"status": 200, "body": [{"id": "b"}]},
                  "/api/v1/chats": {"status": 503, "body": {"error": {"code": "unavailable"}}},
                  "/api/v1/hosting/friends": {"status": 403, "body": {"error": {"code": "feature_disabled", "message": "off"}}},
                  "/api/v1/cosmetics/loadout/signed": {"status": 200, "body": null},
                  "/api/v1/cosmetics/me": {"status": 503, "body": {"error": {"code": "feature_disabled", "message": "off"}}}
                }}""");
        Bootstrap b = new Bootstrap(List.of("/api/v1/blocks", "/api/v1/chats", "/api/v1/hosting/friends", "/api/v1/invites",
                "/api/v1/cosmetics/loadout/signed", "/api/v1/cosmetics/me"), CompletableFuture.completedFuture(answer), 60_000);
        assertEquals("b", b.take("/api/v1/blocks").join().getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString());
        assertNull(b.take("/api/v1/blocks"), "second take: normal request");
        assertNull(b.take("/api/v1/chats").join(), "transient error: normal request");
        assertEquals("feature_disabled", err(b.take("/api/v1/hosting/friends")).code);
        assertNull(b.take("/api/v1/invites").join(), "not in the answer: normal request");
        assertTrue(b.take("/api/v1/cosmetics/loadout/signed").join().isJsonNull());
        assertEquals(503, err(b.take("/api/v1/cosmetics/me")).status, "feature off: final, no second request");
        assertNull(b.take("/api/v1/notifications"), "not asked for");
        Bootstrap failed = new Bootstrap(List.of("/a"), CompletableFuture.failedFuture(new ApiException(404, "not_found", "old backend")), 60_000);
        assertNull(failed.take("/a").join(), "bootstrap failed: normal request");
        Bootstrap expired = new Bootstrap(List.of("/a"), CompletableFuture.completedFuture(answer), -1);
        assertNull(expired.take("/a"));
    }

    @Test
    void metaIsSharedAndCached() throws Exception {
        try (LocalServer s = new LocalServer()) {
            CompletableFuture<Void> hold = new CompletableFuture<>();
            s.on("/api/v1/meta", (ex, n) -> {
                hold.join();
                LocalServer.send(ex, 200, "{\"motd\":\"hi\",\"n\":" + n + "}");
            });
            Meta.clear();
            AtomicInteger seen = new AtomicInteger();
            Meta.onUpdate((base, m) -> seen.incrementAndGet());
            CompletableFuture<JsonObject> a = Meta.get(s.base(), 60_000), b = Meta.get(s.base() + "/", 60_000);
            hold.complete(null);
            assertEquals(1, a.join().get("n").getAsInt());
            assertEquals(1, b.join().get("n").getAsInt());
            assertEquals(1, Meta.get(s.base(), 60_000).join().get("n").getAsInt(), "fresh enough: no request");
            assertEquals(2, Meta.get(s.base(), 0).join().get("n").getAsInt(), "max age 0: new request");
            assertEquals(2, s.hits("/api/v1/meta"));
            assertEquals(2, seen.get());
            assertEquals("hi", Meta.cached(s.base()).get("motd").getAsString());
        }
    }
}
