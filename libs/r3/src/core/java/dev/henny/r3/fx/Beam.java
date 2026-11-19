package dev.henny.r3.fx;

import dev.henny.r3.math.Argb;
import dev.henny.r3.object.Transform;
import dev.henny.r3.render.Quads;
import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.WorldCanvas;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class Beam implements Effect {
    private final Vector3d end = new Vector3d();
    private float width = 0.1f;
    private int coreColor = 0xFFFFFFFF;
    private int glowColor = 0x804C8BFF;
    private float pulseSpeed;
    private float pulseAmount;
    private float time;

    public Beam to(double x, double y, double z) {
        end.set(x, y, z);
        return this;
    }

    public Beam to(Vector3dc target) {
        end.set(target);
        return this;
    }

    public Beam width(float w) {
        width = w;
        return this;
    }

    public Beam colors(int core, int glow) {
        coreColor = core;
        glowColor = glow;
        return this;
    }

    public Beam pulse(float cyclesPerSecond, float amount) {
        pulseSpeed = cyclesPerSecond;
        pulseAmount = amount;
        return this;
    }

    public Vector3dc end() {
        return end;
    }

    public float currentWidth() {
        return width * (1f + (float) Math.sin(time * pulseSpeed * Math.PI * 2) * pulseAmount);
    }

    @Override
    public void simulate(float dt, EffectContext context) {
        time += dt;
    }

    @Override
    public void draw(WorldCanvas canvas, EffectContext ctx) {
        Vector3dc o = ctx.origin();
        Vector3dc cam = ctx.cameraPosition();
        float ax = (float) (o.x() - cam.x()), ay = (float) (o.y() - cam.y()), az = (float) (o.z() - cam.z());
        float bx = (float) (end.x - cam.x()), by = (float) (end.y - cam.y()), bz = (float) (end.z - cam.z());
        float w = currentWidth();
        int glow = Argb.multiplyAlpha(glowColor, ctx.alpha());
        int core = Argb.multiplyAlpha(coreColor, ctx.alpha());
        canvas.beginQuads(RenderMode.ADDITIVE, null);
        Quads.ribbon(canvas, ax, ay, az, bx, by, bz, w * 1.5f, w * 1.5f, glow, glow, ctx.light());
        Quads.ribbon(canvas, ax, ay, az, bx, by, bz, w * 0.35f, w * 0.35f, core, core, ctx.light());
        canvas.endQuads();
    }

    @Override
    public double boundingRadius(Transform transform) {
        return end.distance(transform.position()) + width;
    }
}
