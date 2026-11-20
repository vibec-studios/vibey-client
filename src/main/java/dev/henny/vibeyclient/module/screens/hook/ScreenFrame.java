package dev.henny.vibeyclient.module.screens.hook;

import dev.henny.vibeyclient.module.screens.apply.Guard;
import dev.henny.vibeyclient.module.screens.apply.ScreenSession;
import dev.henny.vibeyclient.module.screens.apply.Sessions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

public final class ScreenFrame {
    private static GuiGraphics pushed;

    private ScreenFrame() {}

    public static int virtual(Screen screen, int size, boolean first) {
        ScreenSession s = Sessions.usable(screen);
        if (s == null) return size;
        if (first) s.scale(s.layout().scale());
        float k = s.scale();
        return k == 1f ? size : Math.max(1, Math.round(size / k));
    }

    public static GuiGraphics begin(GuiGraphics g) {
        ScreenSession s = Sessions.usable(Minecraft.getInstance().screen);
        ScreenStyle.beginFrame(s);
        if (s == null || plain(s)) return g;
        g.pose().pushMatrix();
        g.pose().scale(s.scale(), s.scale());
        g.pose().translate(s.offsetX(), s.offsetY());
        pushed = g;
        return g;
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

    static boolean transforming(GuiGraphics g) {
        return pushed != null && pushed == g;
    }

    public static void suspendCurrent() {
        suspend(pushed);
    }

    public static void resumeCurrent() {
        resume(pushed);
    }

    public static void suspend(Object graphics) {
        ScreenSession s = active();
        if (pushed == null || graphics != pushed || s == null) return;
        pushed.pose().pushMatrix();
        pushed.pose().translate(-s.offsetX(), -s.offsetY());
        pushed.pose().scale(1f / s.scale(), 1f / s.scale());
    }

    public static void resume(Object graphics) {
        if (pushed == null || graphics != pushed || active() == null) return;
        pushed.pose().popMatrix();
    }

    public static double realX(double v) {
        ScreenSession s = active();
        return s == null ? v : (v + s.offsetX()) * s.scale();
    }

    public static double realY(double v) {
        ScreenSession s = active();
        return s == null ? v : (v + s.offsetY()) * s.scale();
    }

    public static int mouseX(int v) {
        return (int) Math.floor(x(v));
    }

    public static int mouseY(int v) {
        return (int) Math.floor(y(v));
    }

    public static double x(double v) {
        ScreenSession s = active();
        return s == null ? v : v / s.scale() - s.offsetX();
    }

    public static double y(double v) {
        ScreenSession s = active();
        return s == null ? v : v / s.scale() - s.offsetY();
    }

    public static double delta(double v) {
        ScreenSession s = active();
        return s == null ? v : v / s.scale();
    }

    public static MouseButtonEvent event(MouseButtonEvent e) {
        ScreenSession s = active();
        return s == null ? e : new MouseButtonEvent(x(e.x()), y(e.y()), e.buttonInfo());
    }

    private static boolean plain(ScreenSession s) {
        return s.scale() == 1f && s.offsetX() == 0 && s.offsetY() == 0;
    }

    private static ScreenSession active() {
        ScreenSession s = Sessions.usable(Minecraft.getInstance().screen);
        return s == null || plain(s) ? null : s;
    }
}
