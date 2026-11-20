package dev.henny.companio.mc.world;

import dev.henny.companio.sense.Owner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

final class OwnerExtras {
    private OwnerExtras() {}

    static void fill(Player p, Owner o) {
        Minecraft mc = Minecraft.getInstance();
        o.level = p.experienceLevel;
        o.advancement = mc.getToastManager().getToast(AdvancementToast.class, Toast.NO_TOKEN) != null;
        o.target = "minecraft:air";
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos at = hit.getBlockPos();
            BlockState state = p.level().getBlockState(at);
            if (!state.isAir()) {
                o.target = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                o.targetX = at.getX() + 0.5;
                o.targetY = at.getY() + 0.5;
                o.targetZ = at.getZ() + 0.5;
            }
        }
    }
}
