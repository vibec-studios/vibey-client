package dev.henny.r3.mc.world;

import dev.henny.r3.anchor.BlockHit;
import dev.henny.r3.math.Face;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;

public final class BlockRaycast {
    private BlockRaycast() {}

    public static boolean cast(ClientLevel level, Entity viewer, Vector3dc origin, Vector3dc dir, double max, BlockHit out) {
        Vec3 from = new Vec3(origin.x(), origin.y(), origin.z());
        Vec3 to = from.add(dir.x() * max, dir.y() * max, dir.z() * max);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return false;
        Vec3 p = hit.getLocation();
        BlockPos b = hit.getBlockPos();
        out.set(p.x, p.y, p.z, face(hit.getDirection()), b.getX(), b.getY(), b.getZ(), p.distanceTo(from));
        return true;
    }

    public static Face face(Direction d) {
        return switch (d) {
            case DOWN -> Face.DOWN;
            case UP -> Face.UP;
            case NORTH -> Face.NORTH;
            case SOUTH -> Face.SOUTH;
            case WEST -> Face.WEST;
            case EAST -> Face.EAST;
        };
    }
}
