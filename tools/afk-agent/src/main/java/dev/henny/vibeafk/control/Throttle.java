package dev.henny.vibeafk.control;

public final class Throttle {
    private volatile boolean on;
    private volatile int fps = 2;
    private long next;
    private volatile long frames;

    public void set(boolean enabled, int targetFps) {
        fps = Math.max(1, Math.min(60, targetFps));
        on = enabled;
    }

    public boolean on() {
        return on;
    }

    public int fps() {
        return fps;
    }

    public long frames() {
        return frames;
    }

    public void frame() {
        frames++;
        if (!on) {
            next = 0;
            return;
        }
        long interval = 1_000_000_000L / fps;
        long now = System.nanoTime();
        if (next == 0 || now - next > interval) next = now;
        long wait = next - now;
        if (wait > 0) {
            try {
                Thread.sleep(wait / 1_000_000L, (int) (wait % 1_000_000L));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        next += interval;
    }
}
