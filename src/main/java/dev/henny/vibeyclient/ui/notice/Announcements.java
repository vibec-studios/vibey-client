package dev.henny.vibeyclient.ui.notice;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.backend.BackendClient;
import dev.henny.vibeyclient.backend.DomainMove;
import dev.henny.vibeyclient.backend.Notices;
import dev.henny.vibeyclient.backend.Notices.Notice;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.VibeyTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Notices for the player (vibe-launcher docs/DOMAINS.md "Hinweise an Nutzer"): the built-in domain-move notice and
 * the backend's announcements ({@code GET {backend}/api/v1/announcements?app=client&version=…&lang=de|en}, public).
 * Each id is shown once as a Vibey toast and as a card on the General tab until the player dismisses it; dismissed
 * and toasted ids are kept in {@code core.json} ({@code _data.notices}). The remote list is fetched on a worker
 * thread at start and every 6 hours, only while online features are on; errors are only logged (debug) and then
 * nothing remote is shown. A remote item with the built-in id replaces the built-in one (same notice, shown once).
 */
public final class Announcements {
    private Announcements() {}

    /** The built-in notice: the domain move, same id as the backend's announcement. */
    public static final String BUILTIN_ID = DomainMove.ID;
    static final long BUILTIN_UNTIL = Instant.parse("2027-03-01T00:00:00Z").toEpochMilli();

    static final long PERIOD_MS = 6 * 60 * 60_000L;
    static final long RETRY_MS = 60 * 60_000L;
    private static final int MAX_REMEMBERED = 200;

    private static volatile List<Notice> remote = List.of();
    /** Backend + language the remote list is for. */
    private static volatile String remoteFor;
    private static volatile long nextFetch;
    private static volatile boolean fetching;
    private static volatile long lastFetchDone;
    private static int ticks;
    private static boolean initDone;
    private static HttpClient http;
    private static final List<String> TOASTED_THIS_RUN = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Startup (client thread, after the global settings were read). */
    public static void init() {
        if (initDone) return;
        initDone = true;
        Events.subscribe(VibeyEvents.OnlineFeaturesChanged.class, e -> {
            if (e.enabled()) nextFetch = 0;
            else remote = List.of();
        });
        Events.subscribe(VibeyEvents.LanguageChanged.class, e -> nextFetch = 0);
    }

    /** Every client tick: starts a fetch when due, shows a pending toast now and then. */
    public static void tick() {
        if (++ticks % 20 != 0) return;
        long now = System.currentTimeMillis();
        if (GlobalSettings.online() && !fetching) {
            String key = key();
            if (!key.equals(remoteFor)) {
                if (remoteFor != null) remote = List.of(); // other backend or language: the old list is not for it
                nextFetch = 0;
            }
            if (now >= nextFetch) fetch(key);
        }
        if (ticks % 40 == 0) toastPending();
    }

    /** What to show on the General tab: visible, not dismissed, built-in first. Client thread. */
    public static List<Notice> active() {
        long now = System.currentTimeMillis();
        List<Notice> out = new ArrayList<>();
        List<Notice> r = GlobalSettings.online() ? remote : List.of();
        // the backend's copy of the built-in notice replaces it (same id: one notice, one toast)
        List<Notice> all = new ArrayList<>(r.size() + 1);
        all.add(r.stream().filter(n -> n.id().equals(BUILTIN_ID)).findFirst().orElseGet(Announcements::builtin));
        for (Notice n : r) if (!n.id().equals(BUILTIN_ID)) all.add(n);
        for (Notice n : all) {
            if (Notices.visible(n, now, BuildInfo.VERSION) && !has("dismissed", n.id())) out.add(n);
        }
        return out;
    }

    /** The built-in notice in the current language. */
    public static Notice builtin() {
        return new Notice(BUILTIN_ID, Notices.Level.INFO, Lang.tr("core.notice.domainMove.title"),
                Lang.tr("core.notice.domainMove.body"), null, BUILTIN_UNTIL, List.of(), null);
    }

    /** The player closed it: never shown again (also not as a toast). */
    public static void dismiss(String id) {
        remember("dismissed", id);
        remember("toasted", id);
    }

    public static boolean dismissed(String id) {
        return has("dismissed", id);
    }

    public static boolean toasted(String id) {
        return has("toasted", id);
    }

    /** Opens the notice's link through the game's "open this link?" confirmation. */
    public static void open(Notice n) {
        if (n.url() != null) ClientCompat.openLink(ClientCompat.screen(), n.url());
    }

    // ============================================================================================ toasts

    private static void toastPending() {
        Minecraft mc = Minecraft.getInstance();
        if (!GlobalSettings.onboarded() || !ModuleManager.get().isStarted() || mc.getOverlay() != null) return;
        boolean place = mc.screen instanceof TitleScreen || mc.screen instanceof VibeyScreen || (mc.screen == null && mc.level != null);
        if (!place) return;
        for (Notice n : active()) {
            if (toasted(n.id())) continue;
            remember("toasted", n.id());
            TOASTED_THIS_RUN.add(n.id());
            Notify.show(new Notify.Toast(n.title(), n.body(), n.level() == Notices.Level.WARN ? Notify.Kind.WARN : Notify.Kind.INFO,
                    null, Announcements::openMenu));
            Vibey.LOG.info("Notice {} shown", n.id());
            return; // one at a time
        }
    }

    private static void openMenu() {
        if (ClientCompat.screen() instanceof VibeyScreen) return;
        ClientCompat.openScreen(new VibeyScreen(ClientCompat.screen(), VibeyTabs.GENERAL));
    }

    // ============================================================================================ fetch

    private static String key() {
        return BackendClient.configuredBaseUrl() + "|" + lang();
    }

    private static String lang() {
        return Lang.german() ? "de" : "en";
    }

    static String url(String base, String version, String lang) {
        return base + "/api/v1/announcements?app=client&version=" + URLEncoder.encode(version, StandardCharsets.UTF_8)
                + "&lang=" + lang;
    }

    private static void fetch(String key) {
        fetching = true;
        String url = url(BackendClient.configuredBaseUrl(), BuildInfo.VERSION, lang());
        try {
            if (http == null) http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).executor(Tasks.executor()).build();
            http.sendAsync(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                            .header("Accept", "application/json").GET().build(), HttpResponse.BodyHandlers.ofString())
                    .whenComplete((res, err) -> {
                        List<Notice> list = null;
                        if (err != null) {
                            Vibey.LOG.debug("Announcements not available: {}", err.toString());
                        } else if (res.statusCode() / 100 != 2) {
                            Vibey.LOG.debug("Announcements not available: HTTP {}", res.statusCode());
                        } else {
                            try {
                                list = Notices.parse(JsonParser.parseString(res.body()));
                            } catch (RuntimeException e) {
                                Vibey.LOG.debug("Announcements unreadable: {}", e.toString());
                            }
                        }
                        List<Notice> got = list;
                        Tasks.onMain(() -> done(key, got));
                    });
        } catch (RuntimeException e) {
            Vibey.LOG.debug("Announcements not requested: {}", e.toString());
            done(key, null);
        }
    }

    private static void done(String key, List<Notice> list) {
        long now = System.currentTimeMillis();
        if (list != null) {
            remote = list;
            remoteFor = key;
            nextFetch = now + PERIOD_MS;
        } else {
            if (!key.equals(remoteFor)) remote = List.of();
            remoteFor = key;
            nextFetch = now + RETRY_MS;
        }
        lastFetchDone = now;
        fetching = false;
    }

    // ============================================================================================ state

    private static JsonObject state() {
        JsonObject data = GlobalSettings.GROUP.data();
        JsonElement e = data.get("notices");
        if (e == null || !e.isJsonObject()) {
            e = new JsonObject();
            data.add("notices", e);
        }
        return e.getAsJsonObject();
    }

    private static boolean has(String list, String id) {
        JsonElement st = GlobalSettings.GROUP.data().get("notices");
        JsonElement e = st != null && st.isJsonObject() ? st.getAsJsonObject().get(list) : null;
        if (e == null || !e.isJsonArray()) return false;
        for (JsonElement x : e.getAsJsonArray()) if (x.isJsonPrimitive() && id.equals(x.getAsString())) return true;
        return false;
    }

    private static void remember(String list, String id) {
        if (has(list, id)) return;
        synchronized (GlobalSettings.GROUP) {
            JsonObject s = state();
            JsonElement e = s.get(list);
            JsonArray a = e != null && e.isJsonArray() ? e.getAsJsonArray() : new JsonArray();
            a.add(id);
            while (a.size() > MAX_REMEMBERED) a.remove(0);
            s.add(list, a);
        }
        GlobalSettings.GROUP.markDirty();
    }

    // ============================================================================================ tests

    /** Gametest: forget the remote list and fetch again at the next tick. */
    public static void debugRefetch() {
        nextFetch = 0;
        remoteFor = null;
        remote = List.of();
    }

    /** Gametest: when the last fetch finished (epoch millis, 0 = never). */
    public static long debugLastFetch() {
        return lastFetchDone;
    }

    /** Gametest: the remote items as received (before the visibility rules). */
    public static List<Notice> debugRemote() {
        return remote;
    }

    /** Gametest: the ids toasted since the game started, in order (an id twice would be a bug). */
    public static List<String> debugToasts() {
        return List.copyOf(TOASTED_THIS_RUN);
    }

    /** Gametest: forget toasted/dismissed ids. */
    public static void debugReset() {
        synchronized (GlobalSettings.GROUP) {
            GlobalSettings.GROUP.data().remove("notices");
        }
        GlobalSettings.GROUP.markDirty();
    }
}
