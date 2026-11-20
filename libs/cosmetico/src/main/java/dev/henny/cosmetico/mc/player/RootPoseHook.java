package dev.henny.cosmetico.mc.player;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.cosmetico.mc.compat.PoseCompat;
import dev.henny.cosmetico.pose.HeldItems;
import dev.henny.cosmetico.pose.RootPose;
import dev.henny.cosmetico.state.Avatar;

public final class RootPoseHook {
    private RootPoseHook() {}

    private static final RootPose ROOT = new RootPose();

    public static void apply(Avatar a, PoseStack ps, float modelScale) {
        if (!ROOT.compute(EmotePoseHook.emote(a), modelScale)) return;
        if (ROOT.translate) ps.translate(ROOT.tx, ROOT.ty, ROOT.tz);
        if (ROOT.rotate) PoseCompat.rotate(ps, ROOT.rotation);
        if (ROOT.scale) ps.scale(ROOT.sx, ROOT.sy, ROOT.sz);
    }

    public static boolean hideHeldItems(Avatar a) {
        return HeldItems.hide(EmotePoseHook.emote(a));
    }
}
