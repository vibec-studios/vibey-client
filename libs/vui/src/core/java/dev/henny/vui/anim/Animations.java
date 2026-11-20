package dev.henny.vui.anim;

public final class Animations {
    private static volatile boolean enabled = true;
    private static volatile float speed = 1f;

    private Animations() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void setEnabled(boolean on) {
        enabled = on;
    }

    public static float speed() {
        return speed;
    }

    public static void setSpeed(float factor) {
        speed = Math.max(0.01f, factor);
    }
}
