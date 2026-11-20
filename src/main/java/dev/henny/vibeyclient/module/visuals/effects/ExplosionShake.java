package dev.henny.vibeyclient.module.visuals.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.henny.vibeyclient.module.visuals.effects.core.Shake;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

public final class ExplosionShake {
    private static final float BIG = 4f, SMALL = 1.5f;
    private static final Shake SHAKE = new Shake();
    private static long lastNanos;

    private ExplosionShake() {}

    public static void seen(ParticleOptions options, double x, double y, double z) {
        ParticleType<?> t = options.getType();
        if (t != ParticleTypes.EXPLOSION_EMITTER && t != ParticleTypes.EXPLOSION) return;
        if (Effects.blastStrength() <= 0) return;
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        double dx = x - cam.x, dy = y - cam.y, dz = z - cam.z;
        SHAKE.kick(Shake.power(Math.sqrt(dx * dx + dy * dy + dz * dz), t == ParticleTypes.EXPLOSION_EMITTER ? BIG : SMALL));
    }

    public static void apply(PoseStack pose) {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0 : (now - lastNanos) / 1e9f;
        lastNanos = now;
        SHAKE.step(dt);
        float strength = Effects.blastStrength();
        if (SHAKE.energy() <= 0 || strength <= 0) return;
        pose.mulPose(Axis.ZP.rotationDegrees(SHAKE.roll(strength)));
        pose.mulPose(Axis.XP.rotationDegrees(SHAKE.pitch(strength)));
    }

    public static float energy() {
        return SHAKE.energy();
    }
}
