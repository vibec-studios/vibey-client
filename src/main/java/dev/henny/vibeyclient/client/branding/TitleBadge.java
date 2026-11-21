package dev.henny.vibeyclient.client.branding;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.toast.ToastHooks;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

public final class TitleBadge {
    private static final int X = 2;
    private static final int LOGO = 9;
    private static final int GAP = 2;
    private static final int COLOR = 0xFFD8D4F0;
    private static boolean installed;

    private TitleBadge() {}

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        ToastHooks.addScreenOverlay((screen, g, mx, my) -> {
            if (screen instanceof TitleScreen) render(screen, g);
        });
    }

    public static String label() {
        return WindowTitle.NAME + " " + BuildInfo.VERSION;
    }

    static void render(Screen screen, Gfx g) {
        String text = label();
        int line = g.lineHeight() + 1;
        int y = screen.height - 10 - VanillaBrandLines.count() * line;
        int w = LOGO + GAP + g.width(text);
        if (!free(screen, X, y - 1, w, LOGO + 1)) return;
        g.image(Compat.id("icons/icon_32x32.png"), X, y - 1, LOGO, LOGO, 32);
        g.textShadow(text, X + LOGO + GAP, y, COLOR);
    }

    private static boolean free(Screen screen, int x, int y, int w, int h) {
        for (GuiEventListener l : screen.children()) {
            if (!(l instanceof LayoutElement e) || (l instanceof AbstractWidget aw && !aw.visible)) continue;
            if (e.getWidth() <= 0 || e.getHeight() <= 0) continue;
            if (e.getX() < x + w && e.getX() + e.getWidth() > x && e.getY() < y + h && e.getY() + e.getHeight() > y) return false;
        }
        return true;
    }
}
