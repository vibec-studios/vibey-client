package dev.henny.vibeyclient.gametest;

import com.mojang.authlib.GameProfile;
import dev.henny.vibeyclient.api.Services;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.model.Slot;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cosmetics + emotes in the real game: a mannequin player (client-side RemotePlayer, i.e. the "other player" path)
 * wears every slot and plays emotes; screenshots from the front, the side and the back. The local player plays an
 * emote too (third person switch). Screenshots: versions/fabric-1.21.11/build/run/clientGameTest/screenshots.
 * Set -Dvibeyclient.test.only=cosmetics to skip nothing; set vibeyclient.test.fast=true for a quicker subset.
 */
public final class CosmeticsGameTest implements FabricClientGameTest {
    private static final UUID MANNEQUIN = UUID.fromString("5eal0000-0000-4000-8000-00000000c0de".replace("l", "1"));
    private RemotePlayer mannequin;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        context.waitFor(mc -> Cosmetico.items().all().size() >= 20, 600);
        context.runOnClient(mc -> System.out.println("[vibeyclient-test] items: " + Cosmetico.items().all().size()));

        // ---------------------------------------------------------------- preview on the title screen
        context.runOnClient(mc -> {
            Wardrobe w = Services.get(Wardrobe.class).orElseThrow();
            w.equip(Cosmetico.items().get("vibey:vibe_cape"));
            w.equip(Cosmetico.items().get("vibey:crystal_wings"));
            w.equip(Cosmetico.items().get("vibey:seal_beanie"));
            w.equip(Cosmetico.items().get("vibey:seal_buddy"));
        });
        PreviewTestScreen[] screen = new PreviewTestScreen[1];
        context.setScreen(() -> screen[0] = new PreviewTestScreen());
        context.waitTicks(20);
        context.takeScreenshot("p01_preview_title");
        context.runOnClient(mc -> {
            for (var v : screen[0].views) v.playEmote("vibey:guitar");
        });
        context.waitTicks(14);
        context.takeScreenshot("p02_preview_title_guitar");
        context.runOnClient(mc -> {
            for (var v : screen[0].views) {
                v.override(Slot.WINGS, "vibey:neon_wings");
                v.override(Slot.PET, "vibey:pixel_penguin");
                v.playEmote("vibey:wave");
            }
        });
        context.waitTicks(16);
        context.takeScreenshot("p03_preview_title_override_wave");
        context.setScreen(() -> null);
        // the real wardrobe on the title screen (real registry, thumbnails, preview)
        context.runOnClient(mc -> dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi.openWardrobe(null, Slot.WINGS));
        context.waitTicks(30);
        context.takeScreenshot("w01_wardrobe_title_wings");
        context.runOnClient(mc -> dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi.openWardrobe(null, Slot.PET));
        context.waitTicks(20);
        context.takeScreenshot("w02_wardrobe_title_pets");
        context.runOnClient(mc -> dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi.openWardrobeEmotes(null));
        context.waitTicks(20);
        context.takeScreenshot("w03_wardrobe_title_emotes");
        context.setScreen(() -> null);
        context.runOnClient(mc -> {
            Wardrobe w = Services.get(Wardrobe.class).orElseThrow();
            for (Slot s : Slot.all()) w.unequip(s);
            String[] wheel = {"vibey:wave", "vibey:cheer", "vibey:guitar", "vibey:vibe_dance", "vibey:sit", "vibey:seal_summon", "vibey:clap", "vibey:shrug"};
            for (int i = 0; i < wheel.length; i++) w.setWheel(i, wheel[i]);
        });

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            world.getServer().runCommand("weather clear");
            context.runOnClient(mc -> {
                mc.options.hideGui = true;
                mc.player.setYRot(0f);
                mc.player.setXRot(8f);
                mc.player.yRotO = 0f;
                mc.player.xRotO = 8f;
                // the mannequin stands 3 blocks in front (south), facing us
                mannequin = new RemotePlayer(mc.level, new GameProfile(MANNEQUIN, "Mannequin"));
                mannequin.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ() + 3.2);
                face(mannequin, 180f);
                mc.level.addEntity(mannequin);
                Cosmetico.engine().avatars.get(MANNEQUIN);
            });
            context.waitTicks(20);

            // ---------------------------------------------------------------- every slot, front/side/back
            Map<String, String> all = new LinkedHashMap<>();
            all.put("cape", "vibey:vibe_cape");
            all.put("wings", "vibey:neon_wings");
            all.put("hat", "vibey:seal_beanie");
            all.put("back", "vibey:seal_backpack");
            all.put("pet", "vibey:seal_buddy");
            all.put("aura", "vibey:ember_sparks");
            wear(context, Map.of("pet", "vibey:seal_buddy"));
            shoot(context, "c01_pet_seal_front", 180f);
            shoot(context, "c02_pet_seal_side", 90f);
            wear(context, Map.of("cape", "vibey:vibe_cape", "hat", "vibey:seal_beanie"));
            shoot(context, "c03_cape_hat_back", 0f);
            shoot(context, "c04_cape_hat_side", 90f);
            wear(context, Map.of("wings", "vibey:crystal_wings", "hat", "vibey:ice_crown"));
            shoot(context, "c05_wings_crown_back", 0f);
            shoot(context, "c06_wings_crown_front", 180f);
            wear(context, Map.of("wings", "vibey:neon_wings", "back", "vibey:seal_backpack"));
            shoot(context, "c07_neon_back", 0f);
            shoot(context, "c08_backpack_side", 270f);
            wear(context, Map.of("aura", "vibey:snowflake_orbit", "pet", "vibey:pixel_penguin"));
            shoot(context, "c09_aura_penguin_front", 180f);
            wear(context, Map.of("aura", "vibey:ember_sparks", "cape", "vibey:aurora_cape"));
            shoot(context, "c10_ember_aurora_back", 0f);
            context.runOnClient(mc -> {
                mannequin.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHIELD));
                mannequin.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHIELD));
            });
            wear(context, Map.of());
            shoot(context, "c10a_shield_vanilla_side", 90f);
            wear(context, Map.of("shield", "vibey:seal_crest_shield"));
            shoot(context, "c10b_shield_front", 180f);
            shoot(context, "c10c_shield_side", 90f);
            wear(context, Map.of("shield", "vibey:glacier_shield"));
            shoot(context, "c10d_glacier_shield_side", 270f);
            context.runOnClient(mc -> {
                mannequin.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, net.minecraft.world.item.ItemStack.EMPTY);
                mannequin.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            });
            wear(context, all);
            shoot(context, "c11_all_front", 180f);
            shoot(context, "c12_all_side", 90f);

            // ---------------------------------------------------------------- emotes on the mannequin
            wear(context, Map.of());
            emote(context, "vibey:wave", 0.9, "e01_wave_front", 180f);
            emote(context, "vibey:wave", 0.9, "e02_wave_side", 90f);
            emote(context, "vibey:guitar", 0.6, "e03_guitar_front", 180f);
            emote(context, "vibey:guitar", 0.6, "e04_guitar_side_right", 90f);
            emote(context, "vibey:seal_summon", 2.0, "e05_seal_summon_front", 180f);
            emote(context, "vibey:sit", 1.2, "e06_sit_side", 90f);
            emote(context, "vibey:cheer", 0.7, "e07_cheer_front", 180f);
            emote(context, "vibey:facepalm", 1.2, "e08_facepalm_front", 180f);
            emote(context, "vibey:clap", 0.6, "e09_clap_front", 180f);
            emote(context, "vibey:vibe_dance", 0.8, "e10_dance_front", 180f);
            emote(context, "vibey:seal_flop", 1.4, "e11_flop_side", 90f);
            emote(context, "vibey:shrug", 0.8, "e12_shrug_front", 180f);

            // ---------------------------------------------------------------- local player
            context.runOnClient(mc -> {
                mannequin.setPos(mannequin.getX() + 40, mannequin.getY(), mannequin.getZ());
                Wardrobe w = Services.get(Wardrobe.class).orElseThrow();
                w.equip(Cosmetico.items().get("vibey:vibe_cape"));
                w.equip(Cosmetico.items().get("vibey:seal_buddy"));
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            });
            context.waitTicks(30);
            context.takeScreenshot("l01_self_front_cape_pet");
            context.setScreen(PreviewTestScreen::new);
            context.waitTicks(10);
            context.takeScreenshot("l01b_preview_in_world");
            context.setScreen(() -> null);
            context.runOnClient(mc -> {
                mc.options.hideGui = false;
                dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi.openWardrobe(null, Slot.CAPE);
            });
            context.waitTicks(20);
            context.takeScreenshot("w04_wardrobe_world_capes");
            context.setScreen(() -> null);
            boolean[] held = {true};
            context.runOnClient(mc -> dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi.openWheel(() -> held[0]));
            context.waitTicks(10);
            context.takeScreenshot("w05_emote_wheel_world");
            context.setScreen(() -> null);
            context.runOnClient(mc -> mc.options.hideGui = true);
            context.runOnClient(mc -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                Services.get(Wardrobe.class).orElseThrow().playEmote("vibey:wave");
            });
            context.waitTicks(16);
            boolean third = context.computeOnClient(mc -> mc.options.getCameraType() != CameraType.FIRST_PERSON);
            System.out.println("[vibeyclient-test] emote switched camera: " + third);
            context.takeScreenshot("l02_self_wave_third_person");
            context.waitTicks(60);
            CameraType after = context.computeOnClient(mc -> mc.options.getCameraType());
            System.out.println("[vibeyclient-test] camera after emote: " + after);
            if (after != CameraType.FIRST_PERSON) throw new AssertionError("camera not restored after the emote: " + after);
            context.runOnClient(mc -> {
                Wardrobe w = Services.get(Wardrobe.class).orElseThrow();
                for (Slot s : Slot.all()) w.unequip(s);
                mc.options.hideGui = false;
                mc.level.removeEntity(mannequin.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            });
            // keep a copy of the world for runtime checks on other versions (--quickPlaySingleplayer vibeytest)
            try {
                java.nio.file.Path src = world.getWorldSave().getSaveDirectory();
                java.nio.file.Path dst = java.nio.file.Path.of(System.getProperty("user.dir")).resolve("../vibeytest-world").normalize();
                copy(src, dst);
                System.out.println("[vibeyclient-test] world copied to " + dst);
            } catch (Exception e) {
                System.out.println("[vibeyclient-test] world copy failed: " + e);
            }
        }
    }

    private static void copy(java.nio.file.Path src, java.nio.file.Path dst) throws java.io.IOException {
        try (var walk = java.nio.file.Files.walk(src)) {
            for (java.nio.file.Path p : (Iterable<java.nio.file.Path>) walk::iterator) {
                if (p.getFileName().toString().equals("session.lock")) continue;
                java.nio.file.Path t = dst.resolve(src.relativize(p).toString());
                if (java.nio.file.Files.isDirectory(p)) java.nio.file.Files.createDirectories(t);
                else java.nio.file.Files.copy(p, t, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
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
                Item i = Cosmetico.items().get(id);
                if (i == null || i.bundle() == null) throw new AssertionError("item missing: " + id);
            }
            Cosmetico.loadouts().setIds(MANNEQUIN, slots);
        });
        context.waitTicks(8);
    }

    private void shoot(ClientGameTestContext context, String name, float yaw) {
        context.runOnClient(mc -> face(mannequin, yaw));
        context.waitTicks(12);
        context.takeScreenshot(name);
    }

    private void emote(ClientGameTestContext context, String id, double at, String name, float yaw) {
        context.runOnClient(mc -> {
            face(mannequin, yaw);
            Avatar st = Cosmetico.engine().avatars.get(MANNEQUIN);
            if (!Cosmetico.engine().emotes.play(st, id)) throw new AssertionError("emote not playable: " + id);
        });
        context.waitTicks((int) Math.round(at * 20));
        context.takeScreenshot(name);
        context.runOnClient(mc -> {
            Avatar st = Cosmetico.engine().avatars.get(MANNEQUIN);
            st.emote = null;
        });
        context.waitTicks(2);
    }

    @SuppressWarnings("unused")
    private static List<String> unused() {
        return List.of();
    }
}
