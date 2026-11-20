package dev.henny.vibeyclient.module.visuals.items.core;

public record HandPose(float x, float y, float z, float rotX, float rotY, float rotZ, float scale) {
    public static final float MAX_OFFSET = 1f;
    public static final float MAX_ROTATION = 180f;
    public static final float MIN_SCALE = 0.1f;
    public static final float MAX_SCALE = 2.5f;
    public static final HandPose VANILLA = new HandPose(0, 0, 0, 0, 0, 0, 1);

    public HandPose {
        x = clamp(x, -MAX_OFFSET, MAX_OFFSET);
        y = clamp(y, -MAX_OFFSET, MAX_OFFSET);
        z = clamp(z, -MAX_OFFSET, MAX_OFFSET);
        rotX = wrap(rotX);
        rotY = wrap(rotY);
        rotZ = wrap(rotZ);
        scale = clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public HandPose moved(float dx, float dy, float dz) {
        return new HandPose(x + dx, y + dy, z + dz, rotX, rotY, rotZ, scale);
    }

    public HandPose rotated(float dx, float dy, float dz) {
        return new HandPose(x, y, z, rotX + dx, rotY + dy, rotZ + dz, scale);
    }

    public HandPose scaled(float factor) {
        return new HandPose(x, y, z, rotX, rotY, rotZ, scale * factor);
    }

    public boolean isVanilla() {
        return near(this, VANILLA);
    }

    public boolean moves() {
        return x != 0 || y != 0 || z != 0;
    }

    public boolean turnsOrScales() {
        return rotX != 0 || rotY != 0 || rotZ != 0 || scale != 1;
    }

    public static boolean near(HandPose a, HandPose b) {
        float e = 0.005f;
        return Math.abs(a.x - b.x) < e && Math.abs(a.y - b.y) < e && Math.abs(a.z - b.z) < e
                && Math.abs(a.rotX - b.rotX) < 0.5f && Math.abs(a.rotY - b.rotY) < 0.5f && Math.abs(a.rotZ - b.rotZ) < 0.5f
                && Math.abs(a.scale - b.scale) < e;
    }

    static float clamp(float v, float min, float max) {
        if (Float.isNaN(v)) return min > 0 ? 1 : 0;
        return Math.max(min, Math.min(max, v));
    }

    static float wrap(float deg) {
        if (Float.isNaN(deg) || Float.isInfinite(deg)) return 0;
        float d = deg % 360f;
        if (d > MAX_ROTATION) d -= 360f;
        if (d < -MAX_ROTATION) d += 360f;
        return d;
    }
}
