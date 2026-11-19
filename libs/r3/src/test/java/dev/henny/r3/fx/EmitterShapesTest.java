package dev.henny.r3.fx;

import dev.henny.r3.view.CameraState;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmitterShapesTest {
    private EffectContext ctx(Quaternionf orientation) {
        return new EffectContext().origin(new Vector3d(10, 64, 10), orientation)
                .camera(new CameraState()).detail(1f, 1f, 0, EffectBudget.UNLIMITED);
    }

    private ParticleSystem burst(Emitter e, EffectContext ctx) {
        ParticleSystem p = new ParticleSystem(200, ParticleStyle.glowDots(-1).withPhysics(0, 0), 11).emitter(e);
        p.simulate(1e-6f, ctx);
        return p;
    }

    @Test
    void boxSpawnsInsideTheBox() {
        ParticleSystem p = burst(new Emitter(EmitterShape.BOX).box(1, 0.5, 2).burst(150), ctx(new Quaternionf()));
        assertEquals(150, p.liveParticles());
        for (int i = 0; i < p.liveParticles(); i++) {
            assertTrue(Math.abs(p.pool().x(i) - 10) <= 1);
            assertTrue(Math.abs(p.pool().y(i) - 64) <= 0.5);
            assertTrue(Math.abs(p.pool().z(i) - 10) <= 2);
        }
    }

    @Test
    void sphereSpawnsWithinTheRadius() {
        ParticleSystem p = burst(new Emitter(EmitterShape.SPHERE).radius(1.5f).burst(100), ctx(new Quaternionf()));
        for (int i = 0; i < p.liveParticles(); i++) {
            double dx = p.pool().x(i) - 10, dy = p.pool().y(i) - 64, dz = p.pool().z(i) - 10;
            assertTrue(Math.sqrt(dx * dx + dy * dy + dz * dz) <= 1.5 + 1e-6);
        }
    }

    @Test
    void beamSpawnsAlongTheNormal() {
        ParticleSystem p = burst(new Emitter(EmitterShape.BEAM).length(5).radius(0.1f).burst(50), ctx(new Quaternionf()));
        for (int i = 0; i < p.liveParticles(); i++) {
            double dz = p.pool().z(i) - 10;
            assertTrue(dz >= -0.02 && dz <= 5.02);
        }
    }

    @Test
    void localVelocityTurnsWithTheObject() {
        Quaternionf turned = new Quaternionf().rotationY((float) Math.toRadians(90));
        Emitter e = new Emitter(EmitterShape.POINT).velocity(0, 0, 1).localVelocity(true).burst(1).lifetime(5, 5);
        ParticleSystem p = burst(e, ctx(turned));
        p.simulate(1f, ctx(turned));
        assertEquals(11, p.pool().x(0), 1e-3);
        assertEquals(10, p.pool().z(0), 1e-3);
    }
}
