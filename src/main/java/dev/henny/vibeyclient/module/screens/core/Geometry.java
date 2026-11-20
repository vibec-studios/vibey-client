package dev.henny.vibeyclient.module.screens.core;

public final class Geometry {
    public static final int MIN_WIDTH = 8;

    private Geometry() {}

    public static float logicalWidth(Box base, Placement p) {
        return p.width() > 0 ? p.width() : base.w();
    }

    public static Box place(Box base, Placement p, float screenW, float screenH, boolean scalable) {
        float s = scalable ? p.scale() : 1f;
        float vw = Math.round(logicalWidth(base, p) * s);
        float vh = Math.round(base.h() * s);
        if (p.pos() != null) {
            Box b = p.pos().resolve(vw, vh, screenW, screenH);
            return new Box(Math.round(b.x()), Math.round(b.y()), vw, vh);
        }
        return new Box(Math.round(base.cx() - vw / 2f), Math.round(base.cy() - vh / 2f), vw, vh);
    }

    public static Placement moved(Placement p, Box visual, float screenW, float screenH) {
        return p.withPos(Pos.of(visual, screenW, screenH));
    }

    public static Placement resized(Placement p, Box visual, float newVisualW, boolean fromLeft, float screenW, float screenH,
                                    boolean scalable) {
        float s = scalable ? p.scale() : 1f;
        int logical = Math.max(MIN_WIDTH, Math.round(newVisualW / s));
        float vw = Math.round(logical * s);
        float x = fromLeft ? visual.right() - vw : visual.x();
        return p.withWidth(logical).withPos(Pos.of(new Box(x, visual.y(), vw, visual.h()), screenW, screenH));
    }

    public static Placement scaled(Placement p, Box visual, float newScale, float screenW, float screenH) {
        float ratio = newScale / p.scale();
        float vw = visual.w() * ratio, vh = visual.h() * ratio;
        Box b = new Box(visual.cx() - vw / 2f, visual.cy() - vh / 2f, vw, vh);
        Placement out = p.withScale(newScale);
        return p.pos() == null ? out : out.withPos(Pos.of(b, screenW, screenH));
    }
}
