package dev.henny.vibeyclient.module.info.look;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class LookTarget {
    private static BlockPos block;
    private static Direction face;
    private static BlockState state;
    private static Entity entity;

    private LookTarget() {}

    public static void update(Minecraft mc) {
        clear();
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return;
        if (hit instanceof EntityHitResult eh) {
            entity = eh.getEntity();
            return;
        }
        if (hit instanceof BlockHitResult bh && mc.level != null) {
            BlockState s = mc.level.getBlockState(bh.getBlockPos());
            if (s.isAir()) return;
            block = bh.getBlockPos().immutable();
            face = bh.getDirection();
            state = s;
        }
    }

    public static void clear() {
        block = null;
        face = null;
        state = null;
        entity = null;
    }

    public static BlockPos block() {
        return block;
    }

    public static Direction face() {
        return face;
    }

    public static BlockState state() {
        return state;
    }

    public static Entity entity() {
        return entity;
    }
}
