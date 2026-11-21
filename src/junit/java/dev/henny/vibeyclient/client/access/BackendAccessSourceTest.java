package dev.henny.vibeyclient.client.access;

import dev.henny.vibeyclient.access.AccessFailure;
import dev.henny.vibeyclient.api.service.VibeBackend.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackendAccessSourceTest {
    private static AccessFailure.Kind kind(int status, String code) {
        return BackendAccessSource.failure(new ApiException(status, code, "x")).kind;
    }

    @Test
    void errorsMapToGateReasons() {
        assertEquals(AccessFailure.Kind.DENIED, kind(403, "ACCESS_REJECTED"));
        assertEquals(AccessFailure.Kind.NO_LOGIN, kind(403, "forbidden"));
        assertEquals(AccessFailure.Kind.UNREACHABLE, kind(0, "network"));
        assertEquals(AccessFailure.Kind.UNREACHABLE, kind(503, "unavailable"));
        assertEquals(AccessFailure.Kind.UNREACHABLE, kind(429, "rate_limited"));
        assertEquals(AccessFailure.Kind.UNREACHABLE, kind(0, "mojang_failed"));
        assertEquals(AccessFailure.Kind.NO_LOGIN, kind(0, "no_login"));
        assertEquals(AccessFailure.Kind.NO_LOGIN, kind(401, "unauthorized"));
        assertEquals(AccessFailure.Kind.NO_LOGIN, kind(403, "banned"));
    }
}
