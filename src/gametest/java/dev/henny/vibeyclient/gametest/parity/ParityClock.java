package dev.henny.vibeyclient.gametest.parity;

public final class ParityClock {
    private ParityClock() {}

    private static volatile ParityEngine engine;
    private static volatile long ticks;

    public static void start(ParityEngine e, long atTick) {
        ticks = atTick;
        engine = e;
        pin();
    }

    public static void stop() {
        ParityEngine e = engine;
        engine = null;
        if (e != null) e.pin(Double.NaN);
    }

    public static double now() {
        return engine == null ? Double.NaN : ticks / 20.0;
    }

    public static void onTick() {
        if (engine == null) return;
        ticks++;
        engine.tick(ticks);
        pin();
    }

    public static void pin() {
        ParityEngine e = engine;
        if (e != null) e.pin(ticks / 20.0);
    }
}
