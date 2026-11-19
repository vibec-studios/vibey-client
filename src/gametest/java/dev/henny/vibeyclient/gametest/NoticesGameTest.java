package dev.henny.vibeyclient.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.backend.DomainMove;
import dev.henny.vibeyclient.backend.Notices;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.VibeyTabs;
import dev.henny.vibeyclient.ui.notice.Announcements;
import dev.henny.vibeyclient.ui.notice.NoticeCards;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Domain move (vibe-launcher docs/DOMAINS.md) in the game, against {@code scripts/mock-announcements.mjs} only:
 * the stored old default backend is migrated once, the built-in notice shows once (toast + card on General),
 * dismissing it survives re-reading the settings file, a remote announcement shows once, an expired one and one
 * with a lower {@code max_version} stay hidden, the remote copy of the built-in id is not shown a second time, and
 * nothing is fetched while online features are off. Runs only with
 * {@code -Dvibeyclient.noticeTest=<review dir> -Dvibeyclient.api=http://127.0.0.1:<mock port>}; screenshots
 * {@code notice_*} (copy them from the gametest screenshot dir, it is wiped every run).
 */
public final class NoticesGameTest implements FabricClientGameTest {
    private static final int SEC = 20;
    private static final String LOG = "[vibeyclient-notice-test] ";
    private static final HttpClient HTTP = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5)).build();
    private static final String BUILTIN = Announcements.BUILTIN_ID;

    private String api;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        String out = System.getProperty("vibeyclient.noticeTest");
        if (out == null || out.isBlank()) return;
        api = System.getProperty("vibeyclient.api", "");
        if (!api.startsWith("http://127.0.0.1:") && !api.startsWith("http://localhost:")) {
            throw new AssertionError("Notice test only runs against a local mock, not " + api);
        }
        Path review = Path.of(out);
        try {
            Files.createDirectories(review);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
        mock("/__mock/reset", "{}");
        mock("/__mock/announcements", "{\"items\":[]}");
        mock("/__mock/config", "{\"filter\":false}"); // the client must drop launcher-only items itself

        // ---- migration of a stored old default (the real core.json, read again)
        String migrated = ctx.computeOnClient(mc -> {
            GlobalSettings.GROUP.saveNow();
            Path f = GlobalSettings.GROUP.file();
            try {
                JsonObject json = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                json.addProperty("backendUrl", "https://vibe.henny.dev/");
                if (json.has("_data")) json.getAsJsonObject("_data").remove("migrations");
                Files.writeString(f, json.toString(), StandardCharsets.UTF_8);
                Files.writeString(review.resolve("core-before.json"), json.toString(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
            GlobalSettings.load();
            GlobalSettings.GROUP.saveNow();
            return GlobalSettings.backendUrl.get();
        });
        if (!DomainMove.NEW_BACKEND.equals(migrated)) throw new AssertionError("backendUrl not migrated: " + migrated);
        JsonObject after = readJson(ctx.computeOnClient(mc -> GlobalSettings.GROUP.file()));
        save(review, "core-after.json", after.toString());
        if (!DomainMove.done(after.getAsJsonObject("_data"), DomainMove.ID)) throw new AssertionError("migration not recorded: " + after);
        // run again: the old value set on purpose stays
        String kept = ctx.computeOnClient(mc -> {
            GlobalSettings.backendUrl.set("https://vibe.henny.dev");
            GlobalSettings.GROUP.saveNow();
            GlobalSettings.load();
            return GlobalSettings.backendUrl.get();
        });
        if (!"https://vibe.henny.dev".equals(kept)) throw new AssertionError("migration ran twice: " + kept);
        log("backendUrl migrated to " + migrated + ", recorded, not run twice");
        // the mock from now on (the -Dvibeyclient.api property wins for the client anyway; this keeps the menus local)
        ctx.runOnClient(mc -> {
            GlobalSettings.backendUrl.set(api);
            GlobalSettings.setOnboarded(true);
            Announcements.debugReset();
            Announcements.debugRefetch();
        });

        // ---- built-in notice: one toast on the title screen, one card on General
        long t0 = System.currentTimeMillis();
        waitUntil(ctx, () -> Announcements.debugLastFetch() >= t0, 20 * SEC, "first fetch");
        waitUntil(ctx, () -> Announcements.toasted(BUILTIN), 20 * SEC, "built-in toast");
        if (!(ctx.computeOnClient(mc -> mc.screen) instanceof TitleScreen)) throw new AssertionError("toast not on the title screen");
        ctx.waitTicks(10);
        ctx.takeScreenshot("notice_1_builtin_toast");
        JsonObject req = lastRequest();
        save(review, "request.json", req.toString());
        JsonObject q = req.getAsJsonObject("query");
        if (!"client".equals(str(q, "app")) || !BuildInfo.VERSION.equals(str(q, "version")) || !"en".equals(str(q, "lang")))
            throw new AssertionError("bad announcements request: " + req);
        log("request " + q);

        ctx.setScreen(() -> new VibeyScreen(new TitleScreen(), VibeyTabs.GENERAL));
        ctx.waitTicks(15);
        if (!NoticeCards.DISMISS_BOUNDS.containsKey(BUILTIN)) throw new AssertionError("no card for the built-in notice");
        ctx.takeScreenshot("notice_2_builtin_card");
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.GERMAN));
        ctx.waitTicks(10);
        ctx.takeScreenshot("notice_2_builtin_card_de");
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
        ctx.waitTicks(5);

        // ---- remote announcements: one shown, expired / newer-than-max / launcher-only hidden, built-in id not again
        String future = Instant.now().plusSeconds(86400).toString(), past = Instant.now().minusSeconds(3600).toString();
        mock("/__mock/announcements", "{\"items\":["
                + "{\"id\":\"" + BUILTIN + "\",\"level\":\"info\",\"title\":{\"de\":\"Vibe zieht um\",\"en\":\"Vibe is moving (remote)\"},"
                + "\"body\":{\"de\":\"Neue Adressen …\",\"en\":\"New addresses …\"},\"until\":\"2027-03-01T00:00:00Z\",\"apps\":[\"launcher\",\"client\"]},"
                + "{\"id\":\"test-remote\",\"level\":\"warn\",\"title\":{\"de\":\"Wartung heute Abend\",\"en\":\"Maintenance tonight\"},"
                + "\"body\":{\"de\":\"Ab 22 Uhr ist Vibe für etwa 15 Minuten nicht erreichbar.\",\"en\":\"From 10 pm Vibe is offline for about 15 minutes.\"},"
                + "\"url\":\"https://vibec-studios.github.io/\",\"until\":\"" + future + "\"},"
                + "{\"id\":\"test-expired\",\"title\":\"Expired\",\"until\":\"" + past + "\"},"
                + "{\"id\":\"test-old-clients\",\"title\":\"Only for old clients\",\"max_version\":\"0.0.1\"},"
                + "{\"id\":\"test-launcher\",\"title\":\"Launcher only\",\"apps\":[\"launcher\"]}"
                + "]}");
        ctx.setScreen(TitleScreen::new);
        long t1 = System.currentTimeMillis();
        ctx.runOnClient(mc -> Announcements.debugRefetch());
        waitUntil(ctx, () -> Announcements.debugLastFetch() >= t1, 20 * SEC, "second fetch");
        List<String> remoteIds = ctx.computeOnClient(mc -> Announcements.debugRemote().stream().map(Notices.Notice::id).toList());
        if (!remoteIds.equals(List.of(BUILTIN, "test-remote", "test-expired", "test-old-clients", "test-launcher")))
            throw new AssertionError("remote items: " + remoteIds);
        waitUntil(ctx, () -> Announcements.toasted("test-remote"), 20 * SEC, "remote toast");
        ctx.waitTicks(10);
        ctx.takeScreenshot("notice_3_remote_toast");
        List<String> active = ctx.computeOnClient(mc -> Announcements.active().stream().map(Notices.Notice::id).toList());
        if (!active.equals(List.of(BUILTIN, "test-remote"))) throw new AssertionError("active notices: " + active);
        String builtinTitle = ctx.computeOnClient(mc -> Announcements.active().get(0).title());
        if (!builtinTitle.contains("(remote)")) throw new AssertionError("remote copy did not replace the built-in text: " + builtinTitle);
        ctx.waitTicks(4 * SEC); // a second toast for the same id would come within 2 s
        List<String> toasts = ctx.computeOnClient(mc -> Announcements.debugToasts());
        if (!toasts.equals(List.of(BUILTIN, "test-remote"))) throw new AssertionError("toasts: " + toasts);
        log("remote shown once, hidden: expired, max_version 0.0.1, launcher only; toasts " + toasts);

        ctx.setScreen(() -> new VibeyScreen(new TitleScreen(), VibeyTabs.GENERAL));
        ctx.waitTicks(15);
        ctx.takeScreenshot("notice_4_two_cards");

        // ---- dismiss the built-in card with a click; still dismissed after reading core.json again
        int[] b = ctx.computeOnClient(mc -> NoticeCards.DISMISS_BOUNDS.get(BUILTIN));
        if (b == null) throw new AssertionError("no dismiss button");
        click(ctx, b[0] + b[2] / 2.0, b[1] + b[3] / 2.0);
        ctx.waitTicks(10);
        if (!ctx.computeOnClient(mc -> Announcements.dismissed(BUILTIN))) throw new AssertionError("click did not dismiss");
        ctx.takeScreenshot("notice_5_dismissed");
        boolean persisted = ctx.computeOnClient(mc -> {
            GlobalSettings.GROUP.saveNow();
            GlobalSettings.load(); // like the next start: everything from the file
            return Announcements.dismissed(BUILTIN) && Announcements.toasted("test-remote")
                    && Announcements.active().stream().noneMatch(n -> n.id().equals(BUILTIN));
        });
        if (!persisted) throw new AssertionError("dismissal not persisted");
        save(review, "core-dismissed.json", readJson(ctx.computeOnClient(mc -> GlobalSettings.GROUP.file())).toString());
        log("dismissed by click, still dismissed after re-reading " + ctx.computeOnClient(mc -> GlobalSettings.GROUP.file().getFileName()));

        // ---- online features off: no request, no remote notices
        ctx.runOnClient(mc -> GlobalSettings.onlineFeatures.set(false));
        int before = requests().size();
        ctx.runOnClient(mc -> Announcements.debugRefetch());
        ctx.waitTicks(4 * SEC);
        int afterOff = requests().size();
        List<String> offline = ctx.computeOnClient(mc -> Announcements.active().stream().map(Notices.Notice::id).toList());
        if (afterOff != before) throw new AssertionError("fetched while online features are off");
        if (!offline.isEmpty()) throw new AssertionError("notices while offline: " + offline);
        ctx.runOnClient(mc -> GlobalSettings.onlineFeatures.set(true));
        save(review, "mock-log.json", requests().toString());

        ctx.runOnClient(mc -> {
            for (Notices.Notice n : Announcements.active()) Announcements.dismiss(n.id());
            GlobalSettings.language.set(GlobalSettings.Language.AUTO);
        });
        ctx.setScreen(TitleScreen::new);
        log("done");
    }

    // ============================================================================================ helpers

    private static void click(ClientGameTestContext ctx, double guiX, double guiY) {
        double scale = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
        ctx.getInput().setCursorPos(guiX * scale, guiY * scale);
        ctx.waitTicks(2);
        ctx.getInput().pressMouse(0);
    }

    private static void waitUntil(ClientGameTestContext ctx, BooleanSupplier ok, int ticks, String what) {
        for (int t = 0; t < ticks; t += 5) {
            if (ctx.computeOnClient(mc -> ok.getAsBoolean())) return;
            ctx.waitTicks(5);
        }
        throw new AssertionError("timed out: " + what);
    }

    private JsonArray requests() {
        try {
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(api + "/__mock/log")).GET().build(), HttpResponse.BodyHandlers.ofString());
            JsonArray all = JsonParser.parseString(r.body()).getAsJsonObject().getAsJsonArray("requests");
            JsonArray out = new JsonArray();
            for (JsonElement e : all) if ("/api/v1/announcements".equals(str(e.getAsJsonObject(), "path"))) out.add(e);
            return out;
        } catch (Exception e) {
            throw new AssertionError("mock log: " + e);
        }
    }

    private JsonObject lastRequest() {
        JsonArray r = requests();
        if (r.isEmpty()) throw new AssertionError("no announcements request");
        return r.get(r.size() - 1).getAsJsonObject();
    }

    private void mock(String path, String body) {
        try {
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(api + path)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) throw new AssertionError("mock " + path + ": " + r.statusCode() + " " + r.body());
        } catch (IOException | InterruptedException e) {
            throw new AssertionError("mock not reachable at " + api + ": " + e);
        }
    }

    private static JsonObject readJson(Path f) {
        try {
            return JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static void save(Path dir, String name, String text) {
        try {
            Files.writeString(dir.resolve(name), text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o == null ? null : o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static void log(String s) {
        System.out.println(LOG + s);
    }
}
