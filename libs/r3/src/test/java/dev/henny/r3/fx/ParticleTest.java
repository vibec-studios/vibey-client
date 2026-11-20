package dev.henny.r3.fx;

import dev.henny.r3.support.RecordingCanvas;
import dev.henny.r3.view.CameraState;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleTest {
    private final EffectContext ctx = new EffectContext()
            .origin(new Vector3d(0, 64, 0), new Quaternionf())
            .camera(new CameraState().set(0, 64, -5, 0, 0))
            .detail(1f, 1f, 0xF000F0, EffectBudget.UNLIMITED);

    private ParticleSystem fountain(long seed) {
        return new ParticleSystem(256, ParticleStyle.glowDots(0xFFFFAA00).withPhysics(9.8f, 0.4f), seed)
                .emitter(new Emitter(EmitterShape.SPHERE).radius(0.3f).rate(120).speed(1, 2).lifetime(0.5f, 1f).size(0.1f, 0.2f));
    }

    @Test
    void sameSeedSameParticles() {
        ParticleSystem a = fountain(42);
        ParticleSystem b = fountain(42);
        for (int i = 0; i < 30; i++) {
            a.simulate(1f / 60f, ctx);
            b.simulate(1f / 60f, ctx);
        }
        assertEquals(a.liveParticles(), b.liveParticles());
        for (int i = 0; i < a.liveParticles(); i++) assertEquals(a.pool().x(i), b.pool().x(i), 0.0);
        ParticleSystem c = fountain(7);
        for (int i = 0; i < 30; i++) c.simulate(1f / 60f, ctx);
        assertTrue(Math.abs(c.pool().x(0) - a.pool().x(0)) > 1e-9);
    }

    @Test
    void particlesDieAfterTheirLifetime() {
        ParticleSystem p = new ParticleSystem(64, ParticleStyle.glowDots(-1), 1)
                .emitter(new Emitter(EmitterShape.POINT).burst(20).lifetime(0.5f, 0.5f)).oneShot(true);
        p.simulate(0.01f, ctx);
        assertEquals(20, p.liveParticles());
        for (int i = 0; i < 60; i++) p.simulate(0.01f, ctx);
        assertEquals(0, p.liveParticles());
        assertTrue(p.finished());
    }

    @Test
    void gravityPullsDown() {
        ParticleSystem p = new ParticleSystem(8, ParticleStyle.glowDots(-1).withPhysics(10f, 0f), 1)
                .emitter(new Emitter(EmitterShape.POINT).burst(1).lifetime(5, 5));
        p.simulate(0.001f, ctx);
        for (int i = 0; i < 100; i++) p.simulate(0.01f, ctx);
        assertTrue(p.pool().y(0) < 64 - 4);
    }

    @Test
    void poolNeverGrowsPastCapacity() {
        ParticleSystem p = new ParticleSystem(32, ParticleStyle.glowDots(-1), 1)
                .emitter(new Emitter(EmitterShape.POINT).rate(10_000).lifetime(10, 10));
        for (int i = 0; i < 10; i++) p.simulate(0.05f, ctx);
        assertEquals(32, p.liveParticles());
    }

    @Test
    void ringEmitterSpawnsOnTheRing() {
        ParticleSystem p = new ParticleSystem(64, ParticleStyle.glowDots(-1), 3)
                .emitter(new Emitter(EmitterShape.RING).radius(2).burst(50).lifetime(1, 1));
        p.simulate(1e-6f, ctx);
        for (int i = 0; i < p.liveParticles(); i++) {
            double dx = p.pool().x(i), dy = p.pool().y(i) - 64, dz = p.pool().z(i);
            assertEquals(2, Math.sqrt(dx * dx + dy * dy), 1e-3);
            assertEquals(0, dz, 1e-3);
        }
    }

    @Test
    void budgetCapsNewParticlesPerFrame() {
        EffectBudget budget = new EffectBudget(10, 16, 48, 96);
        EffectContext limited = new EffectContext().origin(new Vector3d(), new Quaternionf())
                .camera(new CameraState()).detail(1f, 1f, 0, budget);
        ParticleSystem p = new ParticleSystem(256, ParticleStyle.glowDots(-1), 1)
                .emitter(new Emitter(EmitterShape.POINT).burst(100).lifetime(5, 5));
        budget.newFrame();
        p.simulate(0.01f, limited);
        assertEquals(10, p.liveParticles());
    }

    @Test
    void levelOfDetailDropsWithDistance() {
        EffectBudget b = EffectBudget.standard();
        assertEquals(1f, b.lodAt(4), 1e-6);
        assertTrue(b.lodAt(30) < 1f && b.lodAt(30) > 0.1f);
        assertEquals(0f, b.lodAt(200), 1e-6);
        assertTrue(b.culled(200));
        assertFalse(b.culled(10));
    }

    @Test
    void drawingEmitsOneQuadPerParticle() {
        ParticleSystem p = fountain(9);
        p.simulate(0.1f, ctx);
        p.simulate(0.05f, ctx);
        RecordingCanvas canvas = new RecordingCanvas();
        p.draw(canvas, ctx);
        assertTrue(canvas.vertices > 0 && canvas.vertices % 4 == 0 && canvas.vertices <= p.liveParticles() * 4);
        assertEquals(1, canvas.batches);
    }

    @Test
    void noAllocationAfterWarmup() {
        ParticleSystem p = fountain(5);
        RecordingCanvas canvas = new RecordingCanvas();
        canvas.recordTexts = false;
        for (int i = 0; i < 2000; i++) {
            p.simulate(1f / 60f, ctx);
            p.draw(canvas, ctx);
        }
        com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long id = Thread.currentThread().getId();
        long before = mx.getThreadAllocatedBytes(id);
        for (int i = 0; i < 2000; i++) {
            p.simulate(1f / 60f, ctx);
            p.draw(canvas, ctx);
        }
        long allocated = mx.getThreadAllocatedBytes(id) - before;
        assertTrue(p.liveParticles() > 50);
        assertTrue(allocated < 1024, "allocated " + allocated + " bytes");
    }
}
