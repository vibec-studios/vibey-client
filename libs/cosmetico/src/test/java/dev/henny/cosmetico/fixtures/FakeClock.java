package dev.henny.cosmetico.fixtures;

import dev.henny.cosmetico.anim.Clock;
import dev.henny.cosmetico.anim.TimeSource;

public final class FakeClock implements TimeSource {
    private long nanos;

    public Clock clock() {
        return new Clock(this);
    }

    public void advance(double seconds) {
        nanos += (long) (seconds * 1e9);
    }

    @Override
    public long nanos() {
        return nanos;
    }
}
