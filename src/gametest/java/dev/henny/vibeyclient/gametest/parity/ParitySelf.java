package dev.henny.vibeyclient.gametest.parity;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;

final class ParitySelf {
    private final ClientGameTestContext context;
    private final ParityEngine engine;
    private final ParityShots shots;

    ParitySelf(ClientGameTestContext context, ParityEngine engine, ParityShots shots) {
        this.context = context;
        this.engine = engine;
        this.shots = shots;
    }

    void run() {
        context.runOnClient(mc -> {
            engine.seed(17);
            engine.equipSelf(Map.of("cape", "vibey:vibe_cape", "pet", "vibey:seal_buddy"));
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        context.waitTicks(30);
        shots.take("l01_self_front_cape_pet");
        context.setScreen(() -> engine.previews(new float[]{30f}));
        context.waitTicks(40);
        shots.take("l02_preview_in_world");
        context.runOnClient(mc -> {
            mc.options.hideGui = false;
            mc.setScreen(new InventoryScreen(mc.player));
        });
        context.waitTicks(10);
        shots.take("l03_inventory_pet");
        context.setScreen(() -> null);
        context.runOnClient(mc -> engine.openWardrobe("cape"));
        context.waitTicks(20);
        shots.take("l04_wardrobe_world_capes");
        context.setScreen(() -> null);
        context.runOnClient(mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
            engine.equipSelf(Map.of("shield", "vibey:seal_crest_shield"));
        });
        context.waitTicks(10);
        shots.take("l05_first_person_shield");
        context.runOnClient(mc -> {
            mc.player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            ParityClock.pin();
            engine.playSelf("vibey:wave");
        });
        context.waitTicks(16);
        boolean third = context.computeOnClient(mc -> mc.options.getCameraType() != CameraType.FIRST_PERSON);
        if (!third) throw new AssertionError("local emote did not switch to third person");
        shots.take("l06_self_wave_third_person");
        context.waitTicks(60);
        CameraType after = context.computeOnClient(mc -> mc.options.getCameraType());
        if (after != CameraType.FIRST_PERSON) throw new AssertionError("camera not restored after the emote: " + after);
        context.runOnClient(mc -> {
            engine.clearSelf();
            mc.options.hideGui = false;
        });
    }
}
