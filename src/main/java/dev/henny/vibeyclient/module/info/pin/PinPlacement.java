package dev.henny.vibeyclient.module.info.pin;

import dev.henny.r3.anchor.EntityAnchor;
import dev.henny.r3.anchor.Facing;
import dev.henny.r3.math.Axes;
import dev.henny.r3.object.WorldObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public final class PinPlacement {
    public static final float WIDTH = 1.5f, HEIGHT = 0.5f, PIXELS_PER_BLOCK = 96f;
    private static final double LIFT = 1.1, OUT = 0.3;

    private PinPlacement() {}

    public static void atBlock(WorldObject card, BlockPos pos, Direction face, Vec3 eye) {
        double x = pos.getX() + 0.5 + face.getStepX() * OUT;
        double z = pos.getZ() + 0.5 + face.getStepZ() * OUT;
        double y = pos.getY() + 0.5 + (face == Direction.DOWN ? -LIFT : LIFT);
        card.transform().position(x, y, z);
        Axes.facing(eye.x - x, 0, eye.z - z, card.transform().orientation());
    }

    public static void onEntity(WorldObject card, Entity e) {
        card.anchor(new EntityAnchor(e.getUUID(), new Vector3d(0, 0.25 + HEIGHT / 2, 0), true, Facing.CAMERA_YAW));
    }
}
