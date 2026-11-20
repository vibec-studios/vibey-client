package dev.henny.r3.mc.render;

import dev.henny.vui.render.ScissorStack;
import org.joml.Matrix4f;

final class PanelText {
    void draw(DrawList list, Matrix4f pose, Affine2D affine, ScissorStack scissors, String s, float x, float y,
              int argb, int light, boolean shadow, boolean bold) {
        if (!scissors.active()) {
            list.text(pose, s, x, y, argb, light, TextMode.POLYGON_OFFSET, shadow, bold);
            return;
        }
        McFontMetrics m = McFontMetrics.INSTANCE;
        float h = m.lineHeight();
        float w = m.width(s);
        int inside = inside(affine, scissors, x, y, w, h);
        if (inside == 0) return;
        if (inside == 2) {
            list.text(pose, s, x, y, argb, light, TextMode.POLYGON_OFFSET, shadow, bold);
            return;
        }
        float gx = x;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            int n = Character.charCount(cp);
            float gw = m.glyphWidth(cp, bold);
            if (inside(affine, scissors, gx, y, gw, h) == 2) {
                list.text(pose, s.substring(i, i + n), gx, y, argb, light, TextMode.POLYGON_OFFSET, shadow, bold);
            }
            gx += gw;
            i += n;
        }
    }

    private static int inside(Affine2D affine, ScissorStack s, float x, float y, float w, float h) {
        float ax = affine.x(x, y), ay = affine.y(x, y);
        float bx = affine.x(x + w, y + h), by = affine.y(x + w, y + h);
        float x0 = Math.min(ax, bx), x1 = Math.max(ax, bx), y0 = Math.min(ay, by), y1 = Math.max(ay, by);
        if (x1 <= s.x1() || x0 >= s.x2() || y1 <= s.y1() || y0 >= s.y2()) return 0;
        if (x0 >= s.x1() - 0.01f && x1 <= s.x2() + 0.01f && y0 >= s.y1() - 0.01f && y1 <= s.y2() + 0.01f) return 2;
        return 1;
    }
}
