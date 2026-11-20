package dev.henny.vui.mc.draw;

final class Quads {
    private static final float QUARTER_TURN = (float) (Math.PI / 2);

    private final GuiTarget target;

    Quads(GuiTarget target) {
        this.target = target;
    }

    void local(float x, float y, float w, float h, int c1, int c2, boolean horizontal) {
        target.push();
        target.translate(x, y);
        unit(w, h, c1, c2, horizontal);
        target.pop();
    }

    void absolute(Snap s, int c1, int c2, boolean horizontal) {
        target.pushBase();
        target.translate(s.x1, s.y1);
        unit(s.x2 - s.x1, s.y2 - s.y1, c1, c2, horizontal);
        target.pop();
    }

    private void unit(float w, float h, int c1, int c2, boolean horizontal) {
        if (horizontal) {
            target.translate(0, h);
            target.rotate(-QUARTER_TURN);
            target.scale(h, w);
            target.gradient(0, 0, 1, 1, c1, c2);
            return;
        }
        target.scale(w, h);
        if (c1 == c2) target.fill(0, 0, 1, 1, c1);
        else target.gradient(0, 0, 1, 1, c1, c2);
    }
}
