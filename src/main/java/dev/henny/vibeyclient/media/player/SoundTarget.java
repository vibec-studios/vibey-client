package dev.henny.vibeyclient.media.player;

import net.minecraft.world.phys.Vec3;

final class SoundTarget {
    volatile float gain = 1f;
    volatile double x;
    volatile double y;
    volatile double z;
    volatile boolean relative = true;

    void set(float gain, Vec3 pos) {
        this.gain = Math.max(0f, Math.min(1f, gain));
        if (pos == null) {
            relative = true;
            return;
        }
        x = pos.x;
        y = pos.y;
        z = pos.z;
        relative = false;
    }
}
