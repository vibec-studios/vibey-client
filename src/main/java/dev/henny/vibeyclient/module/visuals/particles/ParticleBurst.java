package dev.henny.vibeyclient.module.visuals.particles;

import dev.henny.vibeyclient.module.visuals.effects.PreviewSpot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public final class ParticleBurst {
    public static final int COUNT = 24;
    public static final double DISTANCE = 3.2;
    private static final double SPREAD = 0.45, SPEED = 0.04;

    private ParticleBurst() {}

    public static boolean testable(String id) {
        return PreviewOptions.of(ParticleIds.type(id)) != null;
    }

    public static boolean spawn(String id) {
        Minecraft mc = Minecraft.getInstance();
        ParticleOptions o = PreviewOptions.of(ParticleIds.type(id));
        if (o == null || mc.level == null || mc.player == null) return false;
        Vec3 at = PreviewSpot.ahead(DISTANCE);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < COUNT; i++) {
            mc.level.addParticle(o, at.x + r.nextDouble(-SPREAD, SPREAD), at.y + r.nextDouble(-SPREAD, SPREAD),
                    at.z + r.nextDouble(-SPREAD, SPREAD), r.nextDouble(-SPEED, SPEED), r.nextDouble(0, SPEED * 2),
                    r.nextDouble(-SPEED, SPEED));
        }
        return true;
    }
}
