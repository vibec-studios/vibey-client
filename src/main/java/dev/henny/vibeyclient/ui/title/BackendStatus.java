package dev.henny.vibeyclient.ui.title;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.api.service.SocialStatus;
import dev.henny.vibeyclient.api.service.VibeBackend;
import dev.henny.vibeyclient.backend.BackendClient;
import dev.henny.vibeyclient.backend.MojangJoin;
import dev.henny.vibeyclient.ui.Theme;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Backend status for the menus: whether the Vibe backend is reachable (public {@code GET /api/v1/meta}, also gives the
 * message of the day), whether we are logged in ({@link VibeBackend#isReady()}) and how many friends are online
 * ({@link SocialStatus}). Never contacts anything while online features are off. Fetches at most every 5 minutes,
 * on a worker thread.
 */
public final class BackendStatus {
    private BackendStatus() {}

    /**
     * {@code SIGNED_OUT}: the backend could log in but we are not (logged out by the player or the login was lost).
     * {@code NO_ACCOUNT}: no Minecraft session that can prove the account (offline profile), so online features
     * cannot work at all. {@code LOGIN_ERROR}: login refused (banned, registration closed …). {@code CONNECTING} only
     * while a login or gateway connection is really in progress.
     */
    public enum State { OFF, CHECKING, REACHABLE, CONNECTING, ONLINE, OFFLINE, SIGNED_OUT, NO_ACCOUNT, LOGIN_ERROR }

    private static volatile Boolean reachable;
    private static volatile String motd;
    private static volatile long lastFetch;
    private static volatile boolean fetching;
    private static HttpClient http;

    /** Starts a meta fetch if online features are on and the last one is old. Call when a menu opens. */
    public static void refresh() {
        if (!GlobalSettings.online() || fetching) return;
        if (System.currentTimeMillis() - lastFetch < 5 * 60_000 && reachable != null) return;
        fetching = true;
        String base = GlobalSettings.backend();
        String url = base + "/api/v1/meta";
        Tasks.run(() -> {
            try {
                if (http == null) http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).executor(Tasks.executor()).build();
                HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5))
                        .header("Accept", "application/json").GET().build(), HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() / 100 == 2) {
                    JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
                    JsonElement m = o.get("motd");
                    motd = m != null && m.isJsonPrimitive() ? m.getAsString().trim() : null;
                    // loadout signing key (VIBE-API-2 §6), also without a login
                    if (base.equals(dev.henny.vibeyclient.backend.BackendClient.configuredBaseUrl()))
                        dev.henny.vibeyclient.backend.LoadoutKeys.fromMeta(base, o);
                    reachable = true;
                } else {
                    reachable = false;
                }
            } catch (Exception e) {
                reachable = false;
            } finally {
                lastFetch = System.currentTimeMillis();
                fetching = false;
            }
        });
    }

    public static State state() {
        if (!GlobalSettings.online()) return State.OFF;
        VibeBackend b = Services.get(VibeBackend.class).orElse(null);
        BackendClient.State client = b instanceof BackendClient c ? c.state() : null;
        boolean canProve = BackendClient.devAuth() || MojangJoin.hasOnlineSession();
        return compute(b != null && b.isReady(), b != null, client, canProve, reachable);
    }

    /**
     * The decision behind {@link #state()}, without game state (unit-testable).
     *
     * @param ready     backend logged in and gateway ready
     * @param present   the backend service exists (social module on)
     * @param client    the backend client's own state, or null if unknown
     * @param canProve  a Minecraft session that can do the Mojang join (or the dev login)
     * @param reachable result of the last public meta check (null = not checked yet)
     */
    static State compute(boolean ready, boolean present, BackendClient.State client, boolean canProve, Boolean reachable) {
        if (ready) return State.ONLINE;
        if (present && client != null) {
            switch (client) {
                case LOGGED_OUT:
                    return canProve ? (Boolean.FALSE.equals(reachable) ? State.OFFLINE : State.SIGNED_OUT) : State.NO_ACCOUNT;
                case ERROR:
                    return State.LOGIN_ERROR;
                case OFF:
                    present = false; // social module off: only the public check counts
                    break;
                default: // LOGGING_IN, CONNECTING, RECONNECTING, READY (not yet flagged ready)
                    break;
            }
        } else if (present && !canProve) {
            return State.NO_ACCOUNT;
        }
        if (reachable == null) return State.CHECKING;
        if (!reachable) return State.OFFLINE;
        return present ? State.CONNECTING : State.REACHABLE;
    }

    /** Message of the day from the backend (null/empty = none, or online features off). */
    public static String motd() {
        return GlobalSettings.online() ? motd : null;
    }

    /** Friends online, or -1 if unknown. */
    public static int onlineFriends() {
        if (state() != State.ONLINE) return -1;
        try {
            return Services.get(SocialStatus.class).map(SocialStatus::onlineFriends).orElse(-1);
        } catch (RuntimeException e) {
            return -1;
        }
    }

    public static int color(State s) {
        return switch (s) {
            case ONLINE, REACHABLE -> Theme.OK;
            case CHECKING, CONNECTING -> Theme.WARN;
            case OFFLINE, LOGIN_ERROR -> Theme.DANGER;
            case OFF, SIGNED_OUT, NO_ACCOUNT -> Theme.DIM;
        };
    }

    /** Short status text, e.g. "Online · 3 friends online". */
    public static String label() {
        State s = state();
        String base = Lang.tr("core.status." + s.name().toLowerCase(java.util.Locale.ROOT));
        int f = onlineFriends();
        if (f >= 0) base += " · " + Lang.tr(f == 1 ? "core.status.friend" : "core.status.friends", f);
        return base;
    }
}
