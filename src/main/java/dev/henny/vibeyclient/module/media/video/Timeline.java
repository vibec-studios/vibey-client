package dev.henny.vibeyclient.module.media.video;

public final class Timeline {
    private boolean playing;
    private long baseMs;
    private long baseWall;
    private long durationMs = -1;

    public boolean playing() {
        return playing;
    }

    public long durationMs() {
        return durationMs;
    }

    public void durationMs(long ms) {
        durationMs = ms;
    }

    public long expectedMs(boolean loop) {
        long t = playing ? baseMs + (System.currentTimeMillis() - baseWall) : baseMs;
        if (playing && loop && durationMs > 1000) t %= durationMs;
        return Math.max(0, t);
    }

    public void set(boolean playing, long positionMs) {
        this.playing = playing;
        this.baseMs = Math.max(0, positionMs);
        this.baseWall = System.currentTimeMillis();
    }
}
