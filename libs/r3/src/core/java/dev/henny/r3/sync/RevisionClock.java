package dev.henny.r3.sync;

import java.util.function.LongSupplier;

public final class RevisionClock {
    public static final long TICKS_PER_MS = 1024;
    private final LongSupplier clockMs;
    private long last;

    public RevisionClock(LongSupplier clockMs) {
        this.clockMs = clockMs;
    }

    public long next(long previous) {
        long candidate = Math.max(Math.max(previous, last) + 1, clockMs.getAsLong() * TICKS_PER_MS);
        last = candidate;
        return candidate;
    }
}
