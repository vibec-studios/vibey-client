package dev.henny.r3.fx;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

public final class Emitter {
    private static final float TAU = (float) (Math.PI * 2);

    private final EmitterShape shape;
    private final Vector3d baseVelocity = new Vector3d();
    private final Vector3d dir = new Vector3d();
    private final Vector3d pos = new Vector3d();
    private final Vector3d boxHalf = new Vector3d(0.5, 0.5, 0.5);
    private final Vector3f velocity = new Vector3f();
    private boolean localVelocity;
    private float radius = 0.5f;
    private float length = 2f;
    private float rate;
    private float speedMin;
    private float speedMax;
    private float spread;
    private float lifeMin = 1f;
    private float lifeMax = 1f;
    private float sizeMin = 0.1f;
    private float sizeMax = 0.1f;
    private float spinMax;
    private float accumulator;
    private int pendingBurst;

    public Emitter(EmitterShape shape) {
        this.shape = shape;
    }

    public EmitterShape shape() {
        return shape;
    }

    public Emitter radius(float r) {
        radius = r;
        return this;
    }

    public Emitter length(float l) {
        length = l;
        return this;
    }

    public Emitter rate(float perSecond) {
        rate = Math.max(0f, perSecond);
        return this;
    }

    public Emitter speed(float min, float max) {
        speedMin = min;
        speedMax = max;
        return this;
    }

    public Emitter spread(float randomSpeed) {
        spread = randomSpeed;
        return this;
    }

    public Emitter box(double halfX, double halfY, double halfZ) {
        boxHalf.set(halfX, halfY, halfZ);
        return this;
    }

    public Emitter localVelocity(boolean objectSpace) {
        localVelocity = objectSpace;
        return this;
    }

    public Emitter velocity(double x, double y, double z) {
        baseVelocity.set(x, y, z);
        return this;
    }

    public Emitter lifetime(float min, float max) {
        lifeMin = min;
        lifeMax = Math.max(min, max);
        return this;
    }

    public Emitter size(float min, float max) {
        sizeMin = min;
        sizeMax = Math.max(min, max);
        return this;
    }

    public Emitter spin(float maxRadiansPerSecond) {
        spinMax = maxRadiansPerSecond;
        return this;
    }

    public Emitter burst(int count) {
        pendingBurst += Math.max(0, count);
        return this;
    }

    public float rate() {
        return rate;
    }

    public int due(float dt, float lod) {
        accumulator += rate * lod * dt;
        int n = (int) accumulator;
        accumulator -= n;
        if (pendingBurst > 0) {
            n += Math.max(1, Math.round(pendingBurst * lod));
            pendingBurst = 0;
        }
        return n;
    }

    public void emit(ParticlePool pool, EffectContext ctx, Rng rng) {
        place(ctx, rng);
        float speed = rng.range(speedMin, speedMax);
        velocity.set((float) baseVelocity.x, (float) baseVelocity.y, (float) baseVelocity.z);
        if (localVelocity) ctx.orientation().transform(velocity);
        float vx = (float) (dir.x * speed + velocity.x + rng.signed() * spread);
        float vy = (float) (dir.y * speed + velocity.y + rng.signed() * spread);
        float vz = (float) (dir.z * speed + velocity.z + rng.signed() * spread);
        pool.spawn(pos.x, pos.y, pos.z, vx, vy, vz, rng.range(lifeMin, lifeMax), rng.range(sizeMin, sizeMax), rng.signed() * spinMax);
    }

    private void place(EffectContext ctx, Rng rng) {
        Vector3dc o = ctx.origin();
        switch (shape) {
            case RING -> {
                float a = rng.nextFloat() * TAU;
                dir.set(ctx.right()).mul(Math.cos(a)).fma(Math.sin(a), ctx.up());
                pos.set(o).fma(radius, dir);
            }
            case SPHERE -> {
                randomUnit(rng);
                pos.set(o).fma(radius * Math.cbrt(rng.nextFloat()), dir);
            }
            case BOX -> {
                randomUnit(rng);
                pos.set(o).add(rng.signed() * boxHalf.x, rng.signed() * boxHalf.y, rng.signed() * boxHalf.z);
            }
            case BEAM -> {
                pos.set(o).fma(length * rng.nextFloat(), ctx.normal());
                randomUnit(rng);
                pos.fma(radius * 0.1, dir);
            }
            default -> {
                randomUnit(rng);
                pos.set(o);
            }
        }
    }

    private void randomUnit(Rng rng) {
        float z = rng.signed();
        float a = rng.nextFloat() * TAU;
        float r = (float) Math.sqrt(Math.max(0f, 1f - z * z));
        dir.set(r * Math.cos(a), r * Math.sin(a), z);
    }
}
