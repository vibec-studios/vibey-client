package dev.henny.vui.geom;

public final class Placement {
    private Placement() {
    }

    public static Rect place(Rect anchor, float w, float h, float viewportW, float viewportH, String side, String align, float gap) {
        String s = side == null ? "bottom" : side;
        Rect r = at(anchor, w, h, s, align, gap);
        if (!fits(r, viewportW, viewportH)) {
            Rect flipped = at(anchor, w, h, opposite(s), align, gap);
            if (fits(flipped, viewportW, viewportH) || overflow(flipped, viewportW, viewportH) < overflow(r, viewportW, viewportH)) r = flipped;
        }
        return r.clampInside(viewportW, viewportH);
    }

    public static Rect atPoint(float px, float py, float w, float h, float viewportW, float viewportH, float offset) {
        Rect r = new Rect(px + offset, py + offset, w, h);
        if (r.right() > viewportW) r = r.moved(px - offset - w, r.y());
        if (r.bottom() > viewportH) r = r.moved(r.x(), py - offset - h);
        return r.clampInside(viewportW, viewportH);
    }

    private static Rect at(Rect a, float w, float h, String side, String align, float gap) {
        return switch (side) {
            case "top" -> new Rect(alignX(a, w, align), a.y() - gap - h, w, h);
            case "left" -> new Rect(a.x() - gap - w, alignY(a, h, align), w, h);
            case "right" -> new Rect(a.right() + gap, alignY(a, h, align), w, h);
            case "center" -> new Rect(a.centerX() - w / 2f, a.centerY() - h / 2f, w, h);
            default -> new Rect(alignX(a, w, align), a.bottom() + gap, w, h);
        };
    }

    private static float alignX(Rect a, float w, String align) {
        if ("center".equals(align)) return a.centerX() - w / 2f;
        if ("end".equals(align)) return a.right() - w;
        return a.x();
    }

    private static float alignY(Rect a, float h, String align) {
        if ("center".equals(align)) return a.centerY() - h / 2f;
        if ("end".equals(align)) return a.bottom() - h;
        return a.y();
    }

    private static String opposite(String side) {
        return switch (side) {
            case "top" -> "bottom";
            case "left" -> "right";
            case "right" -> "left";
            case "bottom" -> "top";
            default -> side;
        };
    }

    private static boolean fits(Rect r, float vw, float vh) {
        return r.x() >= 0 && r.y() >= 0 && r.right() <= vw && r.bottom() <= vh;
    }

    private static float overflow(Rect r, float vw, float vh) {
        return Math.max(0, -r.x()) + Math.max(0, -r.y()) + Math.max(0, r.right() - vw) + Math.max(0, r.bottom() - vh);
    }
}
