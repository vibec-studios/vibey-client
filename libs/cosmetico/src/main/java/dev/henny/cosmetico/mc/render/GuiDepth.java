package dev.henny.cosmetico.mc.render;

public final class GuiDepth {
    private GuiDepth() {}

    private static int depth;

    public static void enter() {
        depth++;
    }

    public static void leave() {
        depth = Math.max(0, depth - 1);
    }

    public static boolean inGui() {
        return depth > 0;
    }
}
