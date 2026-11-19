import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.api.service.VibeBackend.ApiException;
import dev.henny.vibeyclient.backend.net.Bootstrap;
import dev.henny.vibeyclient.backend.net.HttpCache;
import dev.henny.vibeyclient.backend.net.Limiter;
import dev.henny.vibeyclient.backend.net.Meta;
import dev.henny.vibeyclient.backend.net.Net;
import dev.henny.vibeyclient.backend.net.Rest;
import dev.henny.vibeyclient.backend.net.SingleFlight;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * The request sequence of the reworked client, on the real {@code dev.henny.vibeyclient.backend.net} classes
 * (Net, HttpCache, Meta, Rest, Bootstrap, Limiter, SingleFlight). See {@link NetBench}.
 */
public class NetBenchAfter {
    /** ItemLoader.CATALOG_IO: 2 threads for hashing/parsing/disk. */
    static final ExecutorService IO = NetBench.Before.daemonPool("Catalog", 2);
    static final Limiter DOWNLOADS = new Limiter(4), THUMBS = new Limiter(4);
    static final SingleFlight<String, byte[]> FLIGHT = new SingleFlight<>();

    /** Net.send with the harness' timing record. */
    static CompletableFuture<Net.Response> send(HttpRequest req) {
        double s = NetBench.ms();
        return Net.send(req).whenComplete((r, e) -> {
            if (r != null) NetBench.rec(NetBench.label(req.uri().toString()), s, NetBench.ms(), NetBench.ms(), r.status(), r.wireBytes(), r.version().toString());
            else NetBench.rec(NetBench.label(req.uri().toString()) + " FAILED " + Net.cause(e), s, NetBench.ms(), NetBench.ms(), 0, 0, "-");
        });
    }

    public static void publicFlow(String base, Path dir, int busy, boolean warm) throws Exception {
        for (int i = 0; i < busy; i++) NetBench.Before.TASKS.execute(() -> NetBench.sleep(2000)); // no longer matters
        // title screen: BackendStatus → shared Meta (also serves social + signed loadouts later)
        double ms0 = NetBench.ms();
        CompletableFuture<Void> meta = Meta.get(base, 5 * 60_000L).handle((o, e) -> {
            NetBench.rec("/api/v1/meta" + (e != null ? " FAILED" : ""), ms0, NetBench.ms(), NetBench.ms(), e == null ? 200 : 0, o == null ? 0 : o.toString().length(), "HTTP_2");
            NetBench.mark("meta");
            return null;
        });
        CompletableFuture<Void> ann = send(Net.request(base + "/api/v1/announcements?app=client&version=0.3.1&lang=en", Net.Kind.API)
                .header("Accept", "application/json").GET().build()).handle((r, e) -> {
            NetBench.mark("announcements");
            return null;
        });
        // ItemLoader: disk copy first, conditional request already on its way, assets async (≤ 4, de-duplicated)
        HttpCache cache = new HttpCache(dir);
        HttpCache.Entry cached = cache.load("catalog");
        double cs = NetBench.ms();
        CompletableFuture<HttpCache.Result> net = cache.fetch("catalog", base + "/api/v1/cosmetics/catalog", cached, Net.Kind.API, IO);
        if (cached != null) {
            missing(base, dir, JsonParser.parseString(new String(cached.body(), StandardCharsets.UTF_8)).getAsJsonObject(), new ArrayList<>());
            NetBench.mark("catalogShown"); // cosmetics visible from the disk cache
        }
        HttpCache.Result res = net.join();
        NetBench.rec("/api/v1/cosmetics/catalog", cs, NetBench.ms(), NetBench.ms(), res.notModified() ? 304 : 200, res.notModified() ? 0 : res.body().length, "HTTP_2");
        NetBench.mark("catalog");
        JsonObject cat = JsonParser.parseString(new String(res.body(), StandardCharsets.UTF_8)).getAsJsonObject();
        List<String> thumbs = new ArrayList<>();
        List<String[]> missing = missing(base, dir, cat, thumbs);
        List<CompletableFuture<Void>> jobs = new ArrayList<>();
        for (String[] m : missing) {
            jobs.add(DOWNLOADS.submit(() -> FLIGHT.run(m[0], () -> send(Net.request(m[1], Net.Kind.BULK).GET().build()).thenApply(Net.Response::body)))
                    .thenAcceptAsync(b -> {
                        try {
                            if (NetBench.sha256(b).equals(m[0])) NetBench.write(dir.resolve("assets").resolve(m[0] + ".vibey"), b);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }, IO));
        }
        CompletableFuture.allOf(jobs.toArray(new CompletableFuture[0])).join();
        NetBench.mark("catalogReady");
        // wardrobe: thumbnails from disk or ≤ 4 parallel downloads
        List<CompletableFuture<Void>> t = new ArrayList<>();
        for (String url : thumbs) {
            Path f = dir.resolve("thumbs").resolve(NetBench.sha256(url.getBytes(StandardCharsets.UTF_8)) + ".png");
            if (Files.isRegularFile(f)) {
                Files.readAllBytes(f);
                continue;
            }
            t.add(THUMBS.submit(() -> send(Net.request(url, Net.Kind.BULK).GET().build())).thenAcceptAsync(r -> {
                try {
                    if (r.status() == 200) NetBench.write(f, r.body());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, IO));
        }
        CompletableFuture.allOf(t.toArray(new CompletableFuture[0])).join();
        NetBench.mark("thumbsReady");
        CompletableFuture.allOf(meta, ann).join();
        NetBench.mark("done");
    }

    /** Assets of the catalog that are not on disk yet (hash, url); fills the thumbnail URLs. */
    static List<String[]> missing(String base, Path dir, JsonObject cat, List<String> thumbs) throws Exception {
        List<String[]> out = new ArrayList<>();
        for (JsonElement el : cat.getAsJsonArray("items")) {
            JsonObject o = el.getAsJsonObject();
            JsonObject asset = o.getAsJsonObject("asset");
            String hash = asset.get("hash").getAsString();
            String url = asset.has("url") ? asset.get("url").getAsString() : "/api/v1/cosmetics/assets/" + hash;
            if (o.get("thumbnailUrl") instanceof com.google.gson.JsonPrimitive p) thumbs.add(NetBench.Before.abs(base, p.getAsString()));
            Path f = dir.resolve("assets").resolve(hash + ".vibey");
            if (Files.isRegularFile(f) && NetBench.sha256(Files.readAllBytes(f)).equals(hash)) continue;
            out.add(new String[]{hash, NetBench.Before.abs(base, url)});
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------ auth

    static volatile String access;
    static volatile Bootstrap boot;

    static final List<String> BOOTSTRAP_PATHS = List.of(
            "/api/v1/friends/requests", "/api/v1/blocks", "/api/v1/chats",
            "/api/v1/invites?direction=incoming&pendingOnly=true", "/api/v1/notifications?limit=50",
            "/api/v1/hosting/friends", "/api/v1/hosting/sessions",
            "/api/v1/cosmetics/me", "/api/v1/cosmetics/loadout/signed");

    static Rest rest;

    /** Rest.call with a timing record per call. */
    static CompletableFuture<JsonElement> call(Rest.Call c) {
        double s = NetBench.ms();
        return rest.call(c).whenComplete((r, e) -> NetBench.rec(c.method() + " " + NetBench.label(c.path()) + (e != null ? " FAILED " + Net.cause(e).getMessage() : ""),
                s, NetBench.ms(), NetBench.ms(), e == null ? 200 : ((Net.cause(e) instanceof ApiException a) ? a.status : 0), r == null ? 0 : r.toString().length(), "-"));
    }

    /** BackendClient.request: GETs consult the bootstrap first. */
    static CompletableFuture<JsonElement> request(String method, String path, JsonElement body) {
        if (method.equals("GET") && boot != null) {
            CompletableFuture<JsonElement> f = boot.take(path);
            if (f != null) return f.thenCompose(v -> v != null ? CompletableFuture.completedFuture(v) : call(Rest.Call.of(method, path, body, true)));
        }
        return call(Rest.Call.of(method, path, body, true));
    }

    public static void authFlow(String base, Path dir, int busy, boolean warm) throws Exception {
        Net.userAgent("vibey-client/0.3.1");
        rest = new Rest(() -> base, new Rest.Auth() {
            @Override
            public CompletableFuture<String> token() {
                return CompletableFuture.completedFuture(access);
            }

            @Override
            public CompletableFuture<String> refreshed(String failed) {
                return CompletableFuture.failedFuture(new ApiException(401, "unauthorized", "harness"));
            }

            @Override
            public void banned(ApiException e) {
            }
        });
        access = NetBench.Auth.login(dir, warm, (m, p, b) -> call(Rest.Call.of(m, p, b, false)).join());
        NetBench.mark("loggedIn");
        JsonObject ready = NetBench.Auth.gateway(Net.client(), base, access);
        NetBench.mark("ready");
        JsonObject features = ready.getAsJsonObject("features");
        if (features != null && features.has(Bootstrap.FEATURE) && features.get(Bootstrap.FEATURE).getAsBoolean()) {
            boot = new Bootstrap(BOOTSTRAP_PATHS, call(Rest.Call.of("POST", Bootstrap.PATH, Bootstrap.body(BOOTSTRAP_PATHS), true).idempotent(true)), 20_000);
        }
        List<CompletableFuture<?>> all = new ArrayList<>();
        for (String p : NetBench.Auth.SOCIAL_PATHS) all.add(request("GET", p, null));
        all.add(request("GET", "/api/v1/hosting/friends", null));
        all.add(request("GET", "/api/v1/hosting/sessions", null));
        // SocialService publicGet("/api/v1/meta") and LoadoutSync.fetchMeta → one shared Meta request
        double ms0 = NetBench.ms();
        all.add(Meta.get(base, 5 * 60_000L).whenComplete((o, e) -> NetBench.rec("GET /api/v1/meta", ms0, NetBench.ms(), NetBench.ms(), e == null ? 200 : 0, 0, "-")));
        all.add(Meta.get(base, 5 * 60_000L));
        CompletableFuture<JsonElement> me = request("GET", "/api/v1/cosmetics/me", null);
        all.add(me.thenCompose(x -> {
            NetBench.mark("cosmeticsMe");
            JsonObject lookup = new JsonObject();
            JsonArray ids = new JsonArray();
            ids.add(ready.getAsJsonObject("user").get("id").getAsString());
            lookup.add("userIds", ids);
            return CompletableFuture.allOf(request("GET", "/api/v1/cosmetics/loadout/signed", null),
                    call(Rest.Call.of("POST", "/api/v1/cosmetics/lookup", lookup, true)));
        }));
        CompletableFuture.allOf(all.toArray(new CompletableFuture[0])).join();
        NetBench.mark("allLoaded");
    }
}
