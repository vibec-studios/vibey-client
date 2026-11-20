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
        ScreenSession s = Sessions.usable(screen);
        if (s == null) return false;
        ScreenLayout l = s.layout();
        if (l.blur() > 0) g.blurBeforeThisStratum();
        boolean shifted = s.offsetX() != 0 || s.offsetY() != 0;
        if (l.dim() == ScreenLayout.VANILLA && !shifted) return false;
        if (l.dim() == ScreenLayout.VANILLA) fill(s, g, VANILLA_TOP, VANILLA_BOTTOM);
        else fill(s, g, l.dim());
        return true;
    }

    public static boolean menuBackground(Screen screen, GuiGraphics g) {
        ScreenSession s = Sessions.usable(screen);
        if (s == null || s.layout().dim() == ScreenLayout.VANILLA) return false;
        fill(s, g, s.layout().dim());
        return true;
    }

    private static void fill(ScreenSession s, GuiGraphics g, int dim) {
        int a = Math.round(dim * 2.55f);
        if (a > 0) fill(s, g, (Math.max(0, a - 16) << 24) | 0x101010, (a << 24) | 0x101010);
    }

    private static void fill(ScreenSession s, GuiGraphics g, int top, int bottom) {
        int x = Math.round(-s.offsetX()), y = Math.round(-s.offsetY());
        g.fillGradient(x, y, x + s.width(), y + s.height(), top, bottom);
    }

    private static final int VANILLA_TOP = 0xC0101010;
    private static final int VANILLA_BOTTOM = 0xD0101010;

    private static ScreenLayout layout(Screen screen) {
        ScreenSession s = Sessions.usable(screen);
        return s == null ? null : s.layout();
    }
}
