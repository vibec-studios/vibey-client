package dev.henny.vibeyclient.avatar.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.avatar.bundle.Bundle.PlayerBone;
import net.minecraft.client.model.HumanoidModel;

/** {@link PlayerParts} of a vanilla humanoid/player model (reused, set per call). */
public final class ModelParts implements PlayerParts {
    private HumanoidModel<?> model;

    public ModelParts set(HumanoidModel<?> model) {
        this.model = model;
        return this;
    }

    @Override
    public void apply(PlayerBone part, PoseStack ps) {
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
