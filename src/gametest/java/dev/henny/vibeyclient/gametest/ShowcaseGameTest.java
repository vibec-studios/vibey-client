package dev.henny.vibeyclient.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.avatar.AvatarItem;
import dev.henny.vibeyclient.avatar.AvatarItems;
import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.AvatarStates;
import dev.henny.vibeyclient.avatar.DevItems;
import dev.henny.vibeyclient.avatar.anim.Clock;
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
 * {@code { "items": { "<id>": { "idle": [seconds…] } }, "hero": [ { "name", "yaw", "slots": { slot: id } } ] }}.
 * {@code VIBEYCLIENT_SHOWCASE_ONLY=<id part>,…} limits the per-item shots (use {@code hero} for the hero shots only).
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
                }
            }
        } catch (Exception e) {
            throw new AssertionError("showcase folder not readable: " + dir, e);
        }
        System.out.println("[vibeyclient-showcase] " + items.size() + " items from " + dir);
        context.getInput().resizeWindow(1280, 720);
        context.waitFor(mc -> AvatarItems.all().size() >= 20, 600);
        JsonObject planItems = plan.has("items") ? plan.getAsJsonObject("items") : new JsonObject();

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("time set 1000");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            world.getServer().runCommand("weather clear");
            context.runOnClient(mc -> {
                DevItems dev = Services.get(DevItems.class).orElseThrow();
                for (var e : bytes.entrySet()) {
                    DevItems.PushResult r = dev.push(e.getValue(), DevItems.Action.NONE);
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
                AvatarStates.get(MANNEQUIN);
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
                    closeOnPet(context, 1.3);
                    context.takeScreenshot("sc_" + n + "_04_close");
                }
                if (p.has("idle")) {
                    int k = 0;
                    for (JsonElement e : p.getAsJsonArray("idle")) {
                        double t = e.getAsDouble();
                        float yaw = pet ? 180f : ("wings".equals(it.slot) || "back".equals(it.slot) || "cape".equals(it.slot) ? 20f : 160f);
                        context.runOnClient(mc -> face(mannequin, yaw));
                        if (pet && k == 0) closeOnPet(context, 1.3);
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
                    closeOnPet(context, 1.3);
                    context.takeScreenshot("sc_" + n + "_41_sneak_close");
                    restoreCamera(context, 2.6, 22f);
                }
                context.runOnClient(mc -> mannequin.setShiftKeyDown(false));
                context.waitTicks(10);
                context.runOnClient(mc -> {
                    AvatarState st = AvatarStates.get(MANNEQUIN);
                    for (String l : st.locators.keySet()) {
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
                    pitch(context, flying ? -4f : 10f);
                    if (flying) {
                        context.runOnClient(mc -> {
                            mannequin.getAbilities().flying = true;
                            mannequin.setPos(baseX, baseY + 0.9, baseZ);
                        });
                    }
                    shoot(context, "sc_hero_" + h.get("name").getAsString(), h.get("yaw").getAsFloat(), 30);
                    context.runOnClient(mc -> {
                        mannequin.getAbilities().flying = false;
                        mannequin.setPos(baseX, baseY, baseZ);
                    });
                }
            }
            context.runOnClient(mc -> {
                AvatarStates.setLoadout(MANNEQUIN, Map.of());
                mc.level.removeEntity(mannequin.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                DevItems dev = Services.get(DevItems.class).orElseThrow();
                for (String id : bytes.keySet()) dev.remove(id);
                mc.options.hideGui = false;
            });
        }
        System.out.println("[vibeyclient-showcase] done");
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
     * spot (AvatarTicker: 0.95 to the right, 0.35 behind) is {@code dist} blocks in front of the camera.
     */
    private void closeOnPet(ClientGameTestContext context, double dist) {
        context.runOnClient(mc -> {
            face(mannequin, 180f);
            mannequin.setPos(baseX - 0.95, baseY, camZ + dist - 0.35);
            float pitch = (float) Math.toDegrees(Math.atan2(1.62 - 0.3, dist));
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
            double ph = Clock.now() % len;
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
                AvatarItem i = AvatarItems.get(id);
                if (i == null || i.bundle() == null) throw new AssertionError("item missing: " + id);
            }
            AvatarStates.setLoadout(MANNEQUIN, slots);
        });
        context.waitTicks(10);
    }

    private void shoot(ClientGameTestContext context, String name, float yaw, int extraTicks) {
        context.runOnClient(mc -> face(mannequin, yaw));
        context.waitTicks(14 + extraTicks);
        context.takeScreenshot(name);
    }
}
