package dev.henny.vibeyclient.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.HostingService;
import dev.henny.vibeyclient.api.service.VibeBackend;
import dev.henny.vibeyclient.module.multiplayer.MultiplayerModule;
import dev.henny.vibeyclient.module.multiplayer.host.HostSession;
import dev.henny.vibeyclient.module.multiplayer.host.PlayLinks;
import dev.henny.vibeyclient.module.multiplayer.screen.HostScreen;
import dev.henny.vibeyclient.ui.VibeyButton;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.PauseScreen;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.function.Predicate;

/**
 * Play links + {@code .vibe/session.json} (vibe-launcher docs/PLAY-LINKS.md §2/§3) against LOCAL services only, either
 * <ul>
 *   <li>a local <b>vibe-backend</b> (AUTH_DEV_BYPASS) with the play-link routes and a local relay started with
 *       {@code ALLOW_OFFLINE=1 DOMAIN=axolite.cc} (the backend only accepts relay hosts under its relay domains; the
 *       client still connects through {@code 127.0.0.1:<port>}), or</li>
 *   <li>{@code scripts/mock-play-links.mjs} (play-link routes, proxies login/gateway to a local vibe-backend), which
 *       also simulates a backend without the routes. Detected by its {@code /__mock/links} route.</li>
 * </ul>
 * Runs only with {@code -Dvibeyclient.playLinkTest=<review dir> -Dvibeyclient.api=http://127.0.0.1:<port>
 * -Dvibeyclient.devAuth=true -Dvibeyclient.test.relay=127.0.0.1:<relay>}. Screenshots, session.json samples, the
 * backend's link JSON and the landing page HTML go to the review dir.
 */
public final class PlayLinksGameTest implements FabricClientGameTest {
    private static final int SEC = 20;
    private static final HttpClient HTTP = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5)).build();

    private String api;
    private Path review;
    private Path vibeDir;
    /** Against the real backend (no /__mock routes). */
    private boolean real;
    /** Code charset of the contract (§3): no 0/o, 1/i/l. */
    private static final String CODE_RE = "[23456789abcdefghjkmnpqrstuvwxyz]{6,8}";

    @Override
    public void runTest(ClientGameTestContext ctx) {
        String out = System.getProperty("vibeyclient.playLinkTest");
        if (out == null || out.isBlank()) return;
        api = System.getProperty("vibeyclient.api", "");
        if (!api.startsWith("http://127.0.0.1:") && !api.startsWith("http://localhost:")) {
            throw new AssertionError("Play link test only runs against a local backend or mock, not " + api);
        }
        String relay = System.getProperty("vibeyclient.test.relay", "127.0.0.1:26400");
        review = Path.of(out);
        try {
            Files.createDirectories(review);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
        real = status("GET", "/__mock/links") != 200;
        log(real ? "against the real local backend at " + api : "against the mock at " + api);
        if (!real) {
            mock("POST", "/__mock/reset", null);
            mock("POST", "/__mock/config", "{\"missing\":false,\"rate\":10}");
        }

        Path game = Vibey.platform().gameDir().toAbsolutePath();
        vibeDir = game.resolve(".vibe");
        writeInstance(game);

        ctx.waitFor(mc -> Services.get(VibeBackend.class).map(VibeBackend::isReady).orElse(false), 90 * SEC);
        log("backend ready as " + ctx.computeOnClient(mc -> Services.get(VibeBackend.class).orElseThrow().selfId()));

        JsonObject menu = waitSession(ctx, o -> "menu".equals(str(o, "state")), "menu state");
        save("session-menu.json", menu);

        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("playlinks")).create()) {
            world.getServer().runCommand("time set 1000");
            world.getServer().runCommand("weather clear");
            world.getClientWorld().waitForChunksRender();
            JsonObject sp = waitSession(ctx, o -> "singleplayer".equals(str(o, "state")), "singleplayer state");
            if (sp.has("play_link") || sp.has("server") || str(sp, "world") == null) throw new AssertionError("singleplayer session.json: " + sp);

            // ---- hosting goes online: link on the mock
            ctx.runOnClient(mc -> {
                MultiplayerModule.instance().relay.set(relay);
                Services.get(HostingService.class).orElseThrow().startHosting();
            });
            ctx.waitFor(mc -> HostSession.status() == HostSession.Status.ONLINE || HostSession.status() == HostSession.Status.ERROR, 30 * SEC);
            if (HostSession.status() != HostSession.Status.ONLINE) throw new AssertionError("Hosting failed: " + HostSession.error());
            ctx.waitFor(mc -> PlayLinks.state() == PlayLinks.State.READY || PlayLinks.state() == PlayLinks.State.ERROR
                    || PlayLinks.state() == PlayLinks.State.HIDDEN, 20 * SEC);
            if (PlayLinks.state() != PlayLinks.State.READY) throw new AssertionError("No play link: " + PlayLinks.state() + " " + PlayLinks.error());
            String address = ctx.computeOnClient(mc -> HostSession.address());
            String url = PlayLinks.url();
            log("hosting at " + address + ", link " + url);
            String code0 = PlayLinks.link().code();
            if (!code0.matches(CODE_RE)) throw new AssertionError("code outside the contract charset: " + code0);
            if (!real) {
                JsonArray links = links();
                if (links.size() != 1) throw new AssertionError("mock links: " + links);
            }
            // the public view (GET /api/v1/play-links/<code>) is what the launcher and the landing page read
            JsonObject l = linkInfo(code0);
            if (l == null) throw new AssertionError("link " + code0 + " not found on the backend");
            save("backend-link.json", l);
            check(l, "code", code0);
            check(l, "url", url);
            checkTimes(l, PlayLinks.link().expiresAt());
            if (!l.has("host") || str(l.getAsJsonObject("host"), "uuid") == null) throw new AssertionError("host: " + l);
            if (!l.has("active")) throw new AssertionError("no 'active': " + l);
            String landing = get("/j/" + code0);
            if (landing == null || !landing.contains(code0)) throw new AssertionError("landing page /j/" + code0 + ": " + landing);
            try {
                Files.writeString(review.resolve("landing-" + code0 + ".html"), landing);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
            log("backend link " + l);
            check(l, "relay_host", address.toLowerCase());
            check(l, "world_name", str(sp, "world"));
            check(l, "client_pack", "vibey");
            check(l, "mc_version", "1.21.11");
            if (l.get("max_players").getAsInt() != 8) throw new AssertionError("max_players: " + l);
            if (!"fabric".equals(str(l.getAsJsonObject("loader"), "kind"))) throw new AssertionError("loader: " + l);
            if (l.getAsJsonArray("mods").size() != 3) throw new AssertionError("mods from instance.json: " + l.get("mods"));
            if (!url.endsWith("/j/" + str(l, "code"))) throw new AssertionError("url " + url + " vs " + l);
            int manual = PlayLinks.manualMods();
            if (manual != 3) throw new AssertionError("hand-added mods: " + manual);

            // ---- host dialog: link, expiry, hint; click "Copy invite link"
            ctx.setScreen(() -> new HostScreen(null));
            ctx.waitTicks(15);
            shot(ctx, "playlinks_1_host_dialog");
            ctx.runOnClient(mc -> mc.keyboardHandler.setClipboard(""));
            int[] b = ctx.computeOnClient(mc -> mc.screen instanceof HostScreen h ? h.inviteButtonBounds() : null);
            if (b == null) throw new AssertionError("no copy button in the host dialog");
            click(ctx, b[0] + b[2] / 2.0, b[1] + b[3] / 2.0);
            ctx.waitTicks(6);
            String clip = ctx.computeOnClient(mc -> mc.keyboardHandler.getClipboard());
            if (!url.equals(clip)) throw new AssertionError("clipboard after the host dialog button: " + clip);
            shot(ctx, "playlinks_2_host_dialog_copied_toast");
            ctx.setScreen(() -> null);
            ctx.waitTicks(20 * 6); // let the toast go

            // ---- pause menu row: "Copy invite link"
            ctx.runOnClient(mc -> mc.keyboardHandler.setClipboard(""));
            ctx.setScreen(() -> new PauseScreen(true));
            ctx.waitTicks(8);
            int[] pb = ctx.computeOnClient(mc -> {
                for (var c : mc.screen.children()) {
                    if (c instanceof VibeyButton v && v.getMessage().getString().equals("Copy invite link")) {
                        return new int[]{v.getX(), v.getY(), v.getWidth(), v.getHeight()};
                    }
                }
                return null;
            });
            if (pb == null) throw new AssertionError("no 'Copy invite link' in the pause menu");
            shot(ctx, "playlinks_3_pause_menu");
            click(ctx, pb[0] + pb[2] / 2.0, pb[1] + pb[3] / 2.0);
            ctx.waitTicks(6);
            clip = ctx.computeOnClient(mc -> mc.keyboardHandler.getClipboard());
            if (!url.equals(clip)) throw new AssertionError("clipboard after the pause button: " + clip);
            shot(ctx, "playlinks_4_pause_menu_toast");
            ctx.setScreen(() -> null);

            // ---- session.json while hosting, heartbeat
            JsonObject hosting = waitSession(ctx, o -> "hosting".equals(str(o, "state")) && url.equals(str(o, "play_link")), "hosting state");
            if (hosting.get("players").getAsInt() != 1 || hosting.get("max_players").getAsInt() != 8 || hosting.has("server")) {
                throw new AssertionError("hosting session.json: " + hosting);
            }
            save("session-hosting.json", hosting);
            long t0 = hosting.get("updated_at").getAsLong();
            ctx.waitTicks(17 * SEC);
            long t1 = readSession().get("updated_at").getAsLong();
            if (t1 <= t0) throw new AssertionError("no heartbeat: updated_at " + t0 + " -> " + t1);
            log("heartbeat ok: updated_at " + t0 + " -> " + t1);

            // ---- new relay address: a new link replaces the old one
            String oldCode = str(l, "code");
            ctx.runOnClient(mc -> HostSession.rotate());
            boolean rotated = waitUntil(ctx, () -> !address.equals(HostSession.address()) && PlayLinks.state() == PlayLinks.State.READY
                    && PlayLinks.link() != null && !PlayLinks.link().code().equals(oldCode), 20 * SEC);
            if (rotated) {
                String addr2 = ctx.computeOnClient(mc -> HostSession.address());
                JsonObject now = linkInfo(PlayLinks.link().code());
                if (now == null || !addr2.toLowerCase().equals(str(now, "relay_host"))) throw new AssertionError("after the new address: " + now);
                if (linkInfo(oldCode) != null) throw new AssertionError("old link " + oldCode + " still there");
                if (!real && links().size() != 1) throw new AssertionError("after the new address: " + links());
                log("new address " + addr2 + " -> new link " + PlayLinks.url() + " (old " + oldCode + " gone)");
            } else {
                log("relay gave no new address (" + HostSession.notice() + "), re-create check skipped");
            }

            // ---- stop hosting: link deleted, session back to singleplayer
            String lastCode = PlayLinks.link().code();
            ctx.runOnClient(mc -> Services.get(HostingService.class).orElseThrow().stopHosting());
            ctx.waitFor(mc -> HostSession.status() == HostSession.Status.OFF, 5 * SEC);
            if (!waitUntil(ctx, () -> linkInfo(lastCode) == null && (real || links().isEmpty()), 10 * SEC)) {
                throw new AssertionError("link " + lastCode + " not deleted");
            }
            log("stopped hosting: link deleted");
            JsonObject after = waitSession(ctx, o -> "singleplayer".equals(str(o, "state")), "singleplayer after hosting");
            if (after.has("play_link")) throw new AssertionError("play_link after hosting: " + after);
            save("session-singleplayer.json", after);
            ctx.setScreen(() -> new HostScreen(null));
            ctx.waitTicks(10);
            shot(ctx, "playlinks_5_host_dialog_stopped");
            ctx.setScreen(() -> null);

            if (!real) {
                // ---- backend without the routes (404): hosting works, no invite UI (mock only)
                mock("POST", "/__mock/config", "{\"missing\":true}");
                hostAndWait(ctx, PlayLinks.State.HIDDEN);
                if (PlayLinks.visible()) throw new AssertionError("invite UI visible although the backend has no play links");
                ctx.setScreen(() -> new HostScreen(null));
                ctx.waitTicks(10);
                shot(ctx, "playlinks_6_backend_without_links");
                ctx.setScreen(() -> null);
                stopHosting(ctx);
                mock("POST", "/__mock/config", "{\"missing\":false,\"rate\":0}");
            } else {
                // ---- the real limit: 10 new links per hour and user; use up the rest with the same (dev) account
                String name = ctx.computeOnClient(mc -> mc.getUser().getName());
                exhaustRateLimit(name);
            }

            // ---- rate limit (10 per hour)
            hostAndWait(ctx, PlayLinks.State.RATE_LIMITED);
            long wait = PlayLinks.retryAt() - System.currentTimeMillis();
            if (wait <= 0 || wait > 3_700_000L) throw new AssertionError("retry in " + wait + " ms");
            log("rate limited, retry in " + wait / 1000 + " s");
            ctx.setScreen(() -> new HostScreen(null));
            ctx.waitTicks(10);
            shot(ctx, "playlinks_7_rate_limited");
            ctx.setScreen(() -> null);
            stopHosting(ctx);
            if (!real) mock("POST", "/__mock/config", "{\"rate\":10}");

            // ---- online features off: no link, no UI; switch "show to the launcher" off deletes session.json
            ctx.runOnClient(mc -> GlobalSettings.onlineFeatures.set(false));
            hostAndWait(ctx, PlayLinks.State.HIDDEN);
            if (PlayLinks.link() != null || !real && !links().isEmpty()) throw new AssertionError("link created with online features off");
            stopHosting(ctx);
            ctx.runOnClient(mc -> GlobalSettings.onlineFeatures.set(true));

            ctx.runOnClient(mc -> GlobalSettings.launcherStatus.set(false));
            if (!waitUntil(ctx, () -> !Files.exists(vibeDir.resolve("session.json")), 5 * SEC)) throw new AssertionError("session.json still there with the setting off");
            ctx.runOnClient(mc -> GlobalSettings.launcherStatus.set(true));
            waitSession(ctx, o -> "singleplayer".equals(str(o, "state")), "session.json back on");
            log("setting off deletes session.json, on writes it again");
        }
        JsonObject end = waitSession(ctx, o -> "menu".equals(str(o, "state")), "menu after the world");
        log("back in the menu: " + end);
        if (!real) save("mock-links-at-end.json", links());
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
        log("done");
    }

    // ============================================================================================ steps

    private void hostAndWait(ClientGameTestContext ctx, PlayLinks.State want) {
        ctx.runOnClient(mc -> Services.get(HostingService.class).orElseThrow().startHosting());
        ctx.waitFor(mc -> HostSession.status() == HostSession.Status.ONLINE, 30 * SEC);
        if (!waitUntil(ctx, () -> PlayLinks.state() == want, 15 * SEC)) {
            throw new AssertionError("expected " + want + ", is " + PlayLinks.state() + " " + PlayLinks.error());
        }
        log("hosting with play link state " + want);
    }

    private void stopHosting(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> Services.get(HostingService.class).orElseThrow().stopHosting());
        ctx.waitFor(mc -> HostSession.status() == HostSession.Status.OFF, 5 * SEC);
        ctx.waitTicks(10);
    }

    /** A launcher-written instance.json (no modpack, 3 mods with a source, one with a file name) + 3 hand-added jars. */
    private static void writeInstance(Path game) {
        try {
            Path vibe = Files.createDirectories(game.resolve(".vibe"));
            Files.writeString(vibe.resolve("instance.json"), """
                    {
                      "schema": 1,
                      "name": "Vibey Dev",
                      "mc_version": "1.21.11",
                      "loader": { "kind": "fabric", "version": "0.18.1" },
                      "pack": null,
                      "mods": [
                        { "source": "modrinth", "project_id": "AANobbMI", "version_id": "abc123", "kind": "mod", "file": "sodium-fabric-0.6.jar" },
                        { "source": "modrinth", "project_id": "P7dR8mSH", "version_id": "def456", "kind": "mod", "file": "fabric-api.jar" },
                        { "source": "modrinth", "project_id": "faithful-32x", "kind": "resourcepack", "file": "faithful.zip" }
                      ],
                      "client_pack": "vibey"
                    }
                    """, StandardCharsets.UTF_8);
            Path mods = Files.createDirectories(game.resolve("mods"));
            for (String f : new String[]{"sodium-fabric-0.6.jar", "fabric-api.jar", "my-own-mod.jar", "friend-gave-me-this.jar", "old-minimap.jar"}) {
                if (!Files.exists(mods.resolve(f))) Files.writeString(mods.resolve(f), "not a real jar (play link test)");
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    // ============================================================================================ helpers

    private static void click(ClientGameTestContext ctx, double guiX, double guiY) {
        double scale = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
        ctx.getInput().setCursorPos(guiX * scale, guiY * scale);
        ctx.waitTicks(2);
        ctx.getInput().pressMouse(0);
    }

    private JsonObject waitSession(ClientGameTestContext ctx, Predicate<JsonObject> ok, String what) {
        JsonObject[] last = new JsonObject[1];
        if (!waitUntil(ctx, () -> {
            last[0] = readSession();
            return last[0] != null && ok.test(last[0]);
        }, 10 * SEC)) throw new AssertionError("session.json never reached " + what + ": " + last[0]);
        JsonObject o = last[0];
        long age = System.currentTimeMillis() / 1000 - o.get("updated_at").getAsLong();
        if (o.get("schema").getAsInt() != 1 || age > 60 || age < -5) throw new AssertionError("session.json: " + o);
        return o;
    }

    private JsonObject readSession() {
        try {
            Path f = vibeDir.resolve("session.json");
            if (!Files.exists(f)) return null;
            return JsonParser.parseString(Files.readString(f)).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            return null; // mid-move on Windows: next try
        }
    }

    /** Polls {@code cond} on the test thread every 10 ticks (the client keeps running meanwhile). */
    private static boolean waitUntil(ClientGameTestContext ctx, java.util.function.BooleanSupplier cond, int ticks) {
        for (int t = 0; t <= ticks; t += 10) {
            if (cond.getAsBoolean()) return true;
            ctx.waitTicks(10);
        }
        return cond.getAsBoolean();
    }

    /** The public view of a link, null when the backend says 404 (unknown, expired or deleted). */
    private JsonObject linkInfo(String code) {
        String body = get("/api/v1/play-links/" + code);
        return body == null ? null : JsonParser.parseString(body).getAsJsonObject();
    }

    /** GET → body, null on 404. */
    private String get(String path) {
        try {
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(api + path)).timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 404) return null;
            if (r.statusCode() >= 300) throw new AssertionError("GET " + path + ": " + r.statusCode() + " " + r.body());
            return r.body();
        } catch (IOException e) {
            throw new AssertionError("backend not reachable at " + api, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private int status(String method, String path) {
        try {
            return HTTP.send(HttpRequest.newBuilder(URI.create(api + path)).timeout(Duration.ofSeconds(10))
                    .method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (IOException e) {
            throw new AssertionError("backend not reachable at " + api, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    /** created_at / expires_at: ISO-8601, 12 h apart (§3), and the client read the same expiry. */
    private static void checkTimes(JsonObject l, long clientExpiry) {
        java.time.Instant created = java.time.Instant.parse(str(l, "created_at"));
        java.time.Instant expires = java.time.Instant.parse(str(l, "expires_at"));
        long ttl = expires.getEpochSecond() - created.getEpochSecond();
        if (ttl < 12 * 3600 - 60 || ttl > 12 * 3600 + 60) throw new AssertionError("link lifetime " + ttl + " s: " + l);
        if (Math.abs(expires.toEpochMilli() - clientExpiry) > 2_000) throw new AssertionError("client expiry " + clientExpiry + " vs " + l);
        if (Math.abs(created.toEpochMilli() - System.currentTimeMillis()) > 120_000) throw new AssertionError("created_at: " + l);
    }

    /**
     * Logs in as {@code name} through the backend's dev login (local backend only) and creates links for a made-up
     * relay address until the backend answers 429; the last one is deleted again.
     */
    private void exhaustRateLimit(String name) {
        JsonObject ch = JsonParser.parseString(post("/api/v1/auth/challenge", "{}", null)).getAsJsonObject();
        JsonObject verify = new JsonObject();
        verify.addProperty("name", name);
        verify.addProperty("serverId", str(ch, "serverId"));
        JsonObject client = new JsonObject();
        client.addProperty("name", "vibey-client");
        client.addProperty("version", "0.0.0");
        client.addProperty("platform", "windows");
        verify.add("client", client);
        JsonObject tokens = JsonParser.parseString(post("/api/v1/auth/verify", verify.toString(), null)).getAsJsonObject();
        String token = str(tokens, "accessToken");
        if (token == null) throw new AssertionError("dev login: " + tokens);
        String body = "{\"relay_host\":\"ratelimit-test.axolite.cc\",\"mc_version\":\"1.21.11\",\"loader\":{\"kind\":\"fabric\"}}";
        String last = null;
        for (int i = 0; i < 12; i++) {
            String res = post("/api/v1/play-links", body, token);
            if (res == null) {
                log("rate limit reached after " + i + " extra links");
                if (last != null) {
                    try {
                        HTTP.send(HttpRequest.newBuilder(URI.create(api + "/api/v1/play-links/" + last)).timeout(Duration.ofSeconds(10))
                                .header("authorization", "Bearer " + token).DELETE().build(), HttpResponse.BodyHandlers.discarding());
                    } catch (IOException e) {
                        // only cleanup
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return;
            }
            last = str(JsonParser.parseString(res).getAsJsonObject(), "code");
        }
        throw new AssertionError("no 429 after 12 links");
    }

    /** POST → body on 2xx, null on 429, else an assertion error. */
    private String post(String path, String body, String token) {
        try {
            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(api + path)).timeout(Duration.ofSeconds(10))
                    .header("content-type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
            if (token != null) rb.header("authorization", "Bearer " + token);
            HttpResponse<String> r = HTTP.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 429) return null;
            if (r.statusCode() >= 300) throw new AssertionError("POST " + path + ": " + r.statusCode() + " " + r.body());
            return r.body();
        } catch (IOException e) {
            throw new AssertionError("backend not reachable at " + api, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private JsonArray links() {
        JsonElement e = JsonParser.parseString(mock("GET", "/__mock/links", null));
        return e.getAsJsonArray();
    }

    private String mock(String method, String path, String body) {
        for (int attempt = 0; ; attempt++) {
            try {
                return mockOnce(method, path, body);
            } catch (AssertionError e) {
                if (attempt >= 3 || !(e.getCause() instanceof IOException)) throw e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    private String mockOnce(String method, String path, String body) {
        try {
            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(api + path)).timeout(Duration.ofSeconds(10));
            rb.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            if (body != null) rb.header("content-type", "application/json");
            HttpResponse<String> r = HTTP.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() >= 300) throw new AssertionError("mock " + method + " " + path + ": " + r.statusCode() + " " + r.body());
            return r.body();
        } catch (IOException e) {
            throw new AssertionError("mock not reachable at " + api, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private void shot(ClientGameTestContext ctx, String name) {
        Path p = ctx.takeScreenshot(name);
        try {
            Files.copy(p, review.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private void save(String name, JsonElement json) {
        try {
            Files.writeString(review.resolve(name), new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json) + "\n");
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static void check(JsonObject o, String key, String want) {
        if (!want.equals(str(o, key))) throw new AssertionError(key + ": want " + want + ", got " + o.get(key) + " in " + o);
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static void log(String s) {
        System.out.println("[vibeyclient-playlinks-test] " + s);
    }
}
