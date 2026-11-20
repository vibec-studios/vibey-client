package dev.henny.vibeyclient.module.screens.hook;

import dev.henny.vibeyclient.module.screens.apply.Guard;
import dev.henny.vibeyclient.module.screens.apply.LayoutApplier;
import dev.henny.vibeyclient.module.screens.apply.ScreenSession;
import dev.henny.vibeyclient.module.screens.apply.Sessions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

public final class ScreenFrame {
    private static GuiGraphics pushed;

    private ScreenFrame() {}

    public static float scale(Screen screen) {
        ScreenSession s = Sessions.usable(screen);
        return s == null ? 1f : s.layout().scale();
    }

    public static float current() {
        return scale(Minecraft.getInstance().screen);
    }

    public static int virtual(Screen screen, int size, boolean first) {
        ScreenSession s = Sessions.usable(screen);
        if (s == null) return size;
        if (first) s.scale(s.layout().scale());
        float k = s.scale();
        return k == 1f ? size : Math.max(1, Math.round(size / k));
    }

    public static GuiGraphics begin(GuiGraphics g) {
        Screen screen = Minecraft.getInstance().screen;
        ScreenSession s = Sessions.usable(screen);
        ScreenStyle.beginFrame(s);
        if (s == null) return g;
        if (LayoutApplier.panelMoved(s)) Sessions.reapply(s);
        float k = s.scale();
        if (k == 1f) return g;
        g.pose().pushMatrix();
        g.pose().scale(k, k);
        pushed = g;
        return g;
    }

    public static int mouse(int v) {
        float k = activeScale();
        return k == 1f ? v : (int) Math.floor(v / k);
    }

    public static void end() {
        ScreenStyle.endFrame();
        if (pushed == null) return;
        try {
            pushed.pose().popMatrix();
        } catch (RuntimeException e) {
            Guard.fail(Sessions.peek(Minecraft.getInstance().screen), e);
        }
        pushed = null;
    }

    public static double pos(double v) {
        float k = activeScale();
        return k == 1f ? v : v / k;
    }

    public static MouseButtonEvent event(MouseButtonEvent e) {
        float k = activeScale();
        return k == 1f ? e : new MouseButtonEvent(e.x() / k, e.y() / k, e.buttonInfo());
    }

    private static float activeScale() {
        ScreenSession s = Sessions.usable(Minecraft.getInstance().screen);
        return s == null ? 1f : s.scale();
    }
}
