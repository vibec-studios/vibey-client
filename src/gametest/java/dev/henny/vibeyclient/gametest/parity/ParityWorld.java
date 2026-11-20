package dev.henny.vibeyclient.gametest.parity;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ParityWorld {
    static final UUID MANNEQUIN = UUID.fromString("5ea10000-0000-4000-8000-0000000c0de1");
    private final ClientGameTestContext context;
    private final ParityEngine engine;
    private final ParityShots shots;
    private RemotePlayer mannequin;

    public ParityWorld(ClientGameTestContext context, ParityEngine engine, ParityShots shots) {
        this.context = context;
        this.engine = engine;
        this.shots = shots;
    }

    public void run() {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("tp @a 8.5 ~ 8.5 0 8");
            context.waitTicks(5);
            world.getClientWorld().waitForChunksRender();
            spawn();
            context.waitTicks(20);
            cosmetics();
            emotes();
            context.runOnClient(mc -> mannequin.setPos(mannequin.getX() + 40, mannequin.getY(), mannequin.getZ()));
            new ParitySelf(context, engine, shots).run();
            context.runOnClient(mc -> mc.level.removeEntity(mannequin.getId(), Entity.RemovalReason.DISCARDED));
        }
    }

    private void spawn() {
        context.runOnClient(mc -> {
            mc.options.hideGui = true;
            mc.player.setYRot(0f);
            mc.player.setXRot(8f);
            mc.player.yRotO = 0f;
            mc.player.xRotO = 8f;
            mannequin = new RemotePlayer(mc.level, new GameProfile(MANNEQUIN, "Mannequin"));
            mannequin.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ() + 3.2);
            face(180f);
            mc.level.addEntity(mannequin);
            engine.track(MANNEQUIN);
            ParityClock.start(engine, 100_000);
        });
    }

    private void cosmetics() {
        wear(Map.of("pet", "vibey:seal_buddy"));
        shoot("c01_pet_front", 180f);
        shoot("c02_pet_side", 90f);
        wear(Map.of("cape", "vibey:vibe_cape", "hat", "vibey:seal_beanie"));
        shoot("c03_cape_hat_back", 0f);
        wear(Map.of("wings", "vibey:crystal_wings", "hat", "vibey:ice_crown"));
        shoot("c04_wings_crown_back", 0f);
        shoot("c05_wings_crown_front", 180f);
        wear(Map.of("wings", "vibey:neon_wings", "back", "vibey:seal_backpack"));
        shoot("c06_neon_backpack_side", 270f);
        wear(Map.of("aura", "vibey:snowflake_orbit", "pet", "vibey:pixel_penguin"));
        shoot("c07_aura_penguin_front", 180f);
        wear(Map.of("aura", "vibey:ember_sparks", "cape", "vibey:aurora_cape"));
        shoot("c08_ember_aurora_back", 0f);
        context.runOnClient(mc -> {
            mannequin.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
            mannequin.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHIELD));
        });
        wear(Map.of("shield", "vibey:seal_crest_shield"));
        shoot("c09_shield_front", 180f);
        wear(Map.of("shield", "vibey:glacier_shield"));
        shoot("c10_glacier_shield_side", 270f);
        context.runOnClient(mc -> {
            mannequin.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            mannequin.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            mannequin.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        });
        wear(Map.of("cape", "vibey:pixel_stars_cape"));
        shoot("c11_elytra_cape_back", 0f);
        context.runOnClient(mc -> mannequin.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY));
        Map<String, String> all = new LinkedHashMap<>();
        all.put("cape", "vibey:vibe_cape");
        all.put("wings", "vibey:neon_wings");
        all.put("hat", "vibey:seal_beanie");
        all.put("back", "vibey:seal_backpack");
        all.put("pet", "vibey:seal_buddy");
        all.put("aura", "vibey:snowflake_orbit");
        wear(all);
        shoot("c12_all_front", 180f);
        shoot("c13_all_side", 90f);
    }

    private void emotes() {
        wear(Map.of());
        emote("vibey:wave", 18, "e01_wave_front", 180f);
        emote("vibey:guitar", 12, "e02_guitar_side", 90f);
        emote("vibey:seal_summon", 40, "e03_seal_summon_front", 180f);
        emote("vibey:sit", 24, "e04_sit_side", 90f);
        emote("vibey:vibe_dance", 16, "e05_dance_front", 180f);
        emote("vibey:seal_flop", 28, "e06_flop_side", 90f);
    }

    private void face(float yaw) {
        mannequin.setYRot(yaw);
        mannequin.yRotO = yaw;
        mannequin.yBodyRot = yaw;
        mannequin.yBodyRotO = yaw;
        mannequin.yHeadRot = yaw;
        mannequin.yHeadRotO = yaw;
        mannequin.setXRot(0);
        mannequin.xRotO = 0;
    }

    private void wear(Map<String, String> slots) {
        context.runOnClient(mc -> {
            for (String id : slots.values()) if (!engine.has(id)) throw new AssertionError("item missing: " + id);
            engine.seed(11);
            engine.wear(MANNEQUIN, slots);
        });
        context.waitTicks(8);
    }

    private void shoot(String name, float yaw) {
        context.runOnClient(mc -> face(yaw));
        context.waitTicks(12);
        shots.take(name);
    }

    private void emote(String id, int ticks, String name, float yaw) {
        context.runOnClient(mc -> {
            face(yaw);
            ParityClock.pin();
            engine.seed(13);
            engine.play(MANNEQUIN, id);
        });
        context.waitTicks(ticks);
        shots.take(name);
        context.runOnClient(mc -> engine.stop(MANNEQUIN));
        context.waitTicks(2);
    }
}
