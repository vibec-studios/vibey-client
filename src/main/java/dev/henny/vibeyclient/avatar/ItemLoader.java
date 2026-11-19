package dev.henny.vibeyclient.avatar;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.bundle.BundleParser;
import dev.henny.vibeyclient.avatar.fx.EmbeddedSounds;
import dev.henny.vibeyclient.avatar.render.GpuBundles;

import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.VibeBackend;
import dev.henny.vibeyclient.backend.net.HttpCache;
import dev.henny.vibeyclient.backend.net.Limiter;
import dev.henny.vibeyclient.backend.net.Net;
import dev.henny.vibeyclient.backend.net.SingleFlight;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fills {@link AvatarItems}: the builtin bundles from the jar ({@code assets/vibeyclient/builtin}) and the backend
 * catalog ({@code GET /api/v1/cosmetics/catalog} with ETag, assets by SHA-256, cached in
 * {@code <gameDir>/vibeyclient/cache}). Plain unauthenticated HTTP, only while online features are on. Parsing runs
 * on worker threads; the registry is updated on the client thread.
 */
public final class ItemLoader {
    private ItemLoader() {}

    public static final String BUILTIN = "/assets/vibeyclient/builtin/";
    private static final AtomicBoolean CATALOG_RUNNING = new AtomicBoolean();
    private static volatile long lastCatalog;
    /** True while catalog items are in the registry (fresh or from the disk cache); client thread. */
    private static boolean catalogLoaded;
    /** Catalog item id → thumbnail URL (absolute). */
    private static final Map<String, String> THUMBS = new java.util.concurrent.ConcurrentHashMap<>();

    public static Path cacheDir() {
        return Vibey.platform().gameDir().resolve("vibeyclient").resolve("cache");
    }

    /** Registers a parsed bundle in the registry (client thread), replacing the GPU side of an older version. */
    public static void register(AvatarItem item) {
        AvatarItem old = AvatarItems.get(item.id(), item.source());
        AvatarItems.put(item);
        if (old != null && old.bundle() != null && old.bundle() != item.bundle()) {
            GpuBundles.evict(old.bundle());
            EmbeddedSounds.evict(old.bundle());
        }
        if (item.bundle() != null && item.isEmote()) AvatarStates.reloadEmote(item.id());
    }

    /** Worker thread: compile the mesh. (Sounds are decoded on first use: LWJGL must not be touched during startup.) */
    static void prepare(Bundle b) {
        GpuBundles.prepare(b);
    }

    // ============================================================================================ builtin

    private static volatile Map<String, AvatarItem> pendingBuiltin;

    /** Client thread, every tick: registers what the loaders finished. */
    public static void tick() {
        Map<String, AvatarItem> b = pendingBuiltin;
        if (b != null) {
            pendingBuiltin = null;
            AvatarItems.putAll(ItemSource.BUILTIN, b, true);
        }
    }

    public static void loadBuiltin() {
        Tasks.run(() -> {
            JsonObject index;
            try (InputStream in = ItemLoader.class.getResourceAsStream(BUILTIN + "index.json")) {
                if (in == null) {
                    Vibey.LOG.warn("No builtin cosmetics in the jar");
                    return;
                }
                index = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (Exception e) {
                Vibey.LOG.error("Builtin cosmetics index unreadable", e);
                return;
            }
            Map<String, AvatarItem> items = new HashMap<>();
            for (JsonElement el : index.getAsJsonArray("items")) {
                JsonObject o = el.getAsJsonObject();
                String file = o.get("file").getAsString();
                try (InputStream in = ItemLoader.class.getResourceAsStream(BUILTIN + file)) {
                    if (in == null) continue;
                    Bundle b = BundleParser.parse(in.readAllBytes());
                    prepare(b);
                    String thumb = o.has("thumbnail") ? BUILTIN + o.get("thumbnail").getAsString() : null;
                    int sort = o.has("sort") ? o.get("sort").getAsInt() : 1000;
                    items.put(b.id, AvatarItem.of(b, ItemSource.BUILTIN, sort, thumb, "default", AvatarItems.nextRevision()));
                } catch (Exception e) {
                    Vibey.LOG.error("Builtin bundle {} is invalid: {}", file, e.getMessage());
                }
            }
            Vibey.LOG.info("Loaded {} builtin cosmetics/emotes", items.size());
            // NeoForge loads mods before the game exists: hand over in tick() instead of Tasks.onMain
            pendingBuiltin = items;
        });
    }

    // ============================================================================================ catalog (OTA)
    //
    // Over-the-air cosmetics (VIBE-API-2 §4), see docs/NETWORK.md. At start the cached catalog is shown right away
    // (no waiting for the network), then revalidated with its ETag (304 = a few hundred bytes). After that the gateway
    // pushes `catalog.updated` and `ready` carries `catalogVersion`, so while it is connected the poll is only a
    // safety net (every ~60 min); without a gateway (not logged in) it stays at ~10 min, plus on world join. Changed
    // items reuse their parsed bundle when the asset hash is the same; new/changed assets are downloaded in parallel
    // (at most 4 at a time, each hash once) and swapped in all at once on the client thread (the old version stays
    // visible until then). Textures of the new bundles upload lazily, a few per frame (GpuBundles). Equipped ids stay
    // equipped. Network futures never block a thread; hashing, parsing and disk I/O run on CATALOG_IO.

    private static final long POLL_MS = 10 * 60_000, POLL_JITTER_MS = 3 * 60_000, MIN_GAP_MS = 15_000;
    /** Poll interval while the gateway is connected (it pushes `catalog.updated`): only a safety net. */
    private static final long PUSH_POLL_MS = 60 * 60_000, PUSH_POLL_JITTER_MS = 15 * 60_000;
    private static final int PARALLEL_DOWNLOADS = 4;
    /**
     * Hashing, parsing and disk I/O of the catalog and its assets (never on the HTTP client's threads, never waiting
     * for other jobs). Threads end after 30 s without work.
     */
    private static final java.util.concurrent.ThreadPoolExecutor CATALOG_IO = new java.util.concurrent.ThreadPoolExecutor(2, 2,
            30, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<>(), r -> {
        Thread t = new Thread(r, "VibeyClient-Catalog");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    static {
        CATALOG_IO.allowCoreThreadTimeOut(true);
    }

    private static final Limiter DOWNLOADS = new Limiter(PARALLEL_DOWNLOADS);
    private static final Limiter THUMB_DOWNLOADS = new Limiter(PARALLEL_DOWNLOADS);
    private static final SingleFlight<String, byte[]> ASSET_FLIGHT = new SingleFlight<>();
    private static final SingleFlight<String, byte[]> THUMB_FLIGHT = new SingleFlight<>();
    /**
     * Bundles of the last apply by asset hash (worker side: the registry is only updated later on the client thread,
     * so the revalidation right after the disk copy would otherwise parse everything twice).
     */
    private static volatile Map<String, Bundle> parsed = Map.of();
    /** What the registry shows (or is about to): generation + ETag, and whether no asset was missing. */
    private record Applied(int gen, String etag, boolean complete) {}
    private static volatile Applied applied;
    /** Next time the catalog is fetched (epoch ms); 0 = as soon as possible. */
    private static volatile long nextCatalog;
    /** Bumped when the catalog is dropped (online off, other backend): loads started before are discarded. */
    private static final java.util.concurrent.atomic.AtomicInteger GENERATION = new java.util.concurrent.atomic.AtomicInteger();
    /** Hashes (or "id@hash") that failed to load: logged once, not retried until the catalog changes them. */
    private static final java.util.Set<String> FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** Catalog ids after the last apply, null before the first one (for "new cosmetics" toasts). */
    private static volatile java.util.Set<String> lastIds;
    /** Called on the client thread with the ids of catalog items that appeared since the last load (not the first). */
    public static volatile java.util.function.Consumer<java.util.Set<String>> onNewItems = ids -> {};
    /** The catalog's version string of the last apply (compared with the gateway's ready.catalogVersion). */
    private static volatile String catalogVersion;
    /** ready.catalogVersion that arrived while a load was running ("" = none): compared once the load is done. */
    private static String pendingReadyVersion;

    public static String catalogVersion() {
        return catalogVersion;
    }

    /** The gateway is connected, so `catalog.updated` arrives by push. */
    public static boolean catalogPushed() {
        return Services.get(VibeBackend.class).map(VibeBackend::isReady).orElse(false);
    }

    /**
     * Polls the catalog if it is due ({@code force}: right away, but never more than every 15 s). Called every tick
     * by the cosmetics module; no-op while online features are off.
     */
    public static void refreshCatalog(boolean force) {
        if (!GlobalSettings.online()) return;
        long now = System.currentTimeMillis();
        if (force) nextCatalog = Math.min(nextCatalog, Math.max(now, lastCatalog + MIN_GAP_MS));
        if (now < nextCatalog) return;
        if (!CATALOG_RUNNING.compareAndSet(false, true)) return;
        lastCatalog = now;
        boolean pushed = catalogPushed();
        nextCatalog = now + (pushed ? PUSH_POLL_MS : POLL_MS)
                + java.util.concurrent.ThreadLocalRandom.current().nextLong(pushed ? PUSH_POLL_JITTER_MS : POLL_JITTER_MS);
        int gen = GENERATION.get();
        String url = base() + "/api/v1/cosmetics/catalog";
        CompletableFuture.supplyAsync(() -> loadCatalog(gen, url), CATALOG_IO).thenCompose(f -> f).whenComplete((v, e) -> {
            if (e != null) {
                Vibey.LOG.info("Cosmetics catalog not available: {}", Net.cause(e).toString());
                // try again sooner than the normal interval
                nextCatalog = Math.min(nextCatalog, System.currentTimeMillis() + 60_000
                        + java.util.concurrent.ThreadLocalRandom.current().nextLong(30_000));
            }
            CATALOG_RUNNING.set(false);
            Tasks.onMain(() -> { // after the swap (same queue)
                // the network answered (or failed, then the disk copy is all there is – as before)
                if (catalogLoaded && gen == GENERATION.get()) catalogConfirmed = true;
                checkReadyVersion();
            });
        });
    }

    /**
     * Asks for a catalog refresh soon, spread over {@code jitterMs} so thousands of clients reacting to the same
     * `catalog.updated` event do not hit the backend in the same second.
     */
    public static void requestCatalog(long jitterMs) {
        long at = System.currentTimeMillis() + (jitterMs > 0 ? java.util.concurrent.ThreadLocalRandom.current().nextLong(jitterMs) : 0);
        nextCatalog = Math.min(nextCatalog, Math.max(at, lastCatalog + MIN_GAP_MS));
    }

    /**
     * Client thread: the gateway's {@code ready} names the catalog version (null = unknown). A different version
     * than the one shown → refresh soon; while a load is running (start: ready usually arrives during the first
     * load) the comparison waits for it instead of asking for a second load right away.
     */
    public static void readyVersion(String v) {
        if (CATALOG_RUNNING.get()) {
            pendingReadyVersion = v == null ? "" : v;
            return;
        }
        if (v == null || !v.equals(catalogVersion)) requestCatalog(10_000);
    }

    private static void checkReadyVersion() {
        String v = pendingReadyVersion;
        if (v == null) return;
        pendingReadyVersion = null;
        if (v.isEmpty() || !v.equals(catalogVersion)) requestCatalog(10_000);
    }

    /** Online features switched off: forget catalog items (they stay cached on disk). */
    public static void dropCatalog() {
        GENERATION.incrementAndGet();
        nextCatalog = 0;
        catalogLoaded = false;
        catalogConfirmed = false;
        lastIds = null;
        applied = null;
        parsed = Map.of();
        AvatarItems.putAll(ItemSource.CATALOG, Map.of(), true);
    }

    /**
     * Whether the catalog items are known (client thread). Until then an item that is only builtin cannot be told
     * apart from an online one, so nothing that depends on "is this a catalog item" (loadout upload) may run.
     */
    public static boolean catalogLoaded() {
        return catalogLoaded;
    }

    /**
     * Client thread: the catalog shown is the one the network answered with (or the network failed and the disk copy
     * is all there is, as before). The disk copy shown at start before that is not enough for a loadout upload.
     */
    public static boolean catalogConfirmed() {
        return catalogConfirmed;
    }

    private static boolean catalogConfirmed;

    private static String base() {
        String b = GlobalSettings.backend();
        return b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
    }

    static String absolute(String url) {
        if (url == null || url.isEmpty()) return null;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        return base() + (url.startsWith("/") ? url : "/" + url);
    }

    /**
     * CATALOG_IO: shows the disk copy first (if nothing is shown for this generation yet) while the conditional
     * request is already on its way, then applies what the network says.
     */
    private static CompletableFuture<Void> loadCatalog(int gen, String url) {
        try {
            Files.createDirectories(cacheDir().resolve("assets"));
        } catch (java.io.IOException e) {
            return CompletableFuture.failedFuture(e);
        }
        HttpCache cache = new HttpCache(cacheDir());
        HttpCache.Entry cached = cache.load("catalog");
        String base = url.substring(0, url.length() - "/api/v1/cosmetics/catalog".length());
        CompletableFuture<HttpCache.Result> net = cache.fetch("catalog", url, cached, Net.Kind.API, CATALOG_IO).thenApply(r -> {
            writeCacheBase(base); // the backend confirmed (304) or replaced (200) the disk copy: it is this backend's
            return r;
        });
        Applied a = applied;
        CompletableFuture<Void> shown = CompletableFuture.completedFuture(null);
        // only a copy of this very backend's catalog (another backend, e.g. after switching, has other items)
        if (cached != null && (a == null || a.gen != gen) && base.equals(readCacheBase())) {
            try {
                JsonObject cat = JsonParser.parseString(new String(cached.body(), StandardCharsets.UTF_8)).getAsJsonObject();
                shown = apply(cat, false, gen, true).thenAccept(complete -> applied = new Applied(gen, cached.etag(), complete));
            } catch (RuntimeException e) {
                Vibey.LOG.debug("Cached catalog unreadable: {}", e.toString());
            }
        }
        // the disk apply must be done before the network result is applied (never two applies at once)
        return shown.handle((x, e) -> null).thenCompose(x -> net).thenComposeAsync(res -> {
            Applied now = applied;
            if (res.notModified() && now != null && now.gen == gen && now.complete && java.util.Objects.equals(now.etag, res.etag())) {
                return CompletableFuture.completedFuture(null); // nothing changed and the registry already has it
            }
            JsonObject cat = JsonParser.parseString(new String(res.body(), StandardCharsets.UTF_8)).getAsJsonObject();
            return apply(cat, true, gen, false).thenAccept(complete -> applied = new Applied(gen, res.etag(), complete));
        }, CATALOG_IO);
    }

    /** {@code catalog.base}: the backend the disk copy of the catalog belongs to. */
    private static String readCacheBase() {
        try {
            Path f = cacheDir().resolve("catalog.base");
            return Files.isRegularFile(f) ? Files.readString(f, StandardCharsets.UTF_8).trim() : null;
        } catch (java.io.IOException e) {
            return null;
        }
    }

    private static void writeCacheBase(String base) {
        try {
            if (!base.equals(readCacheBase())) write(cacheDir().resolve("catalog.base"), base.getBytes(StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            Vibey.LOG.debug("catalog.base not written: {}", e.toString());
        }
    }

    /** One catalog entry as read from the JSON (tolerant: unknown fields are ignored). */
    private record Entry(String id, String hash, String url, JsonObject json, String minClient) {}

    /**
     * CATALOG_IO: turns the catalog JSON into registry items and hands them to the client thread. Completes (true =
     * every asset was there) once the swap is queued on the client thread. {@code fromDisk}: the cached copy shown
     * before the network answered – it never counts for "new cosmetics" (only network catalogs are compared, as
     * before).
     */
    static CompletableFuture<Boolean> apply(JsonObject catalog, boolean download, int gen, boolean fromDisk) {
        JsonArray list = catalog.get("items") instanceof JsonArray a ? a : new JsonArray();
        String version = catalog.get("version") instanceof com.google.gson.JsonPrimitive v ? v.getAsString() : null;
        List<Entry> entries = new java.util.ArrayList<>();
        for (JsonElement el : list) {
            try {
                if (!(el instanceof JsonObject o) || !(o.get("id") instanceof com.google.gson.JsonPrimitive)) continue;
                String id = o.get("id").getAsString();
                JsonObject asset = o.get("asset") instanceof JsonObject x ? x : null;
                String hash = asset != null && asset.get("hash") instanceof com.google.gson.JsonPrimitive h ? h.getAsString().toLowerCase(java.util.Locale.ROOT) : "";
                if (!hash.matches("[0-9a-f]{64}")) {
                    once(id + "@nohash", "Catalog item {} has no valid asset hash, skipped", id);
                    continue;
                }
                String url = asset.get("url") instanceof com.google.gson.JsonPrimitive u ? u.getAsString() : "/api/v1/cosmetics/assets/" + hash;
                String min = o.get("minClientVersion") instanceof com.google.gson.JsonPrimitive m ? m.getAsString() : null;
                entries.add(new Entry(id, hash, url, o, min));
            } catch (RuntimeException e) {
                once("entry:" + el.hashCode(), "Catalog entry skipped: {}", e.toString());
            }
        }

        // 1. what we already have (same hash: reuse the parsed bundle) or have on disk
        Map<String, AvatarItem> items = new HashMap<>();
        Map<String, Bundle> bundles = new java.util.concurrent.ConcurrentHashMap<>();
        Map<String, Bundle> before = parsed;
        List<Entry> missing = new java.util.ArrayList<>();
        boolean skipped = false;
        for (Entry e : entries) {
            try {
                if (e.minClient != null && !ClientVersion.satisfies(e.minClient)) {
                    AvatarItem stub = stub(e, e.minClient);
                    if (stub != null) items.put(e.id, stub);
                    continue;
                }
                AvatarItem old = AvatarItems.get(e.id, ItemSource.CATALOG);
                Bundle b = old != null && old.bundle() != null && e.hash.equals(old.hash()) ? old.bundle() : null;
                if (b == null) b = before.get(e.hash);
                if (b == null) b = fromDisk(e);
                if (b == null) {
                    if (download && !FAILED.contains(e.hash)) missing.add(e);
                    else {
                        skipped = true;
                        if (old != null && old.bundle() != null) items.put(e.id, old); // keep the old version for now
                    }
                    continue;
                }
                bundles.put(e.hash, b);
                items.put(e.id, item(e, b));
            } catch (dev.henny.vibeyclient.avatar.bundle.NeedsUpdateException nu) {
                AvatarItem stub = stub(e, nu.required);
                if (stub != null) items.put(e.id, stub);
            } catch (Exception ex) {
                if (FAILED.add(e.hash)) Vibey.LOG.warn("Catalog item {} skipped: {}", e.id, ex.getMessage());
            }
        }

        // 2. new or changed assets: download in parallel (no thread waits), verify, cache, parse on CATALOG_IO
        List<CompletableFuture<Void>> jobs = new java.util.ArrayList<>();
        for (Entry e : missing) {
            jobs.add(DOWNLOADS.submit(() -> ASSET_FLIGHT.run(e.hash, () -> fetch(absolute(e.url), Net.Kind.BULK)))
                    .thenApplyAsync(bytes -> verifyAndParse(e, bytes), CATALOG_IO)
                    .handle((b, ex) -> {
                        if (ex == null) {
                            bundles.put(e.hash, b);
                            synchronized (items) {
                                items.put(e.id, item(e, b));
                            }
                        } else if (Net.cause(ex) instanceof dev.henny.vibeyclient.avatar.bundle.NeedsUpdateException nu) {
                            AvatarItem stub = stub(e, nu.required);
                            if (stub != null) synchronized (items) { items.put(e.id, stub); }
                        } else if (FAILED.add(e.hash)) {
                            Vibey.LOG.warn("Catalog item {} skipped: {}", e.id, Net.cause(ex).toString());
                        }
                        // a failed download keeps the previous version of the item (if any) until the next poll
                        AvatarItem old = AvatarItems.get(e.id, ItemSource.CATALOG);
                        synchronized (items) {
                            if (!items.containsKey(e.id) && old != null && old.bundle() != null) items.put(e.id, old);
                        }
                        return null;
                    }));
        }
        boolean complete = !skipped;
        return CompletableFuture.allOf(jobs.toArray(new CompletableFuture[0])).thenApply(x -> {
            if (!missing.isEmpty()) Vibey.LOG.info("Cosmetics catalog: downloaded {} new/changed asset(s)", missing.size());
            Vibey.LOG.info("Cosmetics catalog: {} items", items.size());
            if (gen == GENERATION.get()) parsed = Map.copyOf(bundles);
            swap(items, version, gen, fromDisk);
            return complete;
        });
    }

    /** 3. swap in on the client thread. */
    private static void swap(Map<String, AvatarItem> items, String version, int gen, boolean fromDisk) {
        Tasks.onMain(() -> {
            if (!GlobalSettings.online() || gen != GENERATION.get()) return; // dropped meanwhile (other backend / offline)
            for (AvatarItem old : AvatarItems.all()) {
                if (old.source() != ItemSource.CATALOG || old.bundle() == null) continue;
                AvatarItem n = items.get(old.id());
                if (n == null || n.bundle() != old.bundle()) {
                    GpuBundles.evict(old.bundle());
                    EmbeddedSounds.evict(old.bundle());
                }
            }
            boolean first = lastIds == null || fromDisk;
            java.util.Set<String> fresh = new java.util.HashSet<>(items.keySet());
            if (!first) fresh.removeAll(lastIds);
            if (!fromDisk) lastIds = java.util.Set.copyOf(items.keySet());
            AvatarItems.putAll(ItemSource.CATALOG, items, true);
            for (AvatarItem i : items.values()) if (i.bundle() != null && i.isEmote()) AvatarStates.reloadEmote(i.id());
            catalogLoaded = true;
            catalogVersion = version;
            fresh.removeIf(id -> {
                AvatarItem i = items.get(id);
                return i == null || i.needsUpdate();
            });
            if (!first && !fresh.isEmpty()) {
                try {
                    onNewItems.accept(fresh);
                } catch (RuntimeException ex) {
                    Vibey.LOG.debug("onNewItems failed: {}", ex.toString());
                }
            }
        });
    }

    private static Bundle fromDisk(Entry e) throws Exception {
        Path file = cacheDir().resolve("assets").resolve(e.hash + ".vibey");
        if (!Files.isRegularFile(file)) return null;
        byte[] bytes = Files.readAllBytes(file);
        if (!BundleParser.sha256(bytes).equals(e.hash)) return null;
        return parse(e, bytes);
    }

    /** GET of an asset or thumbnail: the bytes of a 200, else a failed future. */
    private static CompletableFuture<byte[]> fetch(String url, Net.Kind kind) {
        return Net.send(Net.request(url, kind).GET().build()).thenApply(r -> {
            if (r.status() != 200) throw new java.util.concurrent.CompletionException(new HttpCache.StatusException(r.status()));
            return r.body();
        });
    }

    /** CATALOG_IO: checks the hash, caches the asset on disk and parses it. */
    private static Bundle verifyAndParse(Entry e, byte[] bytes) {
        try {
            if (!BundleParser.sha256(bytes).equals(e.hash)) throw new IllegalStateException("asset has a wrong hash");
            Files.createDirectories(cacheDir().resolve("assets"));
            write(cacheDir().resolve("assets").resolve(e.hash + ".vibey"), bytes);
            return parse(e, bytes);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new java.util.concurrent.CompletionException(ex);
        }
    }

    private static Bundle parse(Entry e, byte[] bytes) {
        Bundle b = BundleParser.parse(bytes);
        if (!b.id.equals(e.id)) once(e.id + "@" + e.hash + "@id", "Catalog item {} contains bundle {}", e.id, b.id);
        if (!b.warnings.isEmpty()) once(e.hash + "@warn", "Catalog item {}: {}", e.id, String.join("; ", b.warnings));
        prepare(b);
        return b;
    }

    private static AvatarItem item(Entry e, Bundle b) {
        JsonObject o = e.json;
        String thumb = o.get("thumbnailUrl") instanceof com.google.gson.JsonPrimitive t ? absolute(t.getAsString()) : null;
        if (thumb != null) THUMBS.put(b.id, thumb);
        int sort = o.get("sort") instanceof com.google.gson.JsonPrimitive s && s.isNumber() ? s.getAsInt() : 1000;
        String availability = o.get("availability") instanceof com.google.gson.JsonPrimitive a ? a.getAsString() : "default";
        String rarity = o.get("rarity") instanceof com.google.gson.JsonPrimitive r ? r.getAsString() : null;
        return new AvatarItem(b.id, b.kind, b.slot, text(o.get("name"), b.name), text(o.get("description"), b.description),
                b.author, b.tags, rarity == null ? b.rarity : Rarity.of(rarity), ItemSource.CATALOG, sort, thumb, e.hash,
                availability, b, AvatarItems.nextRevision(), creator(o), null);
    }

    /** Metadata-only item for a bundle that needs a newer client (listed greyed out, never rendered). */
    private static AvatarItem stub(Entry e, String required) {
        JsonObject o = e.json;
        String kindS = o.get("kind") instanceof com.google.gson.JsonPrimitive k ? k.getAsString() : null;
        Bundle.Kind kind = "emote".equals(kindS) ? Bundle.Kind.EMOTE : "cosmetic".equals(kindS) ? Bundle.Kind.COSMETIC : null;
        Slot slot = o.get("slot") instanceof com.google.gson.JsonPrimitive s ? Slot.of(s.getAsString()) : null;
        if (kind == null || (kind == Bundle.Kind.COSMETIC && slot == null)) {
            once(e.id + "@kind", "Catalog item {} has a kind/slot this client does not know ({}/{}), skipped", e.id, kindS, o.get("slot"));
            return null;
        }
        once(e.id + "@" + e.hash + "@update", "Catalog item {} needs Vibey Client {} (this is {})", e.id,
                required == null ? "(newer format)" : required, dev.henny.vibeyclient.BuildInfo.VERSION);
        String thumb = o.get("thumbnailUrl") instanceof com.google.gson.JsonPrimitive t ? absolute(t.getAsString()) : null;
        int sort = o.get("sort") instanceof com.google.gson.JsonPrimitive s && s.isNumber() ? s.getAsInt() : 1000;
        String availability = o.get("availability") instanceof com.google.gson.JsonPrimitive a ? a.getAsString() : "default";
        String rarity = o.get("rarity") instanceof com.google.gson.JsonPrimitive r ? r.getAsString() : null;
        List<String> tags = new java.util.ArrayList<>();
        if (o.get("tags") instanceof JsonArray ta) for (JsonElement t : ta) if (t.isJsonPrimitive()) tags.add(t.getAsString());
        String author = o.get("author") instanceof com.google.gson.JsonPrimitive au ? au.getAsString() : "";
        return new AvatarItem(e.id, kind, kind == Bundle.Kind.EMOTE ? null : slot, text(o.get("name"), new Bundle.Text(e.id, null)),
                text(o.get("description"), Bundle.Text.EMPTY), author, tags, Rarity.of(rarity), ItemSource.CATALOG, sort, thumb,
                e.hash, availability, null, AvatarItems.nextRevision(), creator(o), required == null ? "999.0.0" : required);
    }

    private static String creator(JsonObject o) {
        if (!(o.get("creator") instanceof JsonObject c)) return null;
        if (c.get("uuid") instanceof com.google.gson.JsonPrimitive u && "00000000-0000-0000-0000-000000000000".equals(u.getAsString()))
            return AvatarItem.DELETED_CREATOR;
        return c.get("name") instanceof com.google.gson.JsonPrimitive n ? n.getAsString() : null;
    }

    private static final java.util.Set<String> LOGGED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Logs a message once per key (catalog polls repeat every few minutes). */
    private static void once(String key, String msg, Object... args) {
        if (LOGGED.size() < 4096 && LOGGED.add(key)) Vibey.LOG.info(msg, args);
    }

    private static Bundle.Text text(JsonElement e, Bundle.Text fallback) {
        if (!(e instanceof JsonObject o)) return fallback;
        String en = o.has("en") && !o.get("en").isJsonNull() ? o.get("en").getAsString() : fallback.en();
        String de = o.has("de") && !o.get("de").isJsonNull() ? o.get("de").getAsString() : fallback.de();
        return new Bundle.Text(en, de);
    }

    /**
     * A catalog thumbnail: from the disk cache (read on the calling thread), else downloaded – at most 4 at a time,
     * each URL once – and cached. Completes with null when there is none to show.
     */
    static CompletableFuture<byte[]> thumbnail(String url) {
        try {
            String key = BundleParser.sha256(url.getBytes(StandardCharsets.UTF_8));
            Path file = cacheDir().resolve("thumbs").resolve(key + ".png");
            if (Files.isRegularFile(file)) return CompletableFuture.completedFuture(Files.readAllBytes(file));
            if (!GlobalSettings.online()) return CompletableFuture.completedFuture(null);
            return THUMB_FLIGHT.run(url, () -> THUMB_DOWNLOADS.submit(() -> fetch(url, Net.Kind.BULK))).thenApplyAsync(body -> {
                if (body.length > 1 << 20) return null;
                try {
                    Files.createDirectories(file.getParent());
                    write(file, body);
                } catch (java.io.IOException e) {
                    Vibey.LOG.debug("Thumbnail not cached: {}", e.toString());
                }
                return body;
            }, CATALOG_IO);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static void write(Path file, byte[] bytes) throws java.io.IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, bytes);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    @SuppressWarnings("unused")
    private static List<String> unused() {
        return List.of();
    }
}
