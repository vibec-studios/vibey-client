package dev.henny.vibeyclient.module.visuals.items.held;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.henny.vibeyclient.module.visuals.VisualsModule;
import dev.henny.vibeyclient.module.visuals.items.ItemRules;
import dev.henny.vibeyclient.module.visuals.items.core.EditSession;
import dev.henny.vibeyclient.module.visuals.items.core.HandPose;
import dev.henny.vibeyclient.module.visuals.items.core.HandProjection;
import dev.henny.vibeyclient.module.visuals.items.edit.ItemEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;

public final class HeldRender {
    private HeldRender() {}

    public static void translate(PoseStack stack, HumanoidArm arm) {
        HandPose p = pose(arm);
        if (p == null || !p.moves()) return;
        stack.translate(side(arm) * p.x(), p.y(), p.z());
    }

    public static void transform(PoseStack stack, LivingEntity entity, ItemDisplayContext context) {
        if (context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND) return;
        if (entity != Minecraft.getInstance().player) return;
        HumanoidArm arm = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        HandPose p = pose(arm);
        if (p == null || !p.turnsOrScales()) return;
        int side = side(arm);
        float px = side * HandProjection.PIVOT_X, py = HandProjection.PIVOT_Y, pz = HandProjection.PIVOT_Z;
        stack.translate(px, py, pz);
        stack.mulPose(Axis.XP.rotationDegrees(p.rotX()));
        stack.mulPose(Axis.YP.rotationDegrees(side * p.rotY()));
        stack.mulPose(Axis.ZP.rotationDegrees(side * p.rotZ()));
        stack.scale(p.scale(), p.scale(), p.scale());
        stack.translate(-px, -py, -pz);
    }

    static HandPose pose(HumanoidArm arm) {
        VisualsModule m = VisualsModule.get();
        var player = Minecraft.getInstance().player;
        if (m == null || !m.isEnabled() || player == null || ItemRules.heldBlocked()) return null;
        boolean main = arm == player.getMainArm();
        EditSession s = ItemEditor.session();
        if (s != null) return s.pose(main ? EditSession.MAIN : EditSession.OFF);
        return m.held.of(main).pose();
    }

    public static int side(HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? 1 : -1;
    }
}
