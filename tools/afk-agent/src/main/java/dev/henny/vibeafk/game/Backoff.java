package dev.henny.vibeafk.game;

public final class Backoff {
    private boolean active;
    private int attempt;
    private long dueAt;
    private int delayS;
    private String server;

    public boolean active() {
        return active;
    }

    public int attempt() {
        return attempt;
    }

    public int delayS() {
        return delayS;
    }

    public String server() {
        return server;
    }

    public static int delay(int attemptIndex, int baseS, int maxS) {
        long d = baseS;
        for (int k = 0; k < attemptIndex && d < maxS; k++) d *= 2;
        return (int) Math.min(maxS, d);
    }

    public boolean schedule(long now, String server, int maxAttempts, int baseS, int maxS) {
        if (!active) {
            active = true;
            attempt = 0;
            this.server = server;
        }
        if (attempt >= maxAttempts) {
            active = false;
            dueAt = 0;
            return false;
        }
        delayS = delay(attempt, baseS, maxS);
        dueAt = now + delayS * 1000L;
        return true;
    }

    public boolean due(long now) {
        if (!active || dueAt == 0 || now < dueAt) return false;
        attempt++;
        dueAt = 0;
        return true;
    }

    public boolean waiting() {
        return active && dueAt != 0;
    }

    public void reset() {
        active = false;
        attempt = 0;
        dueAt = 0;
        server = null;
    }
}
