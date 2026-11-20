package dev.henny.cosmetico.mc.player;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.anim.EmotePlay;
import dev.henny.cosmetico.draw.EmoteVisibility;
import dev.henny.cosmetico.model.PlayerBone;
import dev.henny.cosmetico.pose.EmotePartPose;
import dev.henny.cosmetico.pose.PartPose;
import dev.henny.cosmetico.state.Avatar;
import net.minecraft.client.model.geom.ModelPart;

public final class EmotePoseHook {
    private EmotePoseHook() {}

    private static final PartPose POSE = new PartPose();

    public static EmotePlay emote(Avatar a) {
        return a == null ? null : EmoteVisibility.visible(a, Cosmetico.config());
    }

    public static void apply(ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg,
                             ModelPart leftLeg, Avatar a) {
        EmotePlay e = emote(a);
        if (!EmotePartPose.active(e)) return;
        part(head, PlayerBone.HEAD, e);
        part(body, PlayerBone.BODY, e);
        part(rightArm, PlayerBone.RIGHT_ARM, e);
        part(leftArm, PlayerBone.LEFT_ARM, e);
        part(rightLeg, PlayerBone.RIGHT_LEG, e);
        part(leftLeg, PlayerBone.LEFT_LEG, e);
    }

    private static void part(ModelPart part, PlayerBone b, EmotePlay e) {
        if (e.channels[b.ordinal()] == 0) return;
        POSE.set(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot, part.xScale, part.yScale, part.zScale);
        EmotePartPose.blend(e, b, POSE);
        part.x = POSE.x;
        part.y = POSE.y;
        part.z = POSE.z;
        part.xRot = POSE.xRot;
        part.yRot = POSE.yRot;
        part.zRot = POSE.zRot;
        part.xScale = POSE.xScale;
        part.yScale = POSE.yScale;
        part.zScale = POSE.zScale;
    }
}
