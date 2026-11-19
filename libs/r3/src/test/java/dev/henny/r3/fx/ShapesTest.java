package dev.henny.r3.fx;

import dev.henny.r3.support.RecordingCanvas;
import dev.henny.r3.view.CameraState;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShapesTest {
    private final Vector3d origin = new Vector3d(0, 64, 0);
    private final EffectContext ctx = new EffectContext()
            .origin(origin, new Quaternionf())
            .camera(new CameraState().set(0, 64, -5, 0, 0))
            .detail(1f, 1f, 0xF000F0, EffectBudget.UNLIMITED);
    private final RecordingCanvas canvas = new RecordingCanvas();

    @Test
    void beamDrawsCoreAndGlow() {
        Beam beam = new Beam().to(0, 70, 0).width(0.2f).pulse(1, 0.5f);
        beam.simulate(0.25f, ctx);
        assertEquals(0.3f, beam.currentWidth(), 1e-4);
        beam.draw(canvas, ctx);
        assertEquals(8, canvas.vertices);
    }

    @Test
    void ringGrowsAndFinishes() {
        Ring ring = new Ring(0.5f, 3f, 1f).segments(16);
        ring.simulate(0.5f, ctx);
        assertEquals(1.75f, ring.radius(), 1e-4);
        ring.draw(canvas, ctx);
        assertEquals(64, canvas.vertices);
        ring.simulate(0.6f, ctx);
        assertTrue(ring.finished());
        Ring pulse = new Ring(0, 1, 0.5f).repeat(1f);
        pulse.simulate(10.25f, ctx);
        assertFalse(pulse.finished());
        assertEquals(0.5f, pulse.progress(), 1e-4);
    }

    @Test
    void trailFollowsTheOriginWithSpacing() {
        Trail trail = new Trail(16, 1f).spacing(0.5f);
        for (int i = 0; i <= 10; i++) {
            origin.set(i * 0.25, 64, 0);
            ctx.origin(origin, new Quaternionf());
            trail.simulate(0.01f, ctx);
        }
        assertEquals(6, trail.points());
        trail.draw(canvas, ctx);
        assertEquals((trail.points() - 1) * 4, canvas.vertices);
    }

    @Test
    void trailPointsFadeOut() {
        Trail trail = new Trail(8, 0.2f).spacing(0.1f);
        for (int i = 0; i < 5; i++) {
            origin.set(i, 64, 0);
            ctx.origin(origin, new Quaternionf());
            trail.simulate(0.05f, ctx);
        }
        for (int i = 0; i < 10; i++) trail.simulate(0.05f, ctx);
        assertEquals(1, trail.points());
    }

    @Test
    void glowIsOneBillboard() {
        Glow glow = new Glow(1f, 0x80FFFFFF).pulse(2f, 0.25f);
        glow.simulate(0.125f, ctx);
        assertEquals(1.25f, glow.currentSize(), 1e-4);
        glow.draw(canvas, ctx);
        assertEquals(4, canvas.vertices);
    }

    @Test
    void rampsAndCurvesInterpolate() {
        ColorRamp ramp = ColorRamp.of(new float[] {0f, 0.5f, 1f}, new int[] {0xFF000000, 0xFFFFFFFF, 0x00FFFFFF});
        assertEquals(0xFF808080, ramp.at(0.25f));
        assertEquals(0x80FFFFFF, ramp.at(0.75f));
        FloatCurve curve = FloatCurve.popAndShrink(2f);
        assertEquals(0f, curve.at(0f), 1e-6);
        assertEquals(2f, curve.at(0.5f), 1e-6);
        assertEquals(1f, curve.at(0.9f), 1e-4);
    }

    @Test
    void rngIsUniformEnough() {
        Rng rng = new Rng(123);
        double sum = 0;
        for (int i = 0; i < 10_000; i++) {
            float f = rng.nextFloat();
            assertTrue(f >= 0f && f < 1f);
            sum += f;
        }
        assertEquals(0.5, sum / 10_000, 0.02);
    }
}
