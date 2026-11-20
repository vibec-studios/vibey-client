package dev.henny.vibeyclient.gametest;

import dev.henny.cosmetico.dev.PushAction;
import dev.henny.cosmetico.dev.PushResult;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import dev.henny.vibeyclient.api.Services;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.dev.DevItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.player.RemotePlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Content showcase (opt-in, does nothing unless the env var {@code VIBEYCLIENT_SHOWCASE=<folder>} is set): loads
 * every {@code .vibey} in the folder as a DEV item (exactly like a devlink push), puts each cosmetic on a mannequin
 * and takes screenshots – front / side / back at rest, chosen moments of the long {@code idle} loops (the animation
 * clock is global, so the test waits for the right phase), walking, creative flying, elytra gliding and sneaking. Pets
 * walk beside the mannequin. Optional plan {@code VIBEYCLIENT_SHOWCASE_PLAN=<json>}:
 * {@code { "items": { "<id>": { "idle": [seconds…] } }, "hero": [ { "name", "yaw", "slots": { slot: id }, "fly"?,
 * "night"?, "distance"?, "pitch"? } ], "states": [ { "name", "state": "idle_long"|"sprint"|"glide"|"happy", "slots",
 * "yaw"?, "night"? } ], "wardrobe": { "slot", "equip": { slot: id }, "noThumbnail": [id…] } }}. Thumbnails from {@code <folder>/thumbnails}
 * are pushed with the items (devlink {@code push.thumbnail}), the wardrobe shot shows them.
 * {@code VIBEYCLIENT_SHOWCASE_ONLY=<id part>,…} limits the per-item shots ({@code hero} = hero + state + wardrobe
 * shots only).
 * <p>
 * Used for the Polar Pals collection: {@code VIBEYCLIENT_SHOWCASE=<abs>/content/dist/polar-pals
 * VIBEYCLIENT_SHOWCASE_PLAN=<abs>/content/src/polar-pals/showcase.json ./gradlew :fabric-1.21.11:runClientGameTest}.
 * Screenshots: versions/fabric-1.21.11/build/run/clientGameTest/screenshots/sc_*.png.
 */
public final class ShowcaseGameTest implements FabricClientGameTest {
    private static final UUID MANNEQUIN = UUID.fromString("5ea10000-0000-4000-8000-00000005c0de");
    private RemotePlayer mannequin;
    private double baseX, baseY, baseZ, camZ;

    private record Item(String id, String slot, String file, JsonObject json) {}

    private final Map<String, byte[]> thumbs = new LinkedHashMap<>();

    @Override
    public void runTest(ClientGameTestContext context) {
        String dirEnv = System.getenv("VIBEYCLIENT_SHOWCASE");
        if (dirEnv == null || dirEnv.isBlank()) return;
        Path dir = Path.of(dirEnv);
        JsonObject plan = new JsonObject();
        String planEnv = System.getenv("VIBEYCLIENT_SHOWCASE_PLAN");
        List<Item> items = new ArrayList<>();
        Map<String, byte[]> bytes = new LinkedHashMap<>();
        try {
            if (planEnv != null && !planEnv.isBlank()) plan = JsonParser.parseString(Files.readString(Path.of(planEnv))).getAsJsonObject();
            try (Stream<Path> s = Files.list(dir)) {
                for (Path p : s.filter(f -> f.toString().endsWith(".vibey")).sorted().toList()) {
                    byte[] b = Files.readAllBytes(p);
                    JsonObject j = JsonParser.parseString(new String(b, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                    String slot = j.has("cosmetic") ? j.getAsJsonObject("cosmetic").get("slot").getAsString() : "emote";
                    items.add(new Item(j.get("id").getAsString(), slot, p.getFileName().toString(), j));
                    bytes.put(j.get("id").getAsString(), b);
                    Path thumb = dir.resolve("thumbnails").resolve(p.getFileName().toString().replace(".vibey", ".png"));
                    if (Files.isRegularFile(thumb)) thumbs.put(j.get("id").getAsString(), Files.readAllBytes(thumb));
                }
            }
        } catch (Exception e) {
            throw new AssertionError("showcase folder not readable: " + dir, e);
        }
        // items pushed without a thumbnail show the live 3D fallback in the wardrobe
        if (plan.has("wardrobe") && plan.getAsJsonObject("wardrobe").has("noThumbnail"))
            for (JsonElement e : plan.getAsJsonObject("wardrobe").getAsJsonArray("noThumbnail")) thumbs.remove(e.getAsString());
        System.out.println("[vibeyclient-showcase] " + items.size() + " items from " + dir);
        context.getInput().resizeWindow(1280, 720);
        context.waitFor(mc -> Cosmetico.items().all().size() >= 20, 600);
        JsonObject planItems = plan.has("items") ? plan.getAsJsonObject("items") : new JsonObject();

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("time set 1000");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            world.getServer().runCommand("weather clear");
            context.runOnClient(mc -> {
                DevItems dev = Services.get(DevItems.class).orElseThrow();
                for (var e : bytes.entrySet()) {
                    PushResult r = dev.push(e.getValue(), PushAction.NONE, thumbs.get(e.getKey()));
                    if (!r.ok()) throw new AssertionError("push " + e.getKey() + ": " + r.error());
                    System.out.println("[vibeyclient-showcase] pushed " + r.id() + " warnings=" + r.warnings());
                }
                mc.options.hideGui = true;
                mc.player.setYRot(0f);
                mc.player.setXRot(10f);
                mc.player.yRotO = 0f;
                mc.player.xRotO = 10f;
                mannequin = new RemotePlayer(mc.level, new GameProfile(MANNEQUIN, "PolarPal"));
                mannequin.setId(-4400);
                baseX = mc.player.getX();
                baseY = mc.player.getY();
                camZ = mc.player.getZ();
                baseZ = camZ + 3.4;
                mannequin.setPos(baseX, baseY, baseZ);
                face(mannequin, 180f);
                mc.level.addEntity(mannequin);
                Cosmetico.engine().avatars.get(MANNEQUIN);
            });
            context.waitTicks(20);
            String onlyEnv = System.getenv("VIBEYCLIENT_SHOWCASE_ONLY");
            List<String> only = onlyEnv == null || onlyEnv.isBlank() ? List.of() : List.of(onlyEnv.split(","));
            for (Item it : items) {
                if ("emote".equals(it.slot)) continue;
                if (!only.isEmpty() && only.stream().noneMatch(it.id::contains)) continue;
                String n = it.id.substring(it.id.indexOf(':') + 1);
                wear(context, Map.of(it.slot, it.id));
                JsonObject p = planItems.has(it.id) ? planItems.getAsJsonObject(it.id) : new JsonObject();
                JsonObject anims = it.json.has("animations") ? it.json.getAsJsonObject("animations") : new JsonObject();
                double idleLen = anims.has("idle") ? anims.getAsJsonObject("idle").get("length").getAsDouble() : 1.0;
                boolean pet = "pet".equals(it.slot);
                pitch(context, pet ? 22f : 8f);
                distance(context, pet ? 2.6 : 2.7);
                shoot(context, "sc_" + n + "_01_front", pet ? 160f : 180f, 0);
                shoot(context, "sc_" + n + "_02_side", pet ? 70f : 90f, 0);
                shoot(context, "sc_" + n + "_03_back", pet ? 330f : 0f, 0);
                if (pet) {
                    // close-up: the camera looks at the pet itself (the mannequin faces us, so does the pet)
                    closeOnPet(context, 2.0);
                    context.takeScreenshot("sc_" + n + "_04_close");
                }
                if (p.has("idle")) {
                    int k = 0;
                    for (JsonElement e : p.getAsJsonArray("idle")) {
                        double t = e.getAsDouble();
                        float yaw = pet ? 180f : ("wings".equals(it.slot) || "back".equals(it.slot) || "cape".equals(it.slot) ? 20f : 160f);
                        context.runOnClient(mc -> face(mannequin, yaw));
                        if (pet && k == 0) closeOnPet(context, 2.0);
                        waitPhase(context, idleLen, t);
                        context.takeScreenshot("sc_" + n + "_1" + (k++) + "_idle_" + String.format(java.util.Locale.ROOT, "%.1f", t).replace('.', '_'));
                    }
                }
                if (pet) restoreCamera(context, 2.6, 22f);
                // walking (sideways across the view), pets follow
                walk(context, it, n, pet);
                // creative flying (upright) and elytra gliding
                fly(context, n, false, pet ? 160f : ("wings".equals(it.slot) || "cape".equals(it.slot) || "back".equals(it.slot) ? 20f : 160f));
                if ("wings".equals(it.slot) || "cape".equals(it.slot)) fly(context, n, true, 30f);
                // sneaking
                context.runOnClient(mc -> mannequin.setShiftKeyDown(true));
                shoot(context, "sc_" + n + "_40_sneak", pet ? 180f : ("wings".equals(it.slot) || "back".equals(it.slot) ? 60f : 150f), pet ? 26 : 10);
                if (pet) {
                    closeOnPet(context, 2.0);
                    context.takeScreenshot("sc_" + n + "_41_sneak_close");
                    restoreCamera(context, 2.6, 22f);
                }
                context.runOnClient(mc -> mannequin.setShiftKeyDown(false));
                context.waitTicks(10);
                context.runOnClient(mc -> {
                    Avatar st = Cosmetico.engine().avatars.get(MANNEQUIN);
                    for (String l : st.locators.names()) {
                        double[] v = st.locators.get(l);
                        System.out.println("[vibeyclient-showcase] " + it.id + " locator " + l + " = " + (Double.isNaN(v[0]) ? "NaN" : String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", v[0], v[1], v[2])));
                    }
                    st.locators.clear();
                });
            }
            // hero loadouts
            if (plan.has("hero") && (only.isEmpty() || only.contains("hero"))) {
                distance(context, 2.3);
                pitch(context, 10f);
                for (JsonElement e : plan.getAsJsonArray("hero")) {
                    JsonObject h = e.getAsJsonObject();
                    Map<String, String> slots = new LinkedHashMap<>();
                    for (var s : h.getAsJsonObject("slots").entrySet()) slots.put(s.getKey(), s.getValue().getAsString());
                    wear(context, slots);
                    if (h.has("pitch")) pitch(context, h.get("pitch").getAsFloat());
                    boolean flying = h.has("fly") && h.get("fly").getAsBoolean();
                    pitch(context, flying ? -4f : (h.has("pitch") ? h.get("pitch").getAsFloat() : 10f));
                    if (h.has("distance")) distance(context, h.get("distance").getAsDouble());
                    boolean night = h.has("night") && h.get("night").getAsBoolean();
                    if (night) night(context, world, true);
                    if (flying) {
                        context.runOnClient(mc -> {
                            mannequin.getAbilities().flying = true;
                            mannequin.setPos(baseX, baseY + 0.9, baseZ);
                        });
                    }
                    shoot(context, "sc_hero_" + h.get("name").getAsString(), h.get("yaw").getAsFloat(), 30);
                    if (night) night(context, world, false);
                    if (h.has("distance")) distance(context, 2.3);
                    context.runOnClient(mc -> {
                        mannequin.getAbilities().flying = false;
                        mannequin.setPos(baseX, baseY, baseZ);
                    });
                }
            }
            // optional states (idle_long, sprint, glide, happy)
            if (plan.has("states") && (only.isEmpty() || only.contains("hero") || only.contains("states"))) {
                for (JsonElement e : plan.getAsJsonArray("states")) state(context, world, e.getAsJsonObject());
            }
            // the wardrobe with the pushed DEV items and their thumbnails
            if (plan.has("wardrobe") && (only.isEmpty() || only.contains("hero") || only.contains("wardrobe"))) {
                JsonObject w = plan.getAsJsonObject("wardrobe");
                context.runOnClient(mc -> {
                    DevItems devItems = Services.get(DevItems.class).orElseThrow();
                    if (w.has("equip")) for (var s : w.getAsJsonObject("equip").entrySet()) {
                        byte[] b = bytes.get(s.getValue().getAsString());
                        if (b != null) devItems.push(b, PushAction.EQUIP, null);
                    }
                    mc.options.hideGui = false;
                    dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi.openWardrobe(null,
                            dev.henny.cosmetico.model.Slot.of(w.has("slot") ? w.get("slot").getAsString() : "pet"));
                });
                context.waitTicks(60);
                context.takeScreenshot("sc_wardrobe_dev_thumbnails");
                context.setScreen(() -> null);
                context.runOnClient(mc -> mc.options.hideGui = true);
            }
            context.runOnClient(mc -> {
                Cosmetico.loadouts().setIds(MANNEQUIN, Map.of());
                mc.level.removeEntity(mannequin.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                DevItems dev = Services.get(DevItems.class).orElseThrow();
                for (String id : bytes.keySet()) dev.remove(id);
                mc.options.hideGui = false;
            });
        }
        System.out.println("[vibeyclient-showcase] done");
    }

    /** Day (1000) or midnight (18000) without clouds; waits for the sky to update. */
    private static void night(ClientGameTestContext context, TestSingleplayerContext world, boolean on) {
        world.getServer().runCommand(on ? "time set 18000" : "time set 1000");
        context.waitTicks(20);
    }

    /**
     * One optional-state shot: {@code idle_long} (the mannequin stands still for 13 s), {@code sprint} (sprinting
     * across the view), {@code glide} (elytra), {@code happy} (the mannequin plays the builtin wave emote). Pets get a
     * close-up as well.
     */
    private void state(ClientGameTestContext context, TestSingleplayerContext world, JsonObject s) {
        String name = s.get("name").getAsString(), state = s.get("state").getAsString();
        Map<String, String> slots = new LinkedHashMap<>();
        for (var e : s.getAsJsonObject("slots").entrySet()) slots.put(e.getKey(), e.getValue().getAsString());
        boolean pet = slots.containsKey("pet");
        float yaw = s.has("yaw") ? s.get("yaw").getAsFloat() : (pet ? 180f : 20f);
        boolean night = s.has("night") && s.get("night").getAsBoolean();
        wear(context, slots);
        distance(context, pet ? 2.6 : 2.5);
        pitch(context, pet ? 22f : 8f);
        if (night) night(context, world, true);
        switch (state) {
            case "idle_long" -> {
                context.runOnClient(mc -> {
                    face(mannequin, yaw);
                    mannequin.setOnGround(true);
                });
                if (pet) closeOnPet(context, 2.0);
                context.waitTicks(20 * 13);
                context.runOnClient(mc -> debugWeights(name));
                context.takeScreenshot("sc_state_" + name);
                if (pet) restoreCamera(context, 2.6, 22f);
            }
            case "sprint" -> {
                distance(context, pet ? 3.6 : 3.2);
                pitch(context, 16f);
                context.runOnClient(mc -> {
                    mannequin.setPos(baseX + 2.2, baseY, baseZ);
                    mannequin.setSprinting(true);
                    face(mannequin, 90f);
                });
                context.waitTicks(20);
                for (int i = 0; i < 26; i++) {
                    final int k = i;
                    context.runOnClient(mc -> {
                        mannequin.setPos(mannequin.getX() - 0.19, baseY, baseZ);
                        mannequin.setSprinting(true);
                        face(mannequin, 90f);
                    });
                    context.waitTicks(1);
                    if (k == 20) {
                        context.runOnClient(mc -> debugWeights(name));
                        context.takeScreenshot("sc_state_" + name);
                    }
                }
                context.runOnClient(mc -> {
                    mannequin.setSprinting(false);
                    mannequin.setPos(baseX, baseY, baseZ);
                    face(mannequin, 180f);
                });
                context.waitTicks(30);
            }
            case "glide" -> {
                // gliding without an elytra item: the vanilla elytra model would cover the item
                distance(context, pet ? 3.4 : 3.0);
                pitch(context, pet ? 26f : 34f);
                context.runOnClient(mc -> {
                    mannequin.setPos(baseX, baseY + (pet ? 0.6 : 0.2), baseZ);
                    face(mannequin, yaw);
                    mannequin.startFallFlying();
                });
                context.waitTicks(40);
                context.runOnClient(mc -> debugWeights(name));
                context.takeScreenshot("sc_state_" + name);
                context.runOnClient(mc -> {
                    mannequin.stopFallFlying();
                    mannequin.setPos(baseX, baseY, baseZ);
                    face(mannequin, 180f);
                });
                context.waitTicks(20);
            }
            case "happy" -> {
                context.runOnClient(mc -> {
                    face(mannequin, yaw);
                    Cosmetico.engine().emotes.play(Cosmetico.engine().avatars.get(MANNEQUIN), s.has("emote") ? s.get("emote").getAsString() : "vibey:wave");
                });
                if (pet) closeOnPet(context, 2.0);
                else context.waitTicks(25);
                context.runOnClient(mc -> debugWeights(name));
                context.takeScreenshot("sc_state_" + name);
                context.runOnClient(mc -> Cosmetico.engine().emotes.stop(Cosmetico.engine().avatars.get(MANNEQUIN)));
                if (pet) restoreCamera(context, 2.6, 22f);
            }
            default -> throw new AssertionError("unknown showcase state " + state);
        }
        if (night) night(context, world, false);
    }

    /** Logs the smoothed states and the blend weights of every worn item (state shots). */
    private void debugWeights(String name) {
        Avatar st = Cosmetico.engine().avatars.get(MANNEQUIN);
        StringBuilder sb = new StringBuilder("[vibeyclient-showcase] " + name + String.format(java.util.Locale.ROOT,
                " walk=%.2f sprint=%.2f glide=%.2f fly=%.2f idleLong=%.2f happy=%.2f petMove=%.2f sprinting=%s",
                st.motion.walk, st.motion.sprint, st.motion.glide, st.motion.fly, st.motion.idleLong, st.motion.happy, st.pet.move, mannequin.isSprinting()));
        float[] w = new float[dev.henny.cosmetico.anim.StateWeights.NAMES.length];
        for (dev.henny.cosmetico.model.Slot slot : dev.henny.cosmetico.model.Slot.all()) {
            var b = Cosmetico.engine().shown.bundle(st, slot);
            if (b == null) continue;
            new dev.henny.cosmetico.draw.StateWeightsInput().weights(st, b, slot, w);
            sb.append(" | ").append(slot.id()).append(':');
            for (int k = 0; k < w.length; k++) if (w[k] > 0.01f) sb.append(' ').append(dev.henny.cosmetico.anim.StateWeights.NAMES[k]).append('=').append(String.format(java.util.Locale.ROOT, "%.2f", w[k]));
        }
        System.out.println(sb);
    }

    private void walk(ClientGameTestContext context, Item it, String n, boolean pet) {
        // walk to the side and back, shooting mid-way
        context.runOnClient(mc -> {
            mannequin.setPos(baseX + 1.6, baseY, baseZ);
            mannequin.xo = mannequin.getX();
            face(mannequin, 90f);
        });
        context.waitTicks(30);
        for (int i = 0; i < 26; i++) {
            final int k = i;
            context.runOnClient(mc -> {
                mannequin.setPos(mannequin.getX() - 0.13, baseY, baseZ);
                face(mannequin, 90f);
            });
            context.waitTicks(1);
            if (k == 14) context.takeScreenshot("sc_" + n + "_20_walk_a");
            if (k == 22) context.takeScreenshot("sc_" + n + "_21_walk_b");
        }
        context.runOnClient(mc -> {
            mannequin.setPos(baseX, baseY, baseZ);
            face(mannequin, 180f);
        });
        context.waitTicks(40);
    }

    private void fly(ClientGameTestContext context, String n, boolean elytra, float yaw) {
        float oldPitch = context.computeOnClient(mc -> mc.player.getXRot());
        pitch(context, elytra ? 4f : -2f);
        context.runOnClient(mc -> {
            mannequin.setPos(baseX, baseY + 1.0, baseZ);
            face(mannequin, yaw);
            if (elytra) {
                mannequin.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ELYTRA));
                mannequin.startFallFlying();
            } else {
                mannequin.getAbilities().flying = true;
            }
        });
        context.waitTicks(16);
        context.takeScreenshot("sc_" + n + (elytra ? "_32_elytra" : "_30_fly_a"));
        if (!elytra) {
            context.waitTicks(9);
            context.takeScreenshot("sc_" + n + "_31_fly_b");
        }
        context.runOnClient(mc -> {
            if (elytra) {
                mannequin.stopFallFlying();
                mannequin.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
            }
            mannequin.getAbilities().flying = false;
            mannequin.setPos(baseX, baseY, baseZ);
            face(mannequin, 180f);
        });
        pitch(context, oldPitch);
        context.waitTicks(20);
    }

    /**
     * Close-up of the pet: the camera stays put; the mannequin (facing the camera) moves so that its pet's follow
     * spot (AvatarTicker.petSide to the right, a bit behind) is {@code dist} blocks in front of the camera.
     */
    private void closeOnPet(ClientGameTestContext context, double dist) {
        context.runOnClient(mc -> {
            face(mannequin, 180f);
            var b = Cosmetico.engine().shown.bundle(Cosmetico.engine().avatars.get(MANNEQUIN), dev.henny.cosmetico.model.Slot.PET);
            double side = dev.henny.cosmetico.sim.PetGeometry.side(b);
            double half = dev.henny.cosmetico.sim.PetGeometry.halfWidth(b);
            double back = 0.35 + Math.max(0, half - 0.3) * 0.5;
            mannequin.setPos(baseX - side, baseY, camZ + dist - back);
            float pitch = (float) Math.toDegrees(Math.atan2(1.62 - 0.6, dist));
            mc.player.setXRot(pitch);
            mc.player.xRotO = pitch;
        });
        context.waitTicks(45);
    }

    private void restoreCamera(ClientGameTestContext context, double dist, float pitch) {
        context.runOnClient(mc -> {
            baseZ = camZ + dist;
            mannequin.setPos(baseX, baseY, baseZ);
            mc.player.setXRot(pitch);
            mc.player.xRotO = pitch;
        });
        context.waitTicks(30);
    }

    /** Wait until the global animation clock is at {@code t} within a loop of {@code len} seconds. */
    private static void waitPhase(ClientGameTestContext context, double len, double t) {
        context.waitFor(mc -> {
            double ph = Cosmetico.engine().clock.now() % len;
            return ph >= t && ph < t + 0.12;
        }, (int) Math.ceil(len * 20) + 40);
    }

    private void pitch(ClientGameTestContext context, float pitch) {
        context.runOnClient(mc -> {
            mc.player.setXRot(pitch);
            mc.player.xRotO = pitch;
        });
    }

    private void distance(ClientGameTestContext context, double d) {
        context.runOnClient(mc -> {
            baseZ = camZ + d;
            mannequin.setPos(baseX, baseY, baseZ);
            mc.player.setPos(baseX, baseY, camZ);
        });
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

    private void wear(ClientGameTestContext context, Map<String, String> slots) {
        context.runOnClient(mc -> {
            for (String id : slots.values()) {
                dev.henny.cosmetico.registry.Item i = Cosmetico.items().get(id);
                if (i == null || i.bundle() == null) throw new AssertionError("item missing: " + id);
            }
            Cosmetico.loadouts().setIds(MANNEQUIN, slots);
        });
        context.waitTicks(10);
    }

    private void shoot(ClientGameTestContext context, String name, float yaw, int extraTicks) {
        context.runOnClient(mc -> face(mannequin, yaw));
        context.waitTicks(14 + extraTicks);
        context.takeScreenshot(name);
    }
}
