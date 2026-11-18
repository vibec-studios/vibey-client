package dev.henny.vibeyclient.gametest;

import com.mojang.authlib.GameProfile;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.avatar.AvatarItem;
import dev.henny.vibeyclient.avatar.AvatarItems;
import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.AvatarStates;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.Wardrobe;
import dev.henny.vibeyclient.avatar.render.AvatarHooks;
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
        context.waitFor(mc -> AvatarItems.all().size() >= 20, 600);
        context.runOnClient(mc -> System.out.println("[vibeyclient-test] items: " + AvatarItems.all().size()));

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
                AvatarStates.get(MANNEQUIN);
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
                Wardrobe w = Services.get(Wardrobe.class).orElseThrow();
                w.equip(AvatarItems.get("vibey:vibe_cape"));
                w.equip(AvatarItems.get("vibey:seal_buddy"));
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            });
            context.waitTicks(30);
            context.takeScreenshot("l01_self_front_cape_pet");
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
            System.out.println("[vibeyclient-test] layer frames: " + AvatarHooks.frames);
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
                AvatarItem i = AvatarItems.get(id);
                if (i == null || i.bundle() == null) throw new AssertionError("item missing: " + id);
            }
            AvatarStates.setLoadout(MANNEQUIN, slots);
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
            AvatarState st = AvatarStates.get(MANNEQUIN);
            if (!AvatarStates.play(st, id)) throw new AssertionError("emote not playable: " + id);
        });
        context.waitTicks((int) Math.round(at * 20));
        context.takeScreenshot(name);
        context.runOnClient(mc -> {
            AvatarState st = AvatarStates.get(MANNEQUIN);
            st.emote = null;
        });
        context.waitTicks(2);
    }

    @SuppressWarnings("unused")
    private static List<String> unused() {
        return List.of();
    }
}
