package dev.henny.companio.anim;

import dev.henny.companio.def.LookDef;
import dev.henny.companio.def.LookMode;
import dev.henny.companio.move.Angles;

import java.util.Random;

public final class LookControl {
    private final LookDef def;
    private final Random random;
    private float yaw, pitch, prevYaw, prevPitch;
    private float glanceYaw, glancePitch;
    private double nextGlance;

    public LookControl(LookDef def, Random random) {
        this.def = def;
        this.random = random;
    }

    public float tick(LookMode mode, float bodyYaw, double fromX, double fromY, double fromZ, double tx, double ty, double tz,
                      boolean hasTarget, double now, float dt) {
        prevYaw = yaw;
        prevPitch = pitch;
        float wantYaw = 0f, wantPitch = 0f, turnBody = 0f;
        switch (mode) {
            case PLAYER, TARGET -> {
                if (hasTarget) {
                    double dx = tx - fromX, dy = ty - fromY, dz = tz - fromZ;
                    float rel = Angles.wrap(Angles.yawTo(dx, dz) - bodyYaw);
                    wantYaw = clamp(rel, def.maxYaw());
                    if (Math.abs(rel) > def.maxYaw() + 10f) turnBody = rel;
                    wantPitch = clamp((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))), def.maxPitch());
                }
            }
            case SKY -> wantPitch = -def.maxPitch();
            case AROUND -> {
                if (now > nextGlance) {
                    nextGlance = now + 1.5 + random.nextDouble() * 2.5;
                    glanceYaw = (random.nextFloat() * 2f - 1f) * def.maxYaw();
                    glancePitch = (random.nextFloat() * 2f - 1f) * def.maxPitch() * 0.5f;
                }
                wantYaw = glanceYaw;
                wantPitch = glancePitch;
            }
            default -> { }
        }
        float step = def.speed() * dt;
        yaw = Angles.approach(yaw, wantYaw, step);
        pitch = Angles.approach(pitch, wantPitch, step);
        return turnBody;
    }

    private static float clamp(float v, float max) {
        return Math.max(-max, Math.min(max, v));
    }

    public float yaw(float partial) {
        return prevYaw + (yaw - prevYaw) * partial;
    }

    public float pitch(float partial) {
        return prevPitch + (pitch - prevPitch) * partial;
    }
}
