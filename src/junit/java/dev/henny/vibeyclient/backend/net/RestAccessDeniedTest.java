package dev.henny.vibeyclient.backend.net;

import dev.henny.vibeyclient.api.service.VibeBackend.ApiException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RestAccessDeniedTest {
    static final class DenialAuth implements Rest.Auth {
        final AtomicReference<String> denied = new AtomicReference<>();

        @Override
        public CompletableFuture<String> token() {
            return CompletableFuture.completedFuture("t1");
        }

        @Override
        public CompletableFuture<String> refreshed(String failed) {
            return CompletableFuture.completedFuture("t1");
        }

        @Override
        public void banned(ApiException e) {
        }

        @Override
        public void accessDenied(ApiException e) {
            denied.set(e.code);
        }
    }

    @Test
    void accessCodesOnAuthenticatedCallsCloseTheGate() throws Exception {
        try (LocalServer s = new LocalServer()) {
            s.on("/friends", (ex, n) -> LocalServer.send(ex, 403, "{\"error\":{\"code\":\"ACCESS_PENDING\",\"message\":\"wait\"}}"));
            DenialAuth auth = new DenialAuth();
            Rest rest = new Rest(s::base, auth);
            ApiException e = RestTest.err(rest.call(Rest.Call.of("GET", "/friends", null, true)));
            assertEquals(403, e.status);
            assertEquals("ACCESS_PENDING", auth.denied.get());
        }
    }

    @Test
    void otherForbiddenAndPublicCallsDoNot() throws Exception {
        try (LocalServer s = new LocalServer()) {
            s.on("/admin", (ex, n) -> LocalServer.send(ex, 403, "{\"error\":{\"code\":\"forbidden\",\"message\":\"no\"}}"));
            s.on("/public", (ex, n) -> LocalServer.send(ex, 403, "{\"error\":{\"code\":\"ACCESS_NONE\",\"message\":\"no\"}}"));
            DenialAuth auth = new DenialAuth();
            Rest rest = new Rest(s::base, auth);
            RestTest.err(rest.call(Rest.Call.of("GET", "/admin", null, true)));
            RestTest.err(rest.call(Rest.Call.of("GET", "/public", null, false)));
            assertNull(auth.denied.get());
        }
    }
}
