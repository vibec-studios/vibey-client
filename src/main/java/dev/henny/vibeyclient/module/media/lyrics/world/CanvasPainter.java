package dev.henny.vibeyclient.module.media.lyrics.world;

import dev.henny.r3.render.Light;
import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.WorldCanvas;
import dev.henny.vibeyclient.module.media.lyrics.render.LyricLayout;
import dev.henny.vibeyclient.module.media.lyrics.render.Painter;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.joml.Quaternionf;

final class CanvasPainter extends Painter {
    private static final float BOX_Z = -0.7f;

    private final Quaternionf turn = new Quaternionf();
    private WorldCanvas canvas;
    private String fontId;
    private boolean bold;

    CanvasPainter(Font font) {
        super(font);
    }

    CanvasPainter target(WorldCanvas canvas, String fontId, boolean bold) {
        this.canvas = canvas;
        this.fontId = fontId;
        this.bold = bold;
        return this;
    }

    @Override
    protected void draw(Component c, LyricLayout.Run r, float ox, float oy, float oz, int argb, boolean back) {
        float s = r.scale();
        canvas.push();
        canvas.translate(r.x(), r.y() + LyricLayout.ROW * s / 2f, r.z());
        if (r.yaw() != 0f || r.roll() != 0f) {
            canvas.rotate(turn.rotationYXZ((float) Math.toRadians(r.yaw()), 0f, (float) Math.toRadians(r.roll())));
        }
        canvas.scale(s, s, s);
        canvas.translate(-font.width(c) / 2f + ox, -LyricLayout.ROW / 2f + oy, oz);
        canvas.styledText(c.getString(), fontId, bold, 0, 0, argb, Light.FULL_BRIGHT, !back);
        canvas.pop();
    }

    @Override
    protected void box(float x1, float y1, float x2, float y2, int argb) {
        canvas.beginQuads(RenderMode.TRANSLUCENT, null);
        canvas.vertex(x1, y1, BOX_Z, 0, 0, argb, Light.FULL_BRIGHT);
        canvas.vertex(x1, y2, BOX_Z, 0, 1, argb, Light.FULL_BRIGHT);
        canvas.vertex(x2, y2, BOX_Z, 1, 1, argb, Light.FULL_BRIGHT);
        canvas.vertex(x2, y1, BOX_Z, 1, 0, argb, Light.FULL_BRIGHT);
        canvas.endQuads();
    }
}
