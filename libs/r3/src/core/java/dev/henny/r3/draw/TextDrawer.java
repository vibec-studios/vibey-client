package dev.henny.r3.draw;

import dev.henny.r3.math.Argb;
import dev.henny.r3.object.Transform;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.TextMetrics;
import dev.henny.r3.render.WorldCanvas;
import dev.henny.r3.text.GlyphEffect;
import dev.henny.r3.text.GlyphLayout;
import dev.henny.r3.text.GlyphState;
import dev.henny.r3.text.Text3D;
import dev.henny.r3.text.TextStyle;
import org.joml.Quaternionf;
import org.joml.Vector3dc;

import java.util.List;

public final class TextDrawer {
    public static final int DEPTH_LAYERS = 6;
    public static final float BACKGROUND_DEPTH = -0.7f;
    private static final float[] GLOW_X = {1, -1, 0, 0, 0.71f, -0.71f, 0.71f, -0.71f};
    private static final float[] GLOW_Y = {0, 0, 1, -1, 0.71f, 0.71f, -0.71f, -0.71f};

    private final GlyphState glyph = new GlyphState();
    private final Quaternionf roll = new Quaternionf();
    private GlyphLayout layout;
    private boolean seeThrough;
    private int light;

    public void draw(WorldCanvas canvas, WorldObject object, Text3D text, int light) {
        TextMetrics metrics = canvas.textMetrics();
        layout = text.layout(metrics);
        if (layout.count() == 0) return;
        Transform t = object.transform();
        Vector3dc cam = canvas.cameraPosition();
        double distance = object.state().distance();
        float scale = text.worldScale(t, distance);
        double k = text.inFrontDistance() > 0 && distance > text.inFrontDistance() ? text.inFrontDistance() / distance : 1.0;
        TextStyle style = text.style();
        this.seeThrough = style.seeThrough();
        this.light = light;
        canvas.push();
        canvas.translate((float) ((t.position().x - cam.x()) * k), (float) ((t.position().y - cam.y()) * k),
                (float) ((t.position().z - cam.z()) * k));
        canvas.rotate(t.orientation());
        float s = (float) (scale * k);
        canvas.scale(s, -s, s);
        float alpha = object.state().alpha();
        if (style.background() != 0) background(canvas, style, alpha);
        List<GlyphEffect> effects = text.effects();
        for (int i = 0; i < layout.count(); i++) {
            glyph.reset(i, layout.count(), layout.line(i), layout.codepoint(i), layout.x(i), layout.y(i), style.color());
            for (int e = 0; e < effects.size(); e++) effects.get(e).apply(glyph, text.time());
            layers(canvas, i, style, alpha);
        }
        canvas.pop();
    }

    private void layers(WorldCanvas canvas, int i, TextStyle style, float alpha) {
        int main = Argb.multiplyAlpha(glyph.argb, alpha);
        if (style.depthColor() != 0) {
            for (int k = DEPTH_LAYERS; k >= 1; k--) {
                int c = Argb.scaleRgb(style.depthColor(), 0.64f - 0.035f * k);
                one(canvas, i, 0.13f * k, 0.22f * k, -0.3f * k, Argb.multiplyAlpha(c, Argb.alpha(main) / 255f));
            }
        }
        if (style.glowStrength() > 0.02f && style.glowColor() != 0) {
            float g = Math.min(1f, style.glowStrength()) * Argb.alpha(main) / 255f;
            int outer = Argb.withAlpha(style.glowColor(), g * 0.2f);
            int inner = Argb.withAlpha(style.glowColor(), g * 0.5f);
            for (int d = 0; d < GLOW_X.length; d++) one(canvas, i, GLOW_X[d] * 1.6f, GLOW_Y[d] * 1.6f, -0.2f, outer);
            for (int d = 0; d < GLOW_X.length; d++) one(canvas, i, GLOW_X[d] * 0.8f, GLOW_Y[d] * 0.8f, -0.1f, inner);
        }
        one(canvas, i, 0f, 0f, 0f, main);
    }

    private void one(WorldCanvas canvas, int i, float dx, float dy, float dz, int argb) {
        if (Argb.alpha(argb) < 4) return;
        float x = glyph.x() + glyph.offsetX + dx;
        float y = glyph.y() + glyph.offsetY + dy;
        float z = glyph.offsetZ + dz;
        if (!glyph.transformed() && z == 0f) {
            canvas.glyph(glyph.codepoint(), x, y, argb, light, seeThrough);
            return;
        }
        float halfAdvance = layout.advance(i) / 2f;
        float halfLine = layout.lineHeight() / 2f;
        canvas.push();
        canvas.translate(x + halfAdvance, y + halfLine, z);
        if (glyph.roll != 0f) canvas.rotate(roll.rotationZ(glyph.roll));
        if (glyph.scale != 1f) canvas.scale(glyph.scale, glyph.scale, glyph.scale);
        canvas.glyph(glyph.codepoint(), -halfAdvance, -halfLine, argb, light, seeThrough);
        canvas.pop();
    }

    private void background(WorldCanvas canvas, TextStyle style, float alpha) {
        float pad = style.padding();
        float x0 = -layout.width() / 2f - pad, x1 = layout.width() / 2f + pad;
        float y0 = -layout.height() / 2f - pad, y1 = layout.height() / 2f + pad;
        int c = Argb.multiplyAlpha(style.background(), alpha);
        canvas.beginQuads(RenderMode.TRANSLUCENT, null);
        canvas.vertex(x0, y0, BACKGROUND_DEPTH, 0, 0, c, light);
        canvas.vertex(x0, y1, BACKGROUND_DEPTH, 0, 1, c, light);
        canvas.vertex(x1, y1, BACKGROUND_DEPTH, 1, 1, c, light);
        canvas.vertex(x1, y0, BACKGROUND_DEPTH, 1, 0, c, light);
        canvas.endQuads();
    }
}
