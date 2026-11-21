package dev.henny.vibeafk;

public final class RateLimiter {
    private final double capacity;
    private final double perMs;
    private double tokens;
    private long last;

    public RateLimiter(int burst, long intervalMs, long now) {
        this.capacity = burst;
        this.perMs = 1.0 / intervalMs;
        this.tokens = burst;
        this.last = now;
    }

    public boolean tryAcquire(long now) {
        if (now > last) {
            tokens = Math.min(capacity, tokens + (now - last) * perMs);
            last = now;
        }
        if (tokens >= 1.0 - 1e-9) {
            tokens = Math.max(0, tokens - 1.0);
            return true;
        }
        return false;
    }
}
