package dev.henny.vibeyclient.avatar.anim;

/** Animation clock in seconds (monotonic, frame accurate, keeps running in menus). */
public final class Clock {
    private Clock() {}

    private static final long START = System.nanoTime();

    public static double now() {
        return (System.nanoTime() - START) / 1e9;
    }
}
