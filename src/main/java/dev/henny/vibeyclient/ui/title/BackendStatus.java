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

    public enum State { OFF, CHECKING, REACHABLE, CONNECTING, ONLINE, OFFLINE }

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
        if (b != null && b.isReady()) return State.ONLINE;
        Boolean r = reachable;
        if (r == null) return State.CHECKING;
        if (!r) return State.OFFLINE;
        return b != null ? State.CONNECTING : State.REACHABLE;
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
            case OFFLINE -> Theme.DANGER;
            case OFF -> Theme.DIM;
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
