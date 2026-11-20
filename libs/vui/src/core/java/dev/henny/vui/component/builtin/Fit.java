package dev.henny.vui.component.builtin;

import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.paint.PaintContext;
import dev.henny.vui.render.TextureInfo;

final class Fit {
    private Fit() {
    }

    static void draw(PaintContext ctx, String id, TextureInfo info, LayoutBox b, boolean cover, int tint) {
        float bw = b.innerWidth();
        float bh = b.innerHeight();
        float imageAspect = info.width() / (float) info.height();
        float boxAspect = bw / Math.max(1e-3f, bh);
        if (!cover) {
            float w = imageAspect > boxAspect ? bw : bh * imageAspect;
            float h = imageAspect > boxAspect ? bw / imageAspect : bh;
            ctx.canvas().texture(id, b.innerX() + (bw - w) / 2f, b.innerY() + (bh - h) / 2f, w, h, 0, 0, 1, 1, tint);
            return;
        }
        float u0 = 0;
        float v0 = 0;
        float u1 = 1;
        float v1 = 1;
        if (imageAspect > boxAspect) {
            float visible = boxAspect / imageAspect;
            u0 = (1 - visible) / 2f;
            u1 = u0 + visible;
        } else {
            float visible = imageAspect / boxAspect;
            v0 = (1 - visible) / 2f;
            v1 = v0 + visible;
        }
        ctx.canvas().texture(id, b.innerX(), b.innerY(), bw, bh, u0, v0, u1, v1, tint);
    }
}
