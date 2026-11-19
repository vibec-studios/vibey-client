package dev.henny.r3.fx;

import dev.henny.r3.math.Argb;
import dev.henny.r3.object.Transform;
import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.WorldCanvas;
import org.joml.Vector3dc;

public final class Ring implements Effect {
    private static final double TAU = Math.PI * 2;

    private final float startRadius;
    private final float endRadius;
    private final float duration;
    private float thickness = 0.08f;
    private ColorRamp color = ColorRamp.fadeOut(0xFFFFFFFF);
    private float repeatEvery;
    private int segments = 32;
    private boolean horizontal = true;
    private float time;

    public Ring(float startRadius, float endRadius, float durationSeconds) {
        this.startRadius = startRadius;
        this.endRadius = endRadius;
        this.duration = Math.max(0.01f, durationSeconds);
    }

    public Ring thickness(float t) {
        thickness = t;
        return this;
    }

    public Ring color(ColorRamp ramp) {
        color = ramp;
        return this;
    }

    public Ring repeat(float everySeconds) {
        repeatEvery = Math.max(0f, everySeconds);
        return this;
    }

    public Ring segments(int n) {
        segments = Math.max(6, n);
        return this;
    }

    public Ring inObjectPlane() {
        horizontal = false;
        return this;
    }

    public float progress() {
        float t = repeatEvery > 0f ? time % Math.max(repeatEvery, duration) : time;
        return Math.min(1f, t / duration);
    }

    public float radius() {
        return startRadius + (endRadius - startRadius) * progress();
    }

    @Override
    public void simulate(float dt, EffectContext context) {
        time += dt;
    }

    @Override
    public boolean finished() {
        return repeatEvery <= 0f && time >= duration;
    }

    @Override
    public void draw(WorldCanvas canvas, EffectContext ctx) {
        float p = progress();
        if (p >= 1f && repeatEvery <= 0f) return;
        int argb = Argb.multiplyAlpha(color.at(p), ctx.alpha());
        float r = radius();
        Vector3dc o = ctx.origin();
        Vector3dc cam = ctx.cameraPosition();
        float cx = (float) (o.x() - cam.x()), cy = (float) (o.y() - cam.y()), cz = (float) (o.z() - cam.z());
        canvas.beginQuads(RenderMode.ADDITIVE, null);
        for (int i = 0; i < segments; i++) {
            double a0 = TAU * i / segments;
            double a1 = TAU * (i + 1) / segments;
            vertex(canvas, ctx, cx, cy, cz, a0, r + thickness, argb);
            vertex(canvas, ctx, cx, cy, cz, a0, r - thickness, argb);
            vertex(canvas, ctx, cx, cy, cz, a1, r - thickness, argb);
            vertex(canvas, ctx, cx, cy, cz, a1, r + thickness, argb);
        }
        canvas.endQuads();
    }

    private void vertex(WorldCanvas c, EffectContext ctx, float cx, float cy, float cz, double angle, float radius, int argb) {
        float cos = (float) Math.cos(angle) * Math.max(0f, radius);
        float sin = (float) Math.sin(angle) * Math.max(0f, radius);
        if (horizontal) {
            c.vertex(cx + cos, cy, cz + sin, 0f, 0f, argb, ctx.light());
            return;
        }
        Vector3dc r = ctx.right();
        Vector3dc u = ctx.up();
        c.vertex((float) (cx + r.x() * cos + u.x() * sin), (float) (cy + r.y() * cos + u.y() * sin),
                (float) (cz + r.z() * cos + u.z() * sin), 0f, 0f, argb, ctx.light());
    }

    @Override
    public double boundingRadius(Transform transform) {
        return Math.max(startRadius, endRadius) + thickness;
    }
}
