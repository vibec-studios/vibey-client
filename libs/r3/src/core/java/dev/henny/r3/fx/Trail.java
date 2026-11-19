package dev.henny.r3.fx;

import dev.henny.r3.math.Argb;
import dev.henny.r3.object.Transform;
import dev.henny.r3.render.Quads;
import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.WorldCanvas;
import org.joml.Vector3dc;

public final class Trail implements Effect {
    private final double[] xs;
    private final double[] ys;
    private final double[] zs;
    private final float[] ages;
    private final int capacity;
    private final float pointLifetime;
    private float spacing = 0.15f;
    private float startWidth = 0.25f;
    private float endWidth = 0f;
    private ColorRamp color = ColorRamp.fadeOut(0xFFFFFFFF);
    private int head = -1;
    private int count;

    public Trail(int capacity, float pointLifetimeSeconds) {
        this.capacity = Math.max(2, capacity);
        this.pointLifetime = Math.max(0.01f, pointLifetimeSeconds);
        xs = new double[this.capacity];
        ys = new double[this.capacity];
        zs = new double[this.capacity];
        ages = new float[this.capacity];
    }

    public Trail spacing(float meters) {
        spacing = Math.max(0.001f, meters);
        return this;
    }

    public Trail width(float start, float end) {
        startWidth = start;
        endWidth = end;
        return this;
    }

    public Trail color(ColorRamp ramp) {
        color = ramp;
        return this;
    }

    public int points() {
        return count;
    }

    @Override
    public void simulate(float dt, EffectContext ctx) {
        for (int k = 0; k < count; k++) ages[index(k)] += dt;
        while (count > 0 && ages[index(count - 1)] >= pointLifetime) count--;
        Vector3dc o = ctx.origin();
        if (count == 0 || distanceTo(o, head) >= spacing) push(o);
    }

    private double distanceTo(Vector3dc o, int point) {
        double dx = o.x() - xs[point], dy = o.y() - ys[point], dz = o.z() - zs[point];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void push(Vector3dc o) {
        head = (head + 1) % capacity;
        xs[head] = o.x();
        ys[head] = o.y();
        zs[head] = o.z();
        ages[head] = 0f;
        count = Math.min(capacity, count + 1);
    }

    private int index(int k) {
        return Math.floorMod(head - k, capacity);
    }

    @Override
    public void draw(WorldCanvas canvas, EffectContext ctx) {
        if (count < 2) return;
        Vector3dc cam = ctx.cameraPosition();
        canvas.beginQuads(RenderMode.ADDITIVE, null);
        for (int k = 0; k < count - 1; k++) {
            int a = index(k), b = index(k + 1);
            float ta = Math.min(1f, ages[a] / pointLifetime), tb = Math.min(1f, ages[b] / pointLifetime);
            Quads.ribbon(canvas,
                    (float) (xs[a] - cam.x()), (float) (ys[a] - cam.y()), (float) (zs[a] - cam.z()),
                    (float) (xs[b] - cam.x()), (float) (ys[b] - cam.y()), (float) (zs[b] - cam.z()),
                    width(ta), width(tb), Argb.multiplyAlpha(color.at(ta), ctx.alpha()),
                    Argb.multiplyAlpha(color.at(tb), ctx.alpha()), ctx.light());
        }
        canvas.endQuads();
    }

    private float width(float t) {
        return (startWidth + (endWidth - startWidth) * t) * 0.5f;
    }

    @Override
    public double boundingRadius(Transform transform) {
        return capacity * spacing + startWidth;
    }
}
