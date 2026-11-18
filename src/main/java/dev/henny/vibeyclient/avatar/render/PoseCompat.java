package dev.henny.vibeyclient.avatar.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

/** PoseStack calls whose signature changed (26.3: mulPose takes a matrix). Render thread only. */
public final class PoseCompat {
    private PoseCompat() {}

    //? if >=26.3 {
    /*private static final org.joml.Matrix4f M = new org.joml.Matrix4f();
    *///?}

    public static void rotate(PoseStack ps, Quaternionf q) {
        //? if >=26.3 {
        /*ps.mulPose(M.rotation(q));
        *///?} else
        ps.mulPose(q);
    }
}
