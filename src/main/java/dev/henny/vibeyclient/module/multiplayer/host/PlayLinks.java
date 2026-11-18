package dev.henny.vibeyclient.module.multiplayer.host;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.vibeyclient.Platform;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.api.service.VibeBackend;
import dev.henny.vibeyclient.api.service.VibeBackend.ApiException;
import dev.henny.vibeyclient.launcher.InstanceInfo;
import dev.henny.vibeyclient.launcher.LauncherStatus;
import dev.henny.vibeyclient.module.multiplayer.MpLoader;
import dev.henny.vibeyclient.module.multiplayer.MultiplayerModule;
import dev.henny.vibeyclient.module.multiplayer.T;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * "Play with me" links (vibe-launcher {@code docs/PLAY-LINKS.md} §1/§3): while the world is online on the relay, the
 * backend holds one link {@code https://vibe.henny.dev/j/<code>} that opens the Vibe Launcher on a friend's PC, installs
 * the same pack and joins. Created when hosting goes online (again on a new relay address, and shortly before it
 * expires), deleted when hosting ends and on quit (best effort, short timeout).
 *
 * <p>Threads: hosting events arrive on the client thread, requests run on the backend's pool, the UI reads the
 * volatile state every frame. A generation counter drops answers that arrive after hosting stopped or moved on.
 */
public final class PlayLinks {
    private PlayLinks() {}

    public enum State {
        /** Not hosting. */
        IDLE,
        /** Online features off, the backend has no play links (404) or hosting is disabled there: no UI at all. */
        HIDDEN,
        /** No Vibe login (social off, not signed in yet). */
        SIGNED_OUT,
        CREATING,
        READY,
        /** 10 new links per hour used up; {@link #retryAt()} says when. */
        RATE_LIMITED,
        /** Offline or a backend error; {@link #error()} has the text. Retried automatically a few times. */
        ERROR
    }

    /** The link the backend gave us. {@code expiresAt} in epoch millis (0 = unknown). */
    public record Link(String code, String url, long expiresAt, String relayHost) {}

    private static final String PATH = "/api/v1/play-links";
    /** Re-create this long before the backend lets the link expire. */
    private static final long RENEW_BEFORE_MS = 2 * 60_000L;
    private static final long QUIT_TIMEOUT_MS = 2_000L;

    private static volatile State state = State.IDLE;
    private static volatile Link link;
    private static volatile String error = "";
    private static volatile long retryAt;
    private static volatile int manualMods = -1;
    /** The backend answered 404 for the route: hidden until the next start of hosting. */
    private static volatile boolean unsupported;
    private static volatile int generation;
    /** Relay address we want a link for (null = not hosting). Client thread writes, others read. */
    private static volatile String wanted;
    /** The backend the current link was made with: still used for the DELETE when the service went away meanwhile. */
    private static volatile VibeBackend madeWith;
    private static int failures;
    private static ScheduledFuture<?> retryTask;
    private static boolean hookInstalled;

    // ============================================================================================ reading (any thread)

    public static State state() {
        return state;
    }

    /** The current link while {@link State#READY}, else null. */
    public static Link link() {
        return state == State.READY ? link : null;
    }

    public static String url() {
        Link l = link();
        return l == null ? null : l.url();
    }

    public static String error() {
        return error;
    }

    public static long retryAt() {
        return retryAt;
    }

    /** Jars in {@code mods/} that the launcher does not know about, -1 = unknown (see {@link InstanceInfo#manualMods}). */
    public static int manualMods() {
        return manualMods;
    }

    /** Whether the host UI shows the invite link part at all. */
    public static boolean visible() {
        return GlobalSettings.online() && !unsupported && state != State.HIDDEN;
    }

    // ============================================================================================ lifecycle

    /** Once (module load): delete the link when the game quits. */
    public static synchronized void install() {
        if (hookInstalled) return;
        hookInstalled = true;
        Runtime.getRuntime().addShutdownHook(new Thread(PlayLinks::onQuit, "VibeyClient-PlayLink-Exit"));
    }

    /** Re-reads {@code instance.json} and counts hand-added mods (worker thread), e.g. when the host screen opens. */
    public static void refreshInstance() {
        Tasks.run(() -> manualMods = LauncherStatus.instance().manualMods(LauncherStatus.instanceDir() == null
                ? null : LauncherStatus.instanceDir().resolve("mods")));
    }

    /** Hosting is online at {@code address} (full relay address, port included when not 25565). Client thread. */
    public static void onOnline(String address) {
        if (address == null || address.isBlank()) return;
        String addr = address.trim().toLowerCase(Locale.ROOT);
        if (addr.equals(wanted) && (state == State.READY || state == State.CREATING)) return;
        boolean fresh = wanted == null;
        wanted = addr;
        if (fresh) {
            unsupported = false;
            failures = 0;
        }
        create();
    }

    /** Hosting ended (stopped, failed, world closed). Any thread; idempotent. */
    public static void onStopped() {
        if (wanted == null && link == null && state == State.IDLE) return;
        wanted = null;
        generation++;
        cancelRetry();
        Link l = link;
        link = null;
        error = "";
        state = GlobalSettings.online() ? State.IDLE : State.HIDDEN;
        if (l != null) delete(l, false);
        LauncherStatus.changed();
    }

    /** Online features switched (client thread). */
    public static void onOnlineChanged(boolean on) {
        if (!on) {
            String keep = wanted;
            onStopped();
            wanted = keep;
            state = State.HIDDEN;
        } else if (wanted != null) {
            String addr = wanted;
            wanted = null;
            state = State.IDLE;
            onOnline(addr);
        } else {
            state = State.IDLE;
        }
    }

    /** The Vibe login became ready: make the link we could not make before. Client thread. */
    public static void onBackendReady() {
        if (wanted != null && (state == State.SIGNED_OUT || state == State.ERROR)) {
            failures = 0;
            create();
        }
    }

    /** "Try again" in the host screen. */
    public static void retry() {
        if (wanted == null) return;
        failures = 0;
        create();
    }

    /** Twice a second from the module tick: renew before expiry, refresh the "signed out" state. Client thread. */
    public static void tick() {
        if (wanted == null) return;
        Link l = link;
        if (state == State.READY && l != null && l.expiresAt() > 0
                && System.currentTimeMillis() > l.expiresAt() - RENEW_BEFORE_MS) {
            create();
        } else if (state == State.RATE_LIMITED && System.currentTimeMillis() >= retryAt) {
            create();
        } else if (state == State.SIGNED_OUT && Services.get(VibeBackend.class).map(VibeBackend::isReady).orElse(false)) {
            create();
        } else if (state == State.HIDDEN && !unsupported && GlobalSettings.online()) {
            create(); // online features came back without an event (e.g. set before the module was enabled)
        }
    }

    // ============================================================================================ copy

    /** Copies the link and shows "Link copied". Client thread. Returns false when there is no link. */
    public static boolean copy() {
        String url = url();
        if (url == null) return false;
        Minecraft.getInstance().keyboardHandler.setClipboard(url);
        Notify.show(new Notify.Toast(T.get("link.copied"), url, Notify.Kind.SUCCESS, null, null));
        return true;
    }

    // ============================================================================================ requests

    private static void create() {
        String addr = wanted;
        if (addr == null) return;
        cancelRetry();
        if (!GlobalSettings.online()) {
            state = State.HIDDEN;
            return;
        }
        VibeBackend backend = Services.get(VibeBackend.class).orElse(null);
        if (backend == null || !backend.isReady()) {
            state = State.SIGNED_OUT;
            return;
        }
        int gen = ++generation;
        madeWith = backend;
        state = State.CREATING;
        error = "";
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        String world = null;
        try {
            if (server != null) world = server.getWorldData().getLevelName();
        } catch (RuntimeException ignored) {
        }
        String worldName = world;
        int maxPlayers = HostConfig.get().maxPlayers;
        Tasks.supply(() -> {
            InstanceInfo info = LauncherStatus.instance();
            manualMods = info.manualMods(LauncherStatus.instanceDir() == null ? null : LauncherStatus.instanceDir().resolve("mods"));
            return body(addr, info, worldName, maxPlayers);
        }).thenCompose(body -> backend.request("POST", PATH, body)).whenComplete((res, err) ->
                Minecraft.getInstance().execute(() -> created(gen, addr, backend, res, err)));
    }

    private static void created(int gen, String addr, VibeBackend backend, JsonElement res, Throwable err) {
        if (gen != generation || !addr.equals(wanted)) {
            // hosting stopped or moved on meanwhile: do not leave this one behind
            Link stray = err == null ? parse(res, addr) : null;
            if (stray != null && (link == null || !stray.code().equals(link.code()))) delete(stray, false);
            return;
        }
        if (err == null) {
            Link l = parse(res, addr);
            if (l == null) {
                fail(T.get("link.error.generic"), false);
                return;
            }
            link = l;
            failures = 0;
            state = State.READY;
            MultiplayerModule.LOG.info("Play link {} for {}", l.url(), addr);
            LauncherStatus.changed();
            return;
        }
        ApiException e = unwrap(err);
        MultiplayerModule.LOG.info("Play link not created: {} {} {}", e.status, e.code, e.getMessage());
        link = null;
        if (e.status == 404 || e.status == 405 || e.status == 501) {
            unsupported = true; // backend without play links yet: hide quietly
            state = State.HIDDEN;
        } else if ("feature_disabled".equals(e.code)) {
            unsupported = true; // hosting/links switched off on the backend
            state = State.HIDDEN;
        } else if (e.status == 401 || "not_logged_in".equals(e.code) || "unauthorized".equals(e.code)) {
            state = State.SIGNED_OUT;
        } else if (e.status == 429 || "rate_limited".equals(e.code)) {
            retryAt = System.currentTimeMillis() + (e.retryAfter > 0 ? e.retryAfter : 600) * 1000L;
            state = State.RATE_LIMITED;
        } else if (e.status == 0) {
            fail(T.get("link.error.offline"), true);
        } else {
            fail(e.getMessage() == null || e.getMessage().isBlank() ? T.get("link.error.generic") : e.getMessage(), e.status >= 500);
        }
        LauncherStatus.changed();
    }

    private static void fail(String text, boolean autoRetry) {
        state = State.ERROR;
        error = text;
        if (autoRetry && failures++ < 3) {
            long delay = 15_000L << Math.min(failures - 1, 2);
            retryTask = Tasks.later(delay, () -> Tasks.onMain(() -> {
                if (state == State.ERROR) create();
            }));
        }
    }

    private static void cancelRetry() {
        ScheduledFuture<?> r = retryTask;
        retryTask = null;
        if (r != null) r.cancel(false);
    }

    private static void delete(Link l, boolean wait) {
        VibeBackend backend = Services.get(VibeBackend.class).orElse(madeWith);
        if (backend == null || l == null || l.code() == null) return;
        var f = backend.request("DELETE", PATH + "/" + l.code(), null);
        if (!wait) {
            f.whenComplete((r, e) -> {
                if (e == null) MultiplayerModule.LOG.info("Play link {} deleted", l.code());
                else MultiplayerModule.LOG.debug("Play link {} not deleted: {}", l.code(), unwrap(e).code);
            });
            return;
        }
        try {
            f.get(QUIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            // best effort; the backend lets it expire
        }
    }

    private static void onQuit() {
        Link l = link;
        wanted = null;
        link = null;
        generation++;
        if (l != null) delete(l, true);
    }

    // ============================================================================================ body / answer

    /** The {@code POST /api/v1/play-links} body (§3). */
    static JsonObject body(String relayHost, InstanceInfo info, String worldName, int maxPlayers) {
        JsonObject o = new JsonObject();
        o.addProperty("relay_host", relayHost);
        o.addProperty("mc_version", MpLoader.minecraftVersion());
        JsonObject loader = new JsonObject();
        if (info.present() && info.loaderKind() != null) {
            loader.addProperty("kind", info.loaderKind());
            if (info.loaderVersion() != null) loader.addProperty("version", info.loaderVersion());
        } else {
            loader.addProperty("kind", Vibey.platform().loader().toLowerCase(Locale.ROOT));
            String v = loaderVersion();
            if (v != null) loader.addProperty("version", v);
        }
        o.add("loader", loader);
        JsonObject fields = info.playLinkFields();
        for (var e : fields.entrySet()) o.add(e.getKey(), e.getValue());
        if (worldName != null && !worldName.isBlank()) {
            String w = worldName.strip();
            o.addProperty("world_name", w.length() <= 64 ? w : w.substring(0, 64));
        }
        if (maxPlayers > 0) o.addProperty("max_players", maxPlayers);
        return o;
    }

    private static String loaderVersion() {
        String id = Vibey.platform().isFabric() ? "fabricloader" : "neoforge";
        for (Platform.ModInfo m : Vibey.platform().mods()) if (m.id().equals(id)) return m.version();
        return null;
    }

    static Link parse(JsonElement res, String relayHost) {
        if (res == null || !res.isJsonObject()) return null;
        JsonObject o = res.getAsJsonObject();
        String code = str(o, "code");
        String url = str(o, "url");
        if (code == null || url == null || !code.matches("[a-z0-9]{4,16}")) return null;
        return new Link(code, url, expiry(o.get("expires_at")), relayHost);
    }

    /** {@code expires_at} as ISO-8601 text or unix seconds/millis → epoch millis, 0 when unknown. */
    static long expiry(JsonElement e) {
        if (e == null || !e.isJsonPrimitive()) return 0;
        try {
            if (e.getAsJsonPrimitive().isNumber()) {
                long v = e.getAsLong();
                return v > 100_000_000_000L ? v : v * 1000L;
            }
            String s = e.getAsString().trim();
            try {
                return Instant.parse(s).toEpochMilli();
            } catch (RuntimeException ex) {
                return OffsetDateTime.parse(s).toInstant().toEpochMilli();
            }
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static ApiException unwrap(Throwable t) {
        while ((t instanceof CompletionException || t instanceof ExecutionException) && t.getCause() != null) t = t.getCause();
        if (t instanceof ApiException a) return a;
        return new ApiException(0, "network", t == null ? "?" : String.valueOf(t.getMessage()));
    }
}
