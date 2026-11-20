package dev.henny.vibeyclient.module.visuals.items.glint;

import dev.henny.vibeyclient.module.visuals.VisualsModule;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class GlintRender {
    private GlintRender() {}

    private static GlintSettings settings() {
        VisualsModule m = VisualsModule.get();
        return m == null || !m.isEnabled() ? null : m.glint;
    }

    public static boolean isGlint(TextureTransform t) {
        return t == TextureTransform.GLINT_TEXTURING || t == TextureTransform.ENTITY_GLINT_TEXTURING
                || t == TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING;
    }

    public static Vector4fc color(TextureTransform transform, Vector4fc vanilla) {
        if (!isGlint(transform)) return vanilla;
        GlintSettings s = settings();
        if (s == null || !s.changesColor()) return vanilla;
        float[] m = s.modulator();
        return new Vector4f(vanilla.x() * m[0], vanilla.y() * m[1], vanilla.z() * m[2], vanilla.w() * m[3]);
    }

    public static double speed(double vanilla) {
        GlintSettings s = settings();
        if (s == null) return vanilla;
        int pct = s.speed.get();
        return pct == 100 ? vanilla : vanilla * pct / 100.0;
    }
}
