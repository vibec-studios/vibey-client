package dev.henny.vibeyclient.module.screens.hook;

import dev.henny.vibeyclient.module.screens.apply.ScreenSession;
import dev.henny.vibeyclient.module.screens.apply.Sessions;
import dev.henny.vibeyclient.module.screens.core.ScreenLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ScreenStyle {
    private static Component hiddenTitle;

    private ScreenStyle() {}

    static void beginFrame(ScreenSession s) {
        hiddenTitle = s != null && s.layout().hideTitle() ? s.screen.getTitle() : null;
    }

    static void endFrame() {
        hiddenTitle = null;
    }

    public static boolean hideText(Component c) {
        return c != null && c == hiddenTitle;
    }

    public static int blur(int vanilla) {
        ScreenLayout l = layout(Minecraft.getInstance().screen);
        return l == null || l.blur() == ScreenLayout.VANILLA ? vanilla : l.blur();
    }

    public static boolean transparentBackground(Screen screen, GuiGraphics g) {
        ScreenLayout l = layout(screen);
        if (l == null) return false;
        if (l.blur() > 0) g.blurBeforeThisStratum();
        if (l.dim() == ScreenLayout.VANILLA) return false;
        fill(screen, g, l.dim());
        return true;
    }

    public static boolean menuBackground(Screen screen, GuiGraphics g) {
        ScreenLayout l = layout(screen);
        if (l == null || l.dim() == ScreenLayout.VANILLA) return false;
        fill(screen, g, l.dim());
        return true;
    }

    private static void fill(Screen screen, GuiGraphics g, int dim) {
        int a = Math.round(dim * 2.55f);
        if (a <= 0) return;
        int top = (Math.max(0, a - 16) << 24) | 0x101010;
        int bottom = (a << 24) | 0x101010;
        g.fillGradient(0, 0, screen.width, screen.height, top, bottom);
    }

    private static ScreenLayout layout(Screen screen) {
        ScreenSession s = Sessions.usable(screen);
        return s == null ? null : s.layout();
    }
}
