package dev.henny.r3.mc.client;

final class FrameClock {
    static final float MAX_DT = 0.25f;
    private long last;

    float next() {
        long now = System.nanoTime();
        float dt = last == 0 ? 0f : (now - last) / 1_000_000_000f;
        last = now;
        return Math.max(0f, Math.min(MAX_DT, dt));
    }
}
