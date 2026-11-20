package dev.henny.vibeyclient.module.visuals.items.swing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.henny.vibeyclient.module.visuals.VisualsModule;
import dev.henny.vibeyclient.module.visuals.items.ItemRules;
import dev.henny.vibeyclient.module.visuals.items.core.SwingShape;
import dev.henny.vibeyclient.module.visuals.items.core.SwingStyle;
import dev.henny.vibeyclient.module.visuals.items.core.SwingTiming;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class SwingRender {
    private SwingRender() {}

    private static SwingSettings settings() {
        VisualsModule m = VisualsModule.get();
        return m == null || !m.isEnabled() ? null : m.swing;
    }

    public static SwingStyle style() {
        SwingSettings s = settings();
        return s == null ? SwingStyle.VANILLA : s.style.get();
    }

    public static void apply(PoseStack stack, SwingStyle style, float progress, int side) {
        SwingShape s = SwingShape.of(style, progress, side);
        stack.translate(s.tx(), s.ty(), s.tz());
        stack.mulPose(Axis.YP.rotationDegrees(s.yawIn()));
        stack.mulPose(Axis.ZP.rotationDegrees(s.roll()));
        stack.mulPose(Axis.XP.rotationDegrees(s.pitch()));
        stack.mulPose(Axis.YP.rotationDegrees(s.yawOut()));
    }

    public static float equip(float progress) {
        SwingSettings s = settings();
        return s != null && s.noEquipBob.on() ? 0f : progress;
    }

    public static boolean instantSwap() {
        SwingSettings s = settings();
        return s != null && s.noEquipBob.on();
    }

    public static float swapScale(float vanilla) {
        SwingSettings s = settings();
        if (s == null) return vanilla;
        return s.noEquipBob.on() || s.style.get().keepsItemUp() ? 1f : vanilla;
    }

    public static boolean hideEmptyHand() {
        SwingSettings s = settings();
        return s != null && s.hideEmptyHand.on() && !ItemRules.heldBlocked();
    }

    public static int duration(LivingEntity entity, int vanilla) {
        if (!(entity instanceof LocalPlayer)) return vanilla;
        SwingSettings s = settings();
        return s == null ? vanilla : SwingTiming.duration(vanilla, s.speed.get());
    }
}
