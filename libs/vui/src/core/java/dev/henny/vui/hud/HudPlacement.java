package dev.henny.vui.hud;

import dev.henny.vui.geom.Rect;
import dev.henny.vui.layout.Anchor;

public record HudPlacement(Anchor anchor, float offsetX, float offsetY, float scale, boolean visible) {
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 4f;

    public static HudPlacement at(Anchor anchor, float x, float y) {
        return new HudPlacement(anchor, x, y, 1f, true);
    }

    public Rect rect(float width, float height, float viewportW, float viewportH) {
        float w = width * scale;
        float h = height * scale;
        float x = anchor.x(0, viewportW, w, offsetX);
        float y = anchor.y(0, viewportH, h, offsetY);
        return new Rect(x, y, w, h).clampInside(viewportW, viewportH);
    }

    public static HudPlacement fromRect(Rect r, float scale, float viewportW, float viewportH, boolean visible) {
        Anchor a = Anchor.nearest(r.centerX(), r.centerY(), viewportW, viewportH);
        float baseX = a.x(0, viewportW, r.width(), 0);
        float baseY = a.y(0, viewportH, r.height(), 0);
        return new HudPlacement(a, Math.round(r.x() - baseX), Math.round(r.y() - baseY), scale, visible);
    }

    public HudPlacement withScale(float next) {
        return new HudPlacement(anchor, offsetX, offsetY, Math.max(MIN_SCALE, Math.min(MAX_SCALE, next)), visible);
    }

    public HudPlacement withVisible(boolean on) {
        return new HudPlacement(anchor, offsetX, offsetY, scale, on);
    }
}
