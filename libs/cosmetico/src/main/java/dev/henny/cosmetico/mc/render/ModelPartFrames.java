package dev.henny.cosmetico.mc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.cosmetico.model.PlayerBone;
import dev.henny.cosmetico.skeleton.PartFrames;
import net.minecraft.client.model.HumanoidModel;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class ModelPartFrames implements PartFrames {
    private final PoseStack scratch = new PoseStack();
    private HumanoidModel<?> model;

    public ModelPartFrames set(HumanoidModel<?> model) {
        this.model = model;
        return this;
    }

    @Override
    public void apply(PlayerBone part, Matrix4f pose, Matrix3f normal) {
        scratch.pushPose();
        scratch.last().pose().set(pose);
        scratch.last().normal().set(normal);
        applyTo(part, scratch);
        pose.set(scratch.last().pose());
        normal.set(scratch.last().normal());
        scratch.popPose();
    }

    public void applyTo(PlayerBone part, PoseStack ps) {
        //? if >=1.21.2
        model.root().translateAndRotate(ps);
        switch (part) {
            case HEAD -> model.head.translateAndRotate(ps);
            case BODY -> model.body.translateAndRotate(ps);
            case RIGHT_ARM -> model.rightArm.translateAndRotate(ps);
            case LEFT_ARM -> model.leftArm.translateAndRotate(ps);
            case RIGHT_LEG -> model.rightLeg.translateAndRotate(ps);
            case LEFT_LEG -> model.leftLeg.translateAndRotate(ps);
            default -> {}
        }
    }
}
