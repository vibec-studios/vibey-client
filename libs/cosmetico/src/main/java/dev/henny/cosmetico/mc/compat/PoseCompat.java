package dev.henny.cosmetico.mc.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

public final class PoseCompat {
    private PoseCompat() {}

    //? if >=26.3 {
    /*private static final org.joml.Matrix4f M = new org.joml.Matrix4f();
    *///?}

    public static void unwind(PoseStack ps, PoseStack.Pose saved) {
        for (int i = 0; i < 64 && ps.last() != saved && !empty(ps); i++) ps.popPose();
    }

    private static boolean empty(PoseStack ps) {
        //? if >=1.21.5 {
        return ps.isEmpty();
        //?} else
        /*return ps.clear();*/
    }

    public static void rotate(PoseStack ps, Quaternionf q) {
        //? if >=26.3 {
        /*ps.mulPose(M.rotation(q));
        *///?} else
        ps.mulPose(q);
    }
}
