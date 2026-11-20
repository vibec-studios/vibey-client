package dev.henny.vibeyclient.module.visuals.items;

import dev.henny.vibeyclient.module.visuals.core.ItemTumble;

public final class ItemMotion {
    private final ItemTumble tumble;
    private long lastNanos;

    public ItemMotion(int seed) {
        tumble = new ItemTumble(seed);
    }

    public ItemTumble tumble() {
        return tumble;
    }

    ItemTumble step(boolean grounded, boolean inFluid, float speed, boolean flat) {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : (now - lastNanos) / 1e9f;
        lastNanos = now;
        tumble.step(dt, grounded, inFluid, speed, flat);
        return tumble;
    }
}
