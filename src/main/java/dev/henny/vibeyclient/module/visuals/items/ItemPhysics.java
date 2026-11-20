package dev.henny.vibeyclient.module.visuals.items;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.henny.vibeyclient.module.visuals.VisualsModule;
import dev.henny.vibeyclient.module.visuals.core.ItemLift;
import dev.henny.vibeyclient.module.visuals.core.ItemTumble;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

public final class ItemPhysics {
    private static final float QUARTER = (float) (Math.PI / 2);

    private ItemPhysics() {}

    public static void extract(ItemEntity entity, ItemEntityRenderState state) {
        ItemPose pose = (ItemPose) state;
        VisualsModule m = VisualsModule.get();
        if (m == null || !m.itemPhysicsActive() || state.item.isEmpty()) {
            pose.vibey$pose(false, 0, 0, 0, 0, 0);
            return;
        }
        AABB box = state.item.getModelBoundingBox();
        float height = (float) box.getYsize(), depth = (float) box.getZsize();
        boolean flat = ItemLift.flat(depth);
        boolean fluid = entity.isInWater() || entity.isInLava();
        float speed = (float) entity.getDeltaMovement().length();
        ItemTumble t = ((ItemMotionHolder) entity).vibey$motion().step(entity.onGround(), fluid, speed, flat);
        float rotX = flat ? t.angle() - QUARTER : t.angle();
        float lift = ItemLift.lift(rotX, height, ItemLift.stackDepth(depth, state.count));
        pose.vibey$pose(true, t.yaw(), rotX, lift, (float) ((box.minY + box.maxY) / 2), (float) ((box.minZ + box.maxZ) / 2));
    }

    public static boolean active(ItemEntityRenderState state) {
        return ((ItemPose) state).vibey$active();
    }

    public static float lift(ItemEntityRenderState state) {
        return ((ItemPose) state).vibey$lift();
    }

    public static void rotate(PoseStack stack, ItemEntityRenderState state) {
        ItemPose p = (ItemPose) state;
        stack.mulPose(Axis.YP.rotation(p.vibey$yaw()));
        stack.mulPose(Axis.XP.rotation(p.vibey$rotX()));
        stack.translate(0f, -p.vibey$centerY(), -p.vibey$centerZ());
    }
}
