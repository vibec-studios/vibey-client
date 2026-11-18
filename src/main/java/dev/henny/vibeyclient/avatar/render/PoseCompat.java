package dev.henny.vibeyclient.avatar.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

/** PoseStack calls whose signature changed (26.3: mulPose takes a matrix). Render thread only. */
public final class PoseCompat {
    private PoseCompat() {}

    //? if >=26.3 {
    /*private static final org.joml.Matrix4f M = new org.joml.Matrix4f();
    *///?}

    /**
     * After an exception in the middle of our drawing: pops everything we pushed since {@code saved} was on top, so
     * vanilla does not crash with "Pose stack not empty" at the end of the frame.
     */
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
