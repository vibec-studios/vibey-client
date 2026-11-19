package dev.henny.cosmetico.anim;

public final class Clock {
    private final TimeSource source;
    private final long start;

    public Clock(TimeSource source) {
        this.source = source;
        this.start = source.nanos();
    }

    public static Clock system() {
        return new Clock(TimeSource.SYSTEM);
    }

    public double now() {
        return (source.nanos() - start) / 1e9;
    }
}
