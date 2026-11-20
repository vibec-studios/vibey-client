package dev.henny.vui.mc.host;

final class FrameClock {
    private static final float MAX_STEP = 0.25f;

    private long last;

    float tick() {
        long now = System.nanoTime();
        if (last == 0) {
            last = now;
            return 0f;
        }
        float dt = (now - last) / 1_000_000_000f;
        last = now;
        return Math.max(0f, Math.min(MAX_STEP, dt));
    }
}
