package dev.henny.vibeyclient.module.visuals.core;

public final class ItemTumble {
    public static final float TWO_PI = (float) (Math.PI * 2);
    public static final float BASE_SPIN = 1.5f;
    public static final float SPIN_PER_SPEED = 20f;
    public static final float MAX_SPIN = 9f;
    public static final float FLUID_FACTOR = 0.25f;
    public static final float SETTLE_RATE = 14f;
    public static final float MAX_DT = 0.1f;

    private float angle;
    private float yaw;
    private boolean settled;

    public ItemTumble(int seed) {
        yaw = (float) ((mix(seed) & 0xFFFF) / 65536.0 * TWO_PI);
    }

    public void step(float dt, boolean grounded, boolean inFluid, float speed, boolean flat) {
        float t = Math.max(0f, Math.min(MAX_DT, dt));
        if (!grounded) {
            float rate = Math.min(MAX_SPIN, BASE_SPIN + Math.max(0f, speed) * SPIN_PER_SPEED);
            if (inFluid) rate *= FLUID_FACTOR;
            angle = wrap(angle + rate * t);
            yaw = wrap(yaw + rate * 0.3f * t);
            settled = false;
            return;
        }
        float rest = restAngle(angle, flat);
        float k = (float) (1 - Math.exp(-SETTLE_RATE * t));
        angle += (rest - angle) * k;
        if (Math.abs(rest - angle) < 1e-3f) {
            angle = wrap(rest);
            settled = true;
        }
    }

    public float angle() {
        return angle;
    }

    public float yaw() {
        return yaw;
    }

    public boolean settled() {
        return settled;
    }

    public static float restAngle(float angle, boolean flat) {
        float step = flat ? (float) Math.PI : (float) (Math.PI / 2);
        return Math.round(angle / step) * step;
    }

    public static float wrap(float a) {
        float w = a % TWO_PI;
        return w < 0 ? w + TWO_PI : w;
    }

    private static int mix(int seed) {
        int h = seed * 0x9E3779B1;
        return h ^ (h >>> 15);
    }
}
