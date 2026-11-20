package dev.henny.vibeyclient.module.servers.core;

public final class Durations {
    public enum Unit { MINUTES, HOURS, DAYS }

    public record Span(int amount, Unit unit) {}

    private static final long MINUTE = 60_000L, HOUR = 60 * MINUTE, DAY = 24 * HOUR;

    private Durations() {}

    public static Span of(long ms) {
        long v = Math.max(0, ms);
        if (v < HOUR) return new Span((int) Math.max(1, (v + MINUTE - 1) / MINUTE), Unit.MINUTES);
        if (v < 2 * DAY) return new Span((int) (v / HOUR), Unit.HOURS);
        return new Span((int) (v / DAY), Unit.DAYS);
    }

    public static Span since(long startedAt, long now) {
        return startedAt <= 0 ? null : of(now - startedAt);
    }

    public static Span left(long expiresAt, long now) {
        return expiresAt <= 0 ? null : of(expiresAt - now);
    }
}
