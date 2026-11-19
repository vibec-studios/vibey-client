package dev.henny.r3.fx;

import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.TextureHandle;

public record ParticleStyle(ColorRamp color, FloatCurve size, float gravity, float drag, RenderMode mode,
                            TextureHandle texture, float u0, float v0, float u1, float v1) {
    public static ParticleStyle glowDots(int argb) {
        return new ParticleStyle(ColorRamp.fadeOut(argb), FloatCurve.popAndShrink(1f), 0f, 0.5f,
                RenderMode.ADDITIVE, null, 0f, 0f, 1f, 1f);
    }

    public ParticleStyle withPhysics(float gravityPerSecond, float dragPerSecond) {
        return new ParticleStyle(color, size, gravityPerSecond, dragPerSecond, mode, texture, u0, v0, u1, v1);
    }

    public ParticleStyle withTexture(TextureHandle tex, float tu0, float tv0, float tu1, float tv1) {
        return new ParticleStyle(color, size, gravity, drag, mode, tex, tu0, tv0, tu1, tv1);
    }

    public ParticleStyle withMode(RenderMode renderMode) {
        return new ParticleStyle(color, size, gravity, drag, renderMode, texture, u0, v0, u1, v1);
    }

    public ParticleStyle withColor(ColorRamp ramp) {
        return new ParticleStyle(ramp, size, gravity, drag, mode, texture, u0, v0, u1, v1);
    }

    public ParticleStyle withSize(FloatCurve curve) {
        return new ParticleStyle(color, curve, gravity, drag, mode, texture, u0, v0, u1, v1);
    }
}
