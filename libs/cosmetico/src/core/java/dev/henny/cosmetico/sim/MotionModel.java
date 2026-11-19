package dev.henny.cosmetico.sim;

import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.state.FrameSnapshot;
import dev.henny.cosmetico.state.Motion;

public final class MotionModel {
    private MotionModel() {}

    public static final int IDLE_LONG_TICKS = 200, FALL_GLIDE_TICKS = 14;

    public static void step(Avatar a, MotionInput in, double now) {
        Motion m = a.motion;
        FrameSnapshot f = a.frame;
        float speed = Math.min(1f, in.walkSpeed() * 1.2f);
        boolean elytra = in.fallFlying(), flying = in.flying();
        double dx = in.x() - f.x, dy = in.y() - f.y, dz = in.z() - f.z;
        if (!m.moveInit) {
            dx = dy = dz = 0;
            m.moveInit = true;
        }
        boolean falling = dy < -0.2 && !in.onGround() && !flying && !elytra && !in.inWater();
        m.fallTicks = falling ? m.fallTicks + 1 : 0;
        boolean still = Math.abs(dx) + Math.abs(dz) < 0.004 && Math.abs(dy) < 0.02 && speed < 0.05 && !flying && !elytra
                && in.onGround() && a.emote == null;
        m.stillTicks = still ? m.stillTicks + 1 : 0;
        targets(a, m, in, speed, dx, dz);
        approach(m);
        StateStarts.update(m, now);
        f.bodyYaw = in.bodyYaw();
        f.x = in.x();
        f.y = in.y();
        f.z = in.z();
    }

    private static void targets(Avatar a, Motion m, MotionInput in, float speed, double dx, double dz) {
        boolean elytra = in.fallFlying(), flying = in.flying();
        m.walkTarget = elytra ? 0 : speed;
        m.flyTarget = elytra || flying ? 1 : 0;
        m.sneakTarget = in.crouching() ? 1 : 0;
        boolean moving = speed > 0.3f || dx * dx + dz * dz > 0.08 * 0.08;
        m.sprintTarget = in.sprinting() && !in.swimming() && !elytra && !flying && moving ? 1 : 0;
        m.glideTarget = elytra || m.fallTicks > FALL_GLIDE_TICKS ? 1 : 0;
        m.idleLongTarget = m.stillTicks > IDLE_LONG_TICKS ? 1 : 0;
        m.happyTarget = a.emote != null && !a.emote.stopping() ? 1 : 0;
    }

    private static void approach(Motion m) {
        m.walk = Approach.to(m.walk, m.walkTarget, 0.25f);
        m.fly = Approach.to(m.fly, m.flyTarget, 0.2f);
        m.sneak = Approach.to(m.sneak, m.sneakTarget, 0.3f);
        m.sprint = Approach.to(m.sprint, m.sprintTarget, 0.2f);
        m.glide = Approach.to(m.glide, m.glideTarget, 0.15f);
        m.idleLong = Approach.to(m.idleLong, m.idleLongTarget, m.idleLongTarget > m.idleLong ? 0.07f : 0.35f);
        m.happy = Approach.to(m.happy, m.happyTarget, 0.25f);
    }
}
