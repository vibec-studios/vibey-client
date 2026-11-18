package dev.henny.vibeyclient.ui.title;

import dev.henny.vibeyclient.backend.BackendClient;
import org.junit.jupiter.api.Test;

import static dev.henny.vibeyclient.ui.title.BackendStatus.State.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The title screen status must never say "Signing in…" when nothing is signing in. */
class BackendStatusTest {
    private static BackendStatus.State s(boolean ready, boolean present, BackendClient.State client, boolean canProve, Boolean reachable) {
        return BackendStatus.compute(ready, present, client, canProve, reachable);
    }

    @Test
    void offlineProfileIsNoAccountNotSigningIn() {
        // the bug: offline profile, backend reachable, client LOGGED_OUT → was CONNECTING ("Signing in…")
        assertEquals(NO_ACCOUNT, s(false, true, BackendClient.State.LOGGED_OUT, false, true));
        assertEquals(NO_ACCOUNT, s(false, true, BackendClient.State.LOGGED_OUT, false, null));
        assertEquals(NO_ACCOUNT, s(false, true, BackendClient.State.LOGGED_OUT, false, false));
    }

    @Test
    void loggedOutWithRealSessionIsSignedOut() {
        assertEquals(SIGNED_OUT, s(false, true, BackendClient.State.LOGGED_OUT, true, true));
        assertEquals(SIGNED_OUT, s(false, true, BackendClient.State.LOGGED_OUT, true, null));
        assertEquals(OFFLINE, s(false, true, BackendClient.State.LOGGED_OUT, true, false));
    }

    @Test
    void connectingOnlyWhileReallyConnecting() {
        for (BackendClient.State c : new BackendClient.State[]{BackendClient.State.LOGGING_IN, BackendClient.State.CONNECTING, BackendClient.State.RECONNECTING}) {
            assertEquals(CONNECTING, s(false, true, c, true, true), c.name());
            assertEquals(CHECKING, s(false, true, c, true, null), c.name());
            assertEquals(OFFLINE, s(false, true, c, true, false), c.name());
        }
    }

    @Test
    void otherStates() {
        assertEquals(ONLINE, s(true, true, BackendClient.State.READY, true, true));
        assertEquals(LOGIN_ERROR, s(false, true, BackendClient.State.ERROR, true, true));
        // social module off: only the public check counts
        assertEquals(REACHABLE, s(false, true, BackendClient.State.OFF, false, true));
        assertEquals(REACHABLE, s(false, false, null, false, true));
        assertEquals(CHECKING, s(false, false, null, true, null));
        assertEquals(OFFLINE, s(false, false, null, true, false));
    }
}
