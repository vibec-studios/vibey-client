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

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static volatile HttpClient http;
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
    // Over-the-air cosmetics (VIBE-API-2 §4): the catalog is polled every ~10 minutes (jittered), on world join and
    // when the gateway says `catalog.updated`. With ETag a poll is one small request (304 = nothing to do). Changed
    // items reuse their parsed bundle when the asset hash is the same; new/changed assets are downloaded in parallel
    // on worker threads and swapped in all at once on the client thread (the old version stays visible until then).
    // Textures of the new bundles upload lazily, a few per frame (GpuBundles). Equipped ids stay equipped.

    private static final long POLL_MS = 10 * 60_000, POLL_JITTER_MS = 3 * 60_000, MIN_GAP_MS = 15_000;
    private static final int PARALLEL_DOWNLOADS = 4;
    /**
     * Own small pool for catalog loads and asset downloads: a load waits for its downloads, so they must not share the
     * general worker pool (which the HTTP client also needs to complete responses).
     */
    private static final java.util.concurrent.ExecutorService CATALOG_IO = java.util.concurrent.Executors.newFixedThreadPool(PARALLEL_DOWNLOADS + 1, r -> {
        Thread t = new Thread(r, "VibeyClient-Catalog");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });
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

    public static String catalogVersion() {
        return catalogVersion;
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
        nextCatalog = now + POLL_MS + java.util.concurrent.ThreadLocalRandom.current().nextLong(POLL_JITTER_MS);
        int gen = GENERATION.get();
        CATALOG_IO.execute(() -> {
            try {
                loadCatalog(gen);
            } catch (Exception e) {
                Vibey.LOG.info("Cosmetics catalog not available: {}", e.toString());
                if (!catalogLoaded) loadCachedCatalog(gen);
                // try again sooner than the normal interval
                nextCatalog = Math.min(nextCatalog, System.currentTimeMillis() + 60_000
                        + java.util.concurrent.ThreadLocalRandom.current().nextLong(30_000));
            } finally {
                CATALOG_RUNNING.set(false);
            }
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

    /** Online features switched off: forget catalog items (they stay cached on disk). */
    public static void dropCatalog() {
        GENERATION.incrementAndGet();
        nextCatalog = 0;
        catalogLoaded = false;
        lastIds = null;
        AvatarItems.putAll(ItemSource.CATALOG, Map.of(), true);
    }

    /**
     * Whether the catalog items are known (client thread). Until then an item that is only builtin cannot be told
     * apart from an online one, so nothing that depends on "is this a catalog item" (loadout upload) may run.
     */
    public static boolean catalogLoaded() {
        return catalogLoaded;
    }

    private static HttpClient http() {
        if (http == null) {
            http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL)
                    .executor(Tasks.executor()).build();
        }
        return http;
    }

    private static String base() {
        String b = GlobalSettings.backend();
        return b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
    }

    static String absolute(String url) {
        if (url == null || url.isEmpty()) return null;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        return base() + (url.startsWith("/") ? url : "/" + url);
    }

    private static void loadCatalog(int gen) throws Exception {
        Path dir = cacheDir();
        Files.createDirectories(dir.resolve("assets"));
        Path cached = dir.resolve("catalog.json"), etagFile = dir.resolve("catalog.etag");
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(base() + "/api/v1/cosmetics/catalog"))
                .timeout(Duration.ofSeconds(10)).header("Accept", "application/json").GET();
        if (Files.isRegularFile(cached) && Files.isRegularFile(etagFile)) rb.header("If-None-Match", Files.readString(etagFile).trim());
        HttpResponse<byte[]> res = http().send(rb.build(), HttpResponse.BodyHandlers.ofByteArray());
        byte[] body;
        if (res.statusCode() == 304) {
            if (catalogLoaded) return; // nothing changed and the registry already has it
            body = Files.readAllBytes(cached);
        } else if (res.statusCode() == 200) {
            body = res.body();
            write(cached, body);
            String etag = res.headers().firstValue("ETag").orElse(null);
            if (etag != null) write(etagFile, etag.getBytes(StandardCharsets.UTF_8));
            else Files.deleteIfExists(etagFile);
        } else {
            throw new IllegalStateException("HTTP " + res.statusCode());
        }
        apply(JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject(), true, gen);
    }

    private static void loadCachedCatalog(int gen) {
        try {
            Path cached = cacheDir().resolve("catalog.json");
            if (!Files.isRegularFile(cached)) return;
            apply(JsonParser.parseString(Files.readString(cached)).getAsJsonObject(), false, gen);
        } catch (Exception e) {
            Vibey.LOG.debug("Cached catalog unreadable: {}", e.toString());
        }
    }

    /** One catalog entry as read from the JSON (tolerant: unknown fields are ignored). */
    private record Entry(String id, String hash, String url, JsonObject json, String minClient) {}

    /** Worker thread: turns the catalog JSON into registry items and hands them to the client thread. */
    static void apply(JsonObject catalog, boolean download, int gen) {
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
        List<Entry> missing = new java.util.ArrayList<>();
        for (Entry e : entries) {
            try {
                if (e.minClient != null && !ClientVersion.satisfies(e.minClient)) {
                    AvatarItem stub = stub(e, e.minClient);
                    if (stub != null) items.put(e.id, stub);
                    continue;
                }
                AvatarItem old = AvatarItems.get(e.id, ItemSource.CATALOG);
                Bundle b = old != null && old.bundle() != null && e.hash.equals(old.hash()) ? old.bundle() : null;
                if (b == null) b = fromDisk(e);
                if (b == null) {
                    if (download && !FAILED.contains(e.hash)) missing.add(e);
                    else if (old != null && old.bundle() != null) items.put(e.id, old); // keep the old version for now
                    continue;
                }
                items.put(e.id, item(e, b));
            } catch (dev.henny.vibeyclient.avatar.bundle.NeedsUpdateException nu) {
                AvatarItem stub = stub(e, nu.required);
                if (stub != null) items.put(e.id, stub);
            } catch (Exception ex) {
                if (FAILED.add(e.hash)) Vibey.LOG.warn("Catalog item {} skipped: {}", e.id, ex.getMessage());
            }
        }

        // 2. new or changed assets: download in parallel (worker threads), verify, cache, parse
        if (!missing.isEmpty()) {
            java.util.concurrent.Semaphore slots = new java.util.concurrent.Semaphore(PARALLEL_DOWNLOADS);
            List<java.util.concurrent.CompletableFuture<Void>> jobs = new java.util.ArrayList<>();
            for (Entry e : missing) {
                jobs.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                    slots.acquireUninterruptibly();
                    try {
                        Bundle b = downloadAsset(e);
                        if (b != null) synchronized (items) { items.put(e.id, item(e, b)); }
                    } catch (dev.henny.vibeyclient.avatar.bundle.NeedsUpdateException nu) {
                        AvatarItem stub = stub(e, nu.required);
                        if (stub != null) synchronized (items) { items.put(e.id, stub); }
                    } catch (Exception ex) {
                        if (FAILED.add(e.hash)) Vibey.LOG.warn("Catalog item {} skipped: {}", e.id, ex.toString());
                    } finally {
                        slots.release();
                    }
                    // a failed download keeps the previous version of the item (if any) until the next poll
                    AvatarItem old = AvatarItems.get(e.id, ItemSource.CATALOG);
                    synchronized (items) {
                        if (!items.containsKey(e.id) && old != null && old.bundle() != null) items.put(e.id, old);
                    }
                }, CATALOG_IO));
            }
            java.util.concurrent.CompletableFuture.allOf(jobs.toArray(new java.util.concurrent.CompletableFuture[0])).join();
            Vibey.LOG.info("Cosmetics catalog: downloaded {} new/changed asset(s)", missing.size());
        }
        Vibey.LOG.info("Cosmetics catalog: {} items", items.size());

        // 3. swap in on the client thread
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
            boolean first = lastIds == null;
            java.util.Set<String> fresh = new java.util.HashSet<>(items.keySet());
            if (!first) fresh.removeAll(lastIds);
            lastIds = java.util.Set.copyOf(items.keySet());
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

    private static Bundle downloadAsset(Entry e) throws Exception {
        String url = absolute(e.url);
        HttpResponse<byte[]> r = http().send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (r.statusCode() != 200) throw new IllegalStateException("asset HTTP " + r.statusCode());
        byte[] bytes = r.body();
        if (!BundleParser.sha256(bytes).equals(e.hash)) throw new IllegalStateException("asset has a wrong hash");
        Files.createDirectories(cacheDir().resolve("assets"));
        write(cacheDir().resolve("assets").resolve(e.hash + ".vibey"), bytes);
        return parse(e, bytes);
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

    /** Downloads (or reads from cache) a catalog thumbnail; worker thread. */
    static byte[] thumbnail(String url) throws Exception {
        String key = BundleParser.sha256(url.getBytes(StandardCharsets.UTF_8));
        Path file = cacheDir().resolve("thumbs").resolve(key + ".png");
        if (Files.isRegularFile(file)) return Files.readAllBytes(file);
        if (!GlobalSettings.online()) return null;
        HttpResponse<byte[]> r = http().send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (r.statusCode() != 200 || r.body().length > 1 << 20) return null;
        Files.createDirectories(file.getParent());
        write(file, r.body());
        return r.body();
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
