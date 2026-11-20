package dev.henny.vibeyclient.module.cinematic.run;

import net.minecraft.client.Minecraft;

public final class InputGate {
    static final long GRACE_MS = 600;
    static final double MOVE_PX = 4;

    private static long armedAt;
    private static double startX = Double.NaN;
    private static double startY = Double.NaN;
    private static double lastX = Double.NaN;
    private static double lastY = Double.NaN;

    private InputGate() {}

    static void armed(Minecraft mc) {
        armedAt = System.currentTimeMillis();
        startX = mc.mouseHandler.xpos();
        startY = mc.mouseHandler.ypos();
    }

    public static boolean key(int action) {
        AfkWatch.touch();
        return action == 1 && end();
    }

    public static boolean button(int action) {
        AfkWatch.touch();
        return action == 1 && end();
    }

    public static boolean scroll() {
        AfkWatch.touch();
        return end();
    }

    public static void move(double x, double y) {
        if (!Double.isNaN(lastX) && Math.abs(x - lastX) + Math.abs(y - lastY) > 1) AfkWatch.touch();
        lastX = x;
        lastY = y;
        if (Double.isNaN(startX) || System.currentTimeMillis() - armedAt < GRACE_MS) {
            startX = x;
            startY = y;
            return;
        }
        if (Math.abs(x - startX) + Math.abs(y - startY) > MOVE_PX) end();
    }

    public static boolean holdView() {
        return Cinematic.active() && !Cinematic.preview();
    }

    private static boolean end() {
        if (!Cinematic.running() || Cinematic.preview()) return false;
        if (System.currentTimeMillis() - armedAt < GRACE_MS) return false;
        Cinematic.stop();
        return true;
    }
}
