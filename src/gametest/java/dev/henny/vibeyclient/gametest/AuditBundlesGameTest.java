package dev.henny.vibeyclient.gametest;

import dev.henny.cosmetico.dev.PushAction;
import dev.henny.cosmetico.dev.PushResult;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.sun.net.httpserver.HttpServer;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Services;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.dev.DevItems;
import dev.henny.vibeyclient.module.cosmetics.catalog.Catalog;
import dev.henny.cosmetico.registry.ItemSource;
import dev.henny.cosmetico.model.Slot;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.parse.BundleException;
import dev.henny.cosmetico.parse.Bundles;
import dev.henny.cosmetico.parse.NeedsUpdateException;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.player.RemotePlayer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Data-driven audit (content/audit, built by content/tools/build-audit.mjs) and OTA cosmetics:
 * <ol>
 * <li>parse every audit bundle: unusual combinations load with warnings, bundles for a newer client are recognised,
 *     an unknown slot is rejected (never a crash);</li>
 * <li>push them as dev items and render them on a mannequin (hat with a looping particle/sound locator, pet with
 *     props and visibility toggles in idle and walk, per-face animated/HD textures, wings with a future attach point,
 *     an emote with three props on different bones); locators must get world positions;</li>
 * <li>OTA against a local mock catalog server: first catalog, then a changed asset hash for an equipped item, a new
 *     community item with a creator, an item that needs a newer client, an unknown kind – hot swap keeps the item
 *     equipped, a 304 keeps the parsed bundles, the "new items" hook fires once.</li>
 * </ol>
 * Screenshots: versions/fabric-1.21.11/build/run/clientGameTest/screenshots/a*.png.
 */
public final class AuditBundlesGameTest implements FabricClientGameTest {
    private static final UUID MANNEQUIN = UUID.fromString("5ea10000-0000-4000-8000-0000000a0d17");
    private RemotePlayer mannequin;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        context.waitFor(mc -> Cosmetico.items().all().size() >= 20, 600);
        Path dir = auditDir();
        Map<String, byte[]> files = new HashMap<>();
        try {
            for (var e : JsonParser.parseString(Files.readString(dir.resolve("index.json"))).getAsJsonObject().getAsJsonArray("files")) {
                files.put(e.getAsString().replace(".vibey", ""), Files.readAllBytes(dir.resolve(e.getAsString())));
            }
        } catch (Exception e) {
            throw new AssertionError("content/audit not readable from " + dir, e);
        }

        // ---------------------------------------------------------------- 1. parser
        Bundle hat = Bundles.parse(files.get("sparkle_hat"));
        expectWarning(hat, "unknown type light");
        expectWarning(hat, "unknown interpolation \"elastic\"");
        Bundle wings = Bundles.parse(files.get("future_wings"));
        expectWarning(wings, "unknown attach \"tail\"");
        expectWarning(wings, "unknown interpolation \"bounce\"");
        Bundle back = Bundles.parse(files.get("faces_back"));
        check(back.textures.length == 3 && back.textures[1].frameCount == 4 && back.textures[1].interpolate, "faces_back strip texture");
        check(back.textures[2].pngWidth == 128 && back.textures[2].width == 16, "faces_back HD texture");
        Bundle emote = Bundles.parse(files.get("triple_prop_emote"));
        check(emote.model.locators.size() == 4 && emote.isEmote(), "triple_prop_emote locators");
        Bundles.parse(files.get("prop_pet"));
        expectThrows(files.get("needs_update"), NeedsUpdateException.class, "9.0.0");
        expectThrows(files.get("future_format"), NeedsUpdateException.class, "formatVersion 2");
        expectThrows(files.get("unknown_slot"), BundleException.class, "tail");
        System.out.println("[vibeyclient-test] audit parser ok");

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            world.getServer().runCommand("weather clear");

            // ---------------------------------------------------------------- 2. render the audit items
            context.runOnClient(mc -> {
                DevItems dev = Services.get(DevItems.class).orElseThrow();
                for (String f : List.of("sparkle_hat", "prop_pet", "faces_back", "future_wings", "triple_prop_emote")) {
                    PushResult r = dev.push(files.get(f), PushAction.NONE, null);
                    if (!r.ok()) throw new AssertionError("push " + f + ": " + r.error());
                }
                mc.options.hideGui = true;
                mc.player.setYRot(0f);
                mc.player.setXRot(8f);
                mc.player.yRotO = 0f;
                mc.player.xRotO = 8f;
                mannequin = new RemotePlayer(mc.level, new GameProfile(MANNEQUIN, "Audit"));
                mannequin.setId(-4300);
                mannequin.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ() + 3.2);
                face(mannequin, 180f);
                mc.level.addEntity(mannequin);
                Map<String, String> slots = new HashMap<>();
                slots.put("hat", "vibeytest:sparkle_hat");
                slots.put("pet", "vibeytest:prop_pet");
                slots.put("back", "vibeytest:faces_back");
                slots.put("wings", "vibeytest:future_wings");
                Cosmetico.loadouts().setIds(MANNEQUIN, slots);
            });
            context.waitTicks(30);
            context.takeScreenshot("a01_audit_front");
            context.runOnClient(mc -> face(mannequin, 0f));
            context.waitTicks(12);
            context.takeScreenshot("a02_audit_back");
            context.runOnClient(mc -> {
                Avatar st = Cosmetico.engine().avatars.get(MANNEQUIN);
                for (String l : List.of("orbit_tip", "crown", "top", "balloon")) {
                    double[] p = st.locators.get(l);
                    if (p == null || Double.isNaN(p[0])) throw new AssertionError("locator " + l + " has no world position");
                }
            });
            // walking: the pet's walk animation shows the sign, hides the balloon and fires its own events
            for (int i = 0; i < 24; i++) {
                context.runOnClient(mc -> {
                    mannequin.setPos(mannequin.getX() + 0.12, mannequin.getY(), mannequin.getZ());
                    face(mannequin, 270f);
                });
                context.waitTicks(1);
            }
            context.takeScreenshot("a03_audit_walking");
            context.runOnClient(mc -> {
                mannequin.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ() + 3.2);
                mannequin.xo = mannequin.getX();
                mannequin.zo = mannequin.getZ();
                face(mannequin, 180f);
                Cosmetico.loadouts().setIds(MANNEQUIN, Map.of());
                if (!Cosmetico.engine().emotes.play(Cosmetico.engine().avatars.get(MANNEQUIN), "vibeytest:triple_prop_emote")) throw new AssertionError("emote did not start");
            });
            context.waitTicks(16);
            context.takeScreenshot("a04_audit_emote_props");
            context.waitTicks(10);
            context.takeScreenshot("a05_audit_emote_props_later");
            context.runOnClient(mc -> {
                Avatar st = Cosmetico.engine().avatars.get(MANNEQUIN);
                for (String l : List.of("wand_tip", "lantern", "halo", "hand")) {
                    double[] p = st.locators.get(l);
                    if (p == null || Double.isNaN(p[0])) throw new AssertionError("emote locator " + l + " has no world position");
                }
                Cosmetico.engine().emotes.stop(st);
                DevItems dev = Services.get(DevItems.class).orElseThrow();
                for (String id : new ArrayList<>(dev.ids())) if (id.startsWith("vibeytest:")) dev.remove(id);
            });
            System.out.println("[vibeyclient-test] audit render ok");

            // ---------------------------------------------------------------- 3. OTA against a mock catalog
            ota(context, files);

            context.runOnClient(mc -> {
                mc.level.removeEntity(mannequin.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                mc.options.hideGui = false;
            });
        }
    }

    // ============================================================================================ OTA

    private static final class MockCatalog {
        final HttpServer server;
        final Map<String, byte[]> assets = new ConcurrentHashMap<>();
        volatile String etag;
        volatile byte[] catalog;
        final AtomicInteger notModified = new AtomicInteger(), catalogs = new AtomicInteger(), assetHits = new AtomicInteger();

        MockCatalog() throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/cosmetics/catalog", ex -> {
                String inm = ex.getRequestHeaders().getFirst("If-None-Match");
                if (etag.equals(inm)) {
                    notModified.incrementAndGet();
                    ex.sendResponseHeaders(304, -1);
                } else {
                    catalogs.incrementAndGet();
                    ex.getResponseHeaders().add("ETag", etag);
                    ex.getResponseHeaders().add("Content-Type", "application/json");
                    ex.sendResponseHeaders(200, catalog.length);
                    ex.getResponseBody().write(catalog);
                }
                ex.close();
            });
            server.createContext("/api/v1/cosmetics/assets/", ex -> {
                String hash = ex.getRequestURI().getPath().substring("/api/v1/cosmetics/assets/".length());
                byte[] b = assets.get(hash);
                assetHits.incrementAndGet();
                if (b == null) ex.sendResponseHeaders(404, -1);
                else {
                    ex.sendResponseHeaders(200, b.length);
                    ex.getResponseBody().write(b);
                }
                ex.close();
            });
            server.createContext("/", ex -> {
                ex.sendResponseHeaders(404, -1);
                ex.close();
            });
            server.start();
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        JsonObject item(String id, String kind, String slot, byte[] bundle, String availability) {
            String hash = Bundles.sha256(bundle);
            assets.put(hash, bundle);
            JsonObject o = new JsonObject();
            o.addProperty("id", id);
            o.addProperty("kind", kind);
            if (slot != null) o.addProperty("slot", slot);
            JsonObject name = new JsonObject();
            name.addProperty("en", id);
            o.add("name", name);
            o.addProperty("rarity", "rare");
            o.addProperty("availability", availability);
            JsonObject asset = new JsonObject();
            asset.addProperty("hash", hash);
            asset.addProperty("size", bundle.length);
            asset.addProperty("url", "/api/v1/cosmetics/assets/" + hash);
            o.add("asset", asset);
            o.add("thumbnailUrl", com.google.gson.JsonNull.INSTANCE);
            o.addProperty("sort", 1);
            o.addProperty("someFutureField", "ignored");
            return o;
        }

        void publish(String version, JsonObject... items) {
            JsonObject c = new JsonObject();
            c.addProperty("version", version);
            JsonArray a = new JsonArray();
            for (JsonObject i : items) a.add(i);
            c.add("items", a);
            c.addProperty("futureTopLevel", true);
            catalog = c.toString().getBytes(StandardCharsets.UTF_8);
            etag = "\"" + version + "\"";
        }
    }

    private static byte[] withId(byte[] bundle, String id, String nameEn) {
        JsonObject o = JsonParser.parseString(new String(bundle, StandardCharsets.UTF_8)).getAsJsonObject();
        o.addProperty("id", id);
        JsonObject name = new JsonObject();
        name.addProperty("en", nameEn);
        o.add("name", name);
        o.remove("minClientVersion");
        return o.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void ota(ClientGameTestContext context, Map<String, byte[]> files) {
        MockCatalog mock;
        try {
            mock = new MockCatalog();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        String[] saved = new String[1];
        boolean[] wasOnline = new boolean[1];
        Set<String> announced = ConcurrentHashMap.newKeySet();
        AtomicInteger announcements = new AtomicInteger();
        var hook = new java.util.function.Consumer[1];
        try {
            String hatV1 = "community:ota_hat";
            byte[] hat1 = withId(files.get("sparkle_hat"), hatV1, "OTA Hat v1");
            mock.publish("v1", mock.item(hatV1, "cosmetic", "hat", hat1, "default"),
                    mock.item("community:ota_pet", "cosmetic", "pet", withId(files.get("prop_pet"), "community:ota_pet", "OTA Pet"), "default"));
            context.runOnClient(mc -> {
                saved[0] = GlobalSettings.backendUrl.get();
                wasOnline[0] = GlobalSettings.online();
                hook[0] = Catalog.newItemsListener();
                @SuppressWarnings("unchecked")
                java.util.function.Consumer<Set<String>> prev = hook[0];
                Catalog.onNewItems(ids -> {
                    announced.addAll(ids);
                    announcements.incrementAndGet();
                    prev.accept(ids);
                });
                Catalog.dropCatalog();
                GlobalSettings.backendUrl.set(mock.url());
                GlobalSettings.onlineFeatures.set(true);
                Catalog.refreshCatalog(true);
            });
            context.waitFor(mc -> Cosmetico.items().get(hatV1, ItemSource.REMOTE) != null && Cosmetico.items().get(hatV1).bundle() != null, 1600);
            Bundle[] first = new Bundle[1];
            context.runOnClient(mc -> {
                Wardrobe w = Services.get(Wardrobe.class).orElseThrow();
                Item item = Cosmetico.items().get(hatV1);
                if (!w.canUse(item)) throw new AssertionError("default catalog item not usable");
                w.equip(item);
                first[0] = item.bundle();
                if (announcements.get() != 0) throw new AssertionError("first catalog load must not announce new items");
            });
            System.out.println("[vibeyclient-test] ota v1 loaded");

            // v2: changed asset of the equipped hat, a new community emote with a creator, an item that needs a newer
            // client, an item of an unknown kind, a deleted creator
            byte[] hat2 = withId(files.get("sparkle_hat"), hatV1, "OTA Hat v2");
            JsonObject emote = mock.item("community:ota_emote", "emote", null,
                    withId(files.get("triple_prop_emote"), "community:ota_emote", "OTA Emote"), "default");
            JsonObject creator = new JsonObject();
            creator.addProperty("uuid", "5ea10000-0000-4000-8000-0000000c4ea7");
            creator.addProperty("name", "AuditCreator");
            emote.add("creator", creator);
            JsonObject newer = mock.item("community:ota_newer", "cosmetic", "hat", withId(files.get("needs_update"), "community:ota_newer", "OTA Newer"), "default");
            newer.addProperty("minClientVersion", "9.0.0");
            JsonObject unknownKind = mock.item("community:ota_pose", "pose", null, withId(files.get("faces_back"), "community:ota_pose", "OTA Pose"), "default");
            unknownKind.addProperty("minClientVersion", "9.0.0");
            JsonObject gone = mock.item("community:ota_back", "cosmetic", "back", withId(files.get("faces_back"), "community:ota_back", "OTA Back"), "default");
            JsonObject deleted = new JsonObject();
            deleted.addProperty("uuid", "00000000-0000-0000-0000-000000000000");
            deleted.addProperty("name", "Gelöschter Nutzer");
            gone.add("creator", deleted);
            JsonObject hatItem2 = mock.item(hatV1, "cosmetic", "hat", hat2, "default");
            hatItem2.add("creator", creator);
            mock.publish("v2", hatItem2,
                    mock.item("community:ota_pet", "cosmetic", "pet", withId(files.get("prop_pet"), "community:ota_pet", "OTA Pet"), "default"),
                    emote, newer, unknownKind, gone);
            context.runOnClient(mc -> Catalog.requestCatalog(0)); // what catalog.updated does (minus the jitter)
            context.waitFor(mc -> {
                Item i = Cosmetico.items().get(hatV1);
                return i != null && i.bundle() != null && i.bundle() != first[0];
            }, 800);
            context.runOnClient(mc -> {
                Wardrobe w = Services.get(Wardrobe.class).orElseThrow();
                Item hatItem = Cosmetico.items().get(hatV1);
                if (!hatV1.equals(w.equipped(Slot.HAT))) throw new AssertionError("hot swap unequipped the hat");
                if (!"OTA Hat v2".equals(hatItem.bundle().name.en())) throw new AssertionError("hat not swapped");
                Item e = Cosmetico.items().get("community:ota_emote");
                if (e == null || e.bundle() == null || !"AuditCreator".equals(e.creator())) throw new AssertionError("new emote/creator: " + e);
                Item n = Cosmetico.items().get("community:ota_newer");
                if (n == null || !n.needsUpdate() || w.canUse(n) || n.bundle() != null) throw new AssertionError("needs-update stub: " + n);
                if (!w.lockedReason(n).toLowerCase(java.util.Locale.ROOT).contains("vibey")) throw new AssertionError("locked reason: " + w.lockedReason(n));
                if (Cosmetico.items().get("community:ota_pose") != null) throw new AssertionError("unknown kind must be skipped");
                Item b = Cosmetico.items().get("community:ota_back");
                if (b == null || !Item.DELETED_CREATOR.equals(b.creator())) throw new AssertionError("deleted creator: " + (b == null ? null : b.creator()));
            });
            context.waitFor(mc -> announcements.get() > 0, 100);
            if (announcements.get() != 1 || !announced.containsAll(List.of("community:ota_emote", "community:ota_back")))
                throw new AssertionError("new items announced " + announcements.get() + "x: " + announced);
            System.out.println("[vibeyclient-test] ota v2 hot swap ok, announced " + announced);
            context.waitTicks(80); // the "new cosmetics" toast (debounced 3 s)
            context.runOnClient(mc -> mc.options.hideGui = false);
            context.takeScreenshot("a06_ota_toast");

            context.runOnClient(mc -> dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi.openWardrobe(null, Slot.HAT));
            context.waitTicks(30);
            context.takeScreenshot("a07_ota_wardrobe_hats_update");
            context.runOnClient(mc -> dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi.openWardrobeEmotes(null));
            context.waitTicks(20);
            context.takeScreenshot("a08_ota_wardrobe_emotes");
            context.setScreen(() -> null);

            // unchanged catalog: 304, nothing is parsed or uploaded again
            Bundle[] kept = new Bundle[1];
            int before = mock.notModified.get();
            context.runOnClient(mc -> {
                kept[0] = Cosmetico.items().get(hatV1).bundle();
                Catalog.requestCatalog(0);
            });
            context.waitFor(mc -> mock.notModified.get() > before, 800);
            context.waitTicks(10);
            context.runOnClient(mc -> {
                if (Cosmetico.items().get(hatV1).bundle() != kept[0]) throw new AssertionError("304 re-parsed the hat");
            });
            System.out.println("[vibeyclient-test] ota 304 ok (" + mock.catalogs.get() + " catalogs, " + mock.notModified.get() + " not modified, " + mock.assetHits.get() + " asset downloads)");
        } finally {
            context.runOnClient(mc -> {
                Wardrobe w = Services.get(Wardrobe.class).orElseThrow();
                w.unequip(Slot.HAT);
                if (hook[0] != null) {
                    @SuppressWarnings("unchecked")
                    java.util.function.Consumer<Set<String>> prev = hook[0];
                    Catalog.onNewItems(prev);
                }
                Catalog.dropCatalog();
                if (saved[0] != null) GlobalSettings.backendUrl.set(saved[0]);
                GlobalSettings.onlineFeatures.set(wasOnline[0]);
                if (wasOnline[0]) Catalog.refreshCatalog(true);
            });
            mock.server.stop(0);
        }
    }

    // ============================================================================================ util

    private static Path auditDir() {
        Path p = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8 && p != null; i++, p = p.getParent()) {
            Path c = p.resolve("content").resolve("audit");
            if (Files.isRegularFile(c.resolve("index.json"))) return c;
        }
        throw new AssertionError("content/audit not found above " + Path.of("").toAbsolutePath());
    }

    private static void expectWarning(Bundle b, String part) {
        for (String w : b.warnings) if (w.contains(part)) return;
        throw new AssertionError(b.id + ": no warning containing '" + part + "' in " + b.warnings);
    }

    private static void expectThrows(byte[] bytes, Class<? extends RuntimeException> type, String part) {
        try {
            Bundles.parse(bytes);
        } catch (RuntimeException e) {
            if (type.isInstance(e) && String.valueOf(e.getMessage()).contains(part)) return;
            throw new AssertionError("expected " + type.getSimpleName() + " with '" + part + "', got " + e);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " with '" + part + "', but it parsed");
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    private static void face(RemotePlayer p, float yaw) {
        p.setYRot(yaw);
        p.yRotO = yaw;
        p.yBodyRot = yaw;
        p.yBodyRotO = yaw;
        p.yHeadRot = yaw;
        p.yHeadRotO = yaw;
        p.setXRot(0);
        p.xRotO = 0;
    }
}
