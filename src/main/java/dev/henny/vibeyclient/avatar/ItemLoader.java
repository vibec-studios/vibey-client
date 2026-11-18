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

    // ============================================================================================ catalog

    /** Refreshes the catalog unless it ran recently (or {@code force}). No-op while online features are off. */
    public static void refreshCatalog(boolean force) {
        if (!GlobalSettings.online()) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastCatalog < 10 * 60_000) return;
        if (!CATALOG_RUNNING.compareAndSet(false, true)) return;
        lastCatalog = now;
        Tasks.run(() -> {
            try {
                loadCatalog();
            } catch (Exception e) {
                Vibey.LOG.info("Cosmetics catalog not available: {}", e.toString());
                loadCachedCatalog();
            } finally {
                CATALOG_RUNNING.set(false);
            }
        });
    }

    /** Online features switched off: forget catalog items (they stay cached on disk). */
    public static void dropCatalog() {
        AvatarItems.putAll(ItemSource.CATALOG, Map.of(), true);
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

    private static void loadCatalog() throws Exception {
        Path dir = cacheDir();
        Files.createDirectories(dir.resolve("assets"));
        Path cached = dir.resolve("catalog.json"), etagFile = dir.resolve("catalog.etag");
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(base() + "/api/v1/cosmetics/catalog"))
                .timeout(Duration.ofSeconds(10)).header("Accept", "application/json").GET();
        if (Files.isRegularFile(cached) && Files.isRegularFile(etagFile)) rb.header("If-None-Match", Files.readString(etagFile).trim());
        HttpResponse<byte[]> res = http().send(rb.build(), HttpResponse.BodyHandlers.ofByteArray());
        byte[] body;
        if (res.statusCode() == 304) {
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
        apply(JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject(), true);
    }

    private static void loadCachedCatalog() {
        try {
            Path cached = cacheDir().resolve("catalog.json");
            if (!Files.isRegularFile(cached)) return;
            apply(JsonParser.parseString(Files.readString(cached)).getAsJsonObject(), false);
        } catch (Exception e) {
            Vibey.LOG.debug("Cached catalog unreadable: {}", e.toString());
        }
    }

    private static void apply(JsonObject catalog, boolean download) {
        JsonArray list = catalog.has("items") ? catalog.getAsJsonArray("items") : new JsonArray();
        Map<String, AvatarItem> items = new HashMap<>();
        for (JsonElement el : list) {
            try {
                JsonObject o = el.getAsJsonObject();
                String id = o.get("id").getAsString();
                JsonObject asset = o.getAsJsonObject("asset");
                String hash = asset.get("hash").getAsString().toLowerCase(java.util.Locale.ROOT);
                if (!hash.matches("[0-9a-f]{64}")) continue;
                Path file = cacheDir().resolve("assets").resolve(hash + ".vibey");
                byte[] bytes = null;
                if (Files.isRegularFile(file)) {
                    bytes = Files.readAllBytes(file);
                    if (!BundleParser.sha256(bytes).equals(hash)) bytes = null;
                }
                if (bytes == null && download) {
                    String url = absolute(asset.has("url") ? asset.get("url").getAsString() : "/api/v1/cosmetics/assets/" + hash);
                    HttpResponse<byte[]> r = http().send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build(),
                            HttpResponse.BodyHandlers.ofByteArray());
                    if (r.statusCode() != 200) continue;
                    bytes = r.body();
                    if (!BundleParser.sha256(bytes).equals(hash)) {
                        Vibey.LOG.warn("Catalog asset {} has a wrong hash, ignored", id);
                        continue;
                    }
                    write(file, bytes);
                }
                if (bytes == null) continue;
                Bundle b = BundleParser.parse(bytes);
                if (!b.id.equals(id)) Vibey.LOG.warn("Catalog item {} contains bundle {}", id, b.id);
                prepare(b);
                String thumb = o.has("thumbnailUrl") && !o.get("thumbnailUrl").isJsonNull() ? absolute(o.get("thumbnailUrl").getAsString()) : null;
                if (thumb != null) THUMBS.put(id, thumb);
                int sort = o.has("sort") ? o.get("sort").getAsInt() : 1000;
                String availability = o.has("availability") ? o.get("availability").getAsString() : "default";
                Bundle.Text name = text(o.get("name"), b.name);
                Bundle.Text desc = text(o.get("description"), b.description);
                String rarity = o.has("rarity") ? o.get("rarity").getAsString() : null;
                items.put(b.id, new AvatarItem(b.id, b.kind, b.slot, name, desc, b.author, b.tags,
                        rarity == null ? b.rarity : Rarity.of(rarity), ItemSource.CATALOG, sort, thumb, hash, availability, b,
                        AvatarItems.nextRevision()));
            } catch (Exception e) {
                Vibey.LOG.warn("Catalog entry skipped: {}", e.toString());
            }
        }
        Vibey.LOG.info("Cosmetics catalog: {} items", items.size());
        Tasks.onMain(() -> {
            if (!GlobalSettings.online()) return;
            for (AvatarItem old : AvatarItems.all()) {
                if (old.source() == ItemSource.CATALOG) {
                    AvatarItem n = items.get(old.id());
                    if (n == null || n.bundle() != old.bundle()) {
                        GpuBundles.evict(old.bundle());
                    }
                }
            }
            AvatarItems.putAll(ItemSource.CATALOG, items, true);
        });
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
