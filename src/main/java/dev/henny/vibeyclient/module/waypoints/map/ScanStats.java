package dev.henny.vibeyclient.module.waypoints.map;

public final class ScanStats {
    private long chunks;
    private long ticks;
    private long nanos;
    private long worstNanos;

    void record(int samples, long tookNanos) {
        ticks++;
        chunks += samples;
        nanos += tookNanos;
        worstNanos = Math.max(worstNanos, tookNanos);
    }

    public long chunks() {
        return chunks;
    }

    public double averageMillisPerTick() {
        return ticks == 0 ? 0 : nanos / 1e6 / ticks;
    }

    public double worstMillis() {
        return worstNanos / 1e6;
    }

    public double millisPerChunk() {
        return chunks == 0 ? 0 : nanos / 1e6 / chunks;
    }
}
