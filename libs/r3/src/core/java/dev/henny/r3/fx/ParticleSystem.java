package dev.henny.r3.fx;

import dev.henny.r3.math.Argb;
import dev.henny.r3.object.Transform;
import dev.henny.r3.render.Quads;
import dev.henny.r3.render.WorldCanvas;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.List;

public final class ParticleSystem implements Effect {
    private final ParticlePool pool;
    private final ParticleStyle style;
    private final List<Emitter> emitters = new ArrayList<>();
    private final Rng rng;
    private boolean oneShot;
    private boolean started;

    public ParticleSystem(int capacity, ParticleStyle style, long seed) {
        this.pool = new ParticlePool(capacity);
        this.style = style;
        this.rng = new Rng(seed);
    }

    public ParticleSystem emitter(Emitter emitter) {
        emitters.add(emitter);
        return this;
    }

    public ParticleSystem oneShot(boolean finishWhenEmpty) {
        oneShot = finishWhenEmpty;
        return this;
    }

    public List<Emitter> emitters() {
        return emitters;
    }

    public ParticlePool pool() {
        return pool;
    }

    public ParticleStyle style() {
        return style;
    }

    @Override
    public void simulate(float dt, EffectContext ctx) {
        pool.simulate(dt, style.gravity(), style.drag());
        for (int e = 0; e < emitters.size(); e++) {
            Emitter emitter = emitters.get(e);
            int n = ctx.budget().take(emitter.due(dt, ctx.lod()));
            for (int i = 0; i < n; i++) emitter.emit(pool, ctx, rng);
            if (n > 0) started = true;
        }
    }

    @Override
    public void draw(WorldCanvas canvas, EffectContext ctx) {
        int count = pool.count();
        if (count == 0) return;
        Vector3dc cam = ctx.cameraPosition();
        canvas.beginQuads(style.mode(), style.texture());
        for (int i = 0; i < count; i++) {
            float t = pool.lifeFraction(i);
            float half = pool.size[i] * 0.5f * style.size().at(t);
            if (half <= 0f) continue;
            int argb = Argb.multiplyAlpha(style.color().at(t), ctx.alpha());
            Quads.billboard(canvas, (float) (pool.x[i] - cam.x()), (float) (pool.y[i] - cam.y()), (float) (pool.z[i] - cam.z()),
                    ctx.cameraRight(), ctx.cameraUp(), half, pool.spin[i] * pool.age[i],
                    style.u0(), style.v0(), style.u1(), style.v1(), argb, ctx.light());
        }
        canvas.endQuads();
    }

    @Override
    public boolean finished() {
        return oneShot && started && pool.count() == 0;
    }

    @Override
    public int liveParticles() {
        return pool.count();
    }

    @Override
    public double boundingRadius(Transform transform) {
        return 8.0;
    }
}
