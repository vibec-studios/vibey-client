package dev.henny.r3.fx;

import dev.henny.r3.math.Argb;
import dev.henny.r3.object.Transform;
import dev.henny.r3.render.Quads;
import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.TextureHandle;
import dev.henny.r3.render.WorldCanvas;
import org.joml.Vector3dc;

public final class Glow implements Effect {
    private final float size;
    private int color;
    private TextureHandle texture;
    private float pulseSpeed;
    private float pulseAmount;
    private float time;

    public Glow(float size, int color) {
        this.size = size;
        this.color = color;
    }

    public Glow texture(TextureHandle soft) {
        texture = soft;
        return this;
    }

    public Glow color(int argb) {
        color = argb;
        return this;
    }

    public Glow pulse(float cyclesPerSecond, float amount) {
        pulseSpeed = cyclesPerSecond;
        pulseAmount = amount;
        return this;
    }

    public float currentSize() {
        return size * (1f + (float) Math.sin(time * pulseSpeed * Math.PI * 2) * pulseAmount);
    }

    @Override
    public void simulate(float dt, EffectContext context) {
        time += dt;
    }

    @Override
    public void draw(WorldCanvas canvas, EffectContext ctx) {
        Vector3dc o = ctx.origin();
        Vector3dc cam = ctx.cameraPosition();
        canvas.beginQuads(RenderMode.ADDITIVE, texture);
        Quads.billboard(canvas, (float) (o.x() - cam.x()), (float) (o.y() - cam.y()), (float) (o.z() - cam.z()),
                ctx.cameraRight(), ctx.cameraUp(), currentSize() * 0.5f, 0f, 0f, 0f, 1f, 1f,
                Argb.multiplyAlpha(color, ctx.alpha()), ctx.light());
        canvas.endQuads();
    }

    @Override
    public double boundingRadius(Transform transform) {
        return size * (1 + pulseAmount);
    }
}
