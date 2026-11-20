package dev.henny.vui.mc.compat;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

public final class GuiScale {
    private static boolean overridden;

    private GuiScale() {
    }

    public static void apply(int wanted) {
        Minecraft mc = Minecraft.getInstance();
        Window w = mc.getWindow();
        if (wanted <= 0) {
            restore();
            return;
        }
        int max = Math.max(1, w.calculateScale(0, mc.isEnforceUnicode()));
        int scale = Math.min(wanted, max);
        if (scale == Mc.guiScale()) return;
        set(w, scale);
        overridden = true;
    }

    public static void restore() {
        if (!overridden) return;
        overridden = false;
        Minecraft mc = Minecraft.getInstance();
        Window w = mc.getWindow();
        set(w, w.calculateScale(mc.options.guiScale().get(), mc.isEnforceUnicode()));
    }

    private static void set(Window w, int scale) {
        //? if >=1.21.6 {
        w.setGuiScale(scale);
        //?} else
        /*w.setGuiScale((double) scale);*/
    }
}
