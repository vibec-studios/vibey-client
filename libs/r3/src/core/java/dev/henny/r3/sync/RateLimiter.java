package dev.henny.r3.sync;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

public final class RateLimiter {
    private final double burst;
    private final double perSecond;
    private final LongSupplier clockMs;
    private final Map<UUID, double[]> buckets = new HashMap<>();

    public RateLimiter(double burst, double perSecond, LongSupplier clockMs) {
        this.burst = burst;
        this.perSecond = perSecond;
        this.clockMs = clockMs;
    }

    public boolean allow(UUID sender) {
        long now = clockMs.getAsLong();
        double[] b = buckets.computeIfAbsent(sender, k -> new double[] {burst, now});
        b[0] = Math.min(burst, b[0] + (now - b[1]) / 1000.0 * perSecond);
        b[1] = now;
        if (b[0] < 1) return false;
        b[0] -= 1;
        return true;
    }

    public void forget(UUID sender) {
        buckets.remove(sender);
    }

    public void clear() {
        buckets.clear();
    }
}
