package dev.henny.companio.mc.world;

import dev.henny.companio.move.BlockView;
import dev.henny.companio.move.Fluid;
import dev.henny.companio.sense.Senses;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class NearScan {
    private static final int R = 8;

    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    private static final int[][] OFFSETS = offsets();

    private static int[][] offsets() {
        java.util.List<int[]> l = new java.util.ArrayList<>();
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) for (int dy = -3; dy <= 3; dy++) {
            if (dx * dx + dy * dy + dz * dz <= R * R) l.add(new int[] {dx, dy, dz});
        }
        l.sort(java.util.Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1] + o[2] * o[2]));
        return l.toArray(new int[0][]);
    }

    public void scan(ClientLevel level, Player owner, BlockView view, double cx, double cy, double cz, Senses s) {
        s.nearWater = s.nearLava = s.nearFire = Senses.FAR;
        s.danger = false;
        int bx = (int) Math.floor(cx), by = (int) Math.floor(cy), bz = (int) Math.floor(cz);
        Vec3 eye = owner.getEyePosition();
        int checks = 0;
        for (int[] o : OFFSETS) {
            if (s.nearWater < Senses.FAR && s.nearLava < Senses.FAR && s.nearFire < Senses.FAR) break;
            int x = bx + o[0], y = by + o[1], z = bz + o[2];
            Fluid f = view.fluid(x, y, z);
            boolean fire = f == Fluid.NONE && level.getBlockState(pos.set(x, y, z)).is(BlockTags.FIRE);
            if (f == Fluid.NONE && !fire) continue;
            if ((f == Fluid.WATER ? s.nearWater : f == Fluid.LAVA ? s.nearLava : s.nearFire) < Senses.FAR) continue;
            if (checks++ > 48) break;
            if (!visible(level, owner, eye, x + 0.5, y + 0.9, z + 0.5)) continue;
            double d = Math.sqrt(o[0] * o[0] + o[1] * o[1] + o[2] * o[2]);
            if (f == Fluid.WATER) s.nearWater = d;
            else if (f == Fluid.LAVA) s.nearLava = d;
            else s.nearFire = d;
            if (f != Fluid.WATER && (!s.danger || d < Math.min(s.nearLava, s.nearFire))) danger(s, x + 0.5, z + 0.5, d);
        }
        mobs(level, owner, cx, cy, cz, s, Math.min(s.nearLava, s.nearFire));
    }

    private void mobs(ClientLevel level, Player owner, double cx, double cy, double cz, Senses s, double bestDanger) {
        s.nearMob = s.nearPlayer = Senses.FAR;
        AABB box = new AABB(cx - R, cy - 4, cz - R, cx + R, cy + 4, cz + R);
        for (Entity e : level.getEntities(owner, box)) {
            double d = Math.sqrt(e.distanceToSqr(cx, cy, cz));
            if (d > R) continue;
            if (e instanceof Enemy && e.isAlive() && !e.isInvisible() && d < s.nearMob && owner.hasLineOfSight(e)) {
                s.nearMob = d;
                if (d < bestDanger) {
                    danger(s, e.getX(), e.getZ(), d);
                    bestDanger = d;
                }
            } else if (e instanceof Player && !e.isInvisible() && d < s.nearPlayer && owner.hasLineOfSight(e)) {
                s.nearPlayer = d;
            }
        }
    }

    private static void danger(Senses s, double x, double z, double d) {
        s.danger = true;
        s.dangerX = x;
        s.dangerZ = z;
    }

    private static boolean visible(ClientLevel level, Player owner, Vec3 eye, double x, double y, double z) {
        Vec3 to = new Vec3(x, y, z);
        BlockHitResult hit = level.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().closerToCenterThan(to, 1.0);
    }
}
