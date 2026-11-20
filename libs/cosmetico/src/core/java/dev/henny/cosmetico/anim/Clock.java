package dev.henny.cosmetico.anim;

public final class Clock {
    private final TimeSource source;
    private final long start;
    private volatile double pinned = Double.NaN;

    public Clock(TimeSource source) {
        this.source = source;
        this.start = source.nanos();
    }

    public static Clock system() {
        return new Clock(TimeSource.SYSTEM);
    }

    public double now() {
        double p = pinned;
        return Double.isNaN(p) ? (source.nanos() - start) / 1e9 : p;
    }

    public void pin(double seconds) {
        pinned = seconds;
    }

    public boolean pinned() {
        return !Double.isNaN(pinned);
    }
}
