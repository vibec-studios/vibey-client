package dev.henny.vui.mc.draw;

import dev.henny.vui.mc.compat.Mc;
import dev.henny.vui.mc.texture.McTextures;
import dev.henny.vui.mc.texture.TextureEntry;
import dev.henny.vui.render.Canvas;
import dev.henny.vui.render.Cursor;
import dev.henny.vui.render.ScissorStack;

public final class McCanvas implements Canvas {
    private static final int PLACEHOLDER = 0x80FF00FF;

    private final GuiTarget target = new GuiTarget();
    private final Affine t = new Affine();
    private final Snap snap = new Snap();
    private final TextStrings strings = new TextStrings();
    private final BoldText bold = new BoldText();
    private final ScissorStack scissors = new ScissorStack();
    private final AlphaStack alphas = new AlphaStack();
    private final Quads quads = new Quads(target);
    private final McTextures textures;
    private int nativeScissors;
    private boolean placeholders;

    public McCanvas(McTextures textures) {
        this.textures = textures;
    }

    public McCanvas placeholders(boolean on) {
        placeholders = on;
        return this;
    }

    public McCanvas begin(Object graphics) {
        target.bind(graphics, Mc.font());
        t.reset();
        snap.scale(Mc.guiScale());
        alphas.reset();
        while (scissors.active()) scissors.pop();
        nativeScissors = 0;
        return this;
    }

    public void end() {
        while (nativeScissors > 0) {
            target.endScissor();
            nativeScissors--;
        }
        target.unwind();
    }

    public void cursor(Cursor cursor) {
        target.cursor(cursor);
    }

    @Override
    public void fill(float x, float y, float w, float h, int argb) {
        int c = Colors.alpha(argb, alphas.value());
        if (w <= 0 || h <= 0 || Colors.alphaOf(c) == 0) return;
        if (!t.axisAligned()) {
            quads.local(x, y, w, h, c, c, false);
            return;
        }
        if (!snap.rect(t, x, y, w, h)) return;
        if (t.unitTranslation() && snap.whole()) {
            target.fill(local(snap.x1, t.tx), local(snap.y1, t.ty), local(snap.x2, t.tx), local(snap.y2, t.ty), c);
            return;
        }
        quads.absolute(snap, c, c, false);
    }

    @Override
    public void gradient(float x, float y, float w, float h, int from, int to, boolean horizontal) {
        float a = alphas.value();
        int c1 = Colors.alpha(from, a);
        int c2 = Colors.alpha(to, a);
        if (w <= 0 || h <= 0 || (Colors.alphaOf(c1) == 0 && Colors.alphaOf(c2) == 0)) return;
        if (!t.axisAligned()) {
            quads.local(x, y, w, h, c1, c2, horizontal);
            return;
        }
        if (!snap.rect(t, x, y, w, h)) return;
        if (!horizontal && t.unitTranslation() && snap.whole()) {
            target.gradient(local(snap.x1, t.tx), local(snap.y1, t.ty), local(snap.x2, t.tx), local(snap.y2, t.ty), c1, c2);
            return;
        }
        quads.absolute(snap, c1, c2, horizontal);
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float width, int argb) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float half = width / 2f;
        if (dy == 0) {
            fill(Math.min(x1, x2), y1 - half, Math.abs(dx), width, argb);
            return;
        }
        if (dx == 0) {
            fill(x1 - half, Math.min(y1, y2), width, Math.abs(dy), argb);
            return;
        }
        push();
        translate(x1, y1);
        rotate((float) Math.atan2(dy, dx));
        fill(0, -half, (float) Math.sqrt(dx * dx + dy * dy), width, argb);
        pop();
    }

    @Override
    public void text(CharSequence text, int start, int end, float x, float y, int argb, boolean shadow, boolean isBold) {
        if (end <= start) return;
        int c = Colors.alpha(argb, alphas.value());
        if (Colors.alphaOf(c) < 4) return;
        draw(strings.of(text, start, end), x, y, c, shadow, isBold);
    }

    @Override
    public void glyph(int codepoint, float x, float y, int argb, boolean shadow, boolean isBold) {
        if (codepoint == ' ') return;
        int c = Colors.alpha(argb, alphas.value());
        if (Colors.alphaOf(c) < 4) return;
        draw(strings.glyph(codepoint), x, y, c, shadow, isBold);
    }

    private void draw(String s, float x, float y, int c, boolean shadow, boolean isBold) {
        if (!t.axisAligned()) {
            target.push();
            target.translate(x, y);
            drawText(s, 0, 0, c, shadow, isBold);
            target.pop();
            return;
        }
        float ax = snap.px(t.x(x, y));
        float ay = snap.px(t.y(x, y));
        if (t.unitTranslation() && Affine.whole(ax) && Affine.whole(ay)) {
            drawText(s, local(ax, t.tx), local(ay, t.ty), c, shadow, isBold);
            return;
        }
        target.pushBase();
        target.translate(ax, ay);
        target.scale(t.a, t.d);
        drawText(s, 0, 0, c, shadow, isBold);
        target.pop();
    }

    private void drawText(String s, int x, int y, int c, boolean shadow, boolean isBold) {
        if (isBold) target.text(bold.of(s), x, y, c, shadow);
        else target.text(s, x, y, c, shadow);
    }

    @Override
    public void texture(String id, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int tint) {
        if (w <= 0 || h <= 0) return;
        TextureEntry e = textures.entry(id);
        if (e == null) {
            if (placeholders) fill(x, y, w, h, PLACEHOLDER);
            return;
        }
        int c = Colors.alpha(tint, alphas.value());
        if (Colors.alphaOf(c) == 0) return;
        int tw = e.width();
        int th = e.height();
        float u = u0 * tw;
        float v = v0 * th;
        int uw = Math.round((u1 - u0) * tw);
        int vh = Math.round((v1 - v0) * th);
        if (uw <= 0 || vh <= 0) return;
        if (!t.axisAligned()) {
            target.push();
            target.translate(x, y);
            target.scale(w / uw, h / vh);
            target.blit(e.id(), 0, 0, uw, vh, u, v, uw, vh, tw, th, c);
            target.pop();
            return;
        }
        if (!snap.rect(t, x, y, w, h)) return;
        if (t.unitTranslation() && snap.whole()) {
            int lx = local(snap.x1, t.tx);
            int ly = local(snap.y1, t.ty);
            target.blit(e.id(), lx, ly, local(snap.x2, t.tx) - lx, local(snap.y2, t.ty) - ly, u, v, uw, vh, tw, th, c);
            return;
        }
        target.pushBase();
        target.translate(snap.x1, snap.y1);
        target.scale((snap.x2 - snap.x1) / uw, (snap.y2 - snap.y1) / vh);
        target.blit(e.id(), 0, 0, uw, vh, u, v, uw, vh, tw, th, c);
        target.pop();
    }

    @Override
    public void nineSlice(String id, float x, float y, float w, float h, int slice, int textureWidth, int textureHeight, int tint) {
        NineSlices.draw(this, id, x, y, w, h, slice, textureWidth, textureHeight, tint);
    }

    @Override
    public void pushScissor(float x, float y, float w, float h) {
        float ax = t.x(x, y), ay = t.y(x, y);
        float bx = t.x(x + w, y), by = t.y(x + w, y);
        float cx = t.x(x, y + h), cy = t.y(x, y + h);
        float dx = t.x(x + w, y + h), dy = t.y(x + w, y + h);
        float minX = Math.min(Math.min(ax, bx), Math.min(cx, dx));
        float minY = Math.min(Math.min(ay, by), Math.min(cy, dy));
        float maxX = Math.max(Math.max(ax, bx), Math.max(cx, dx));
        float maxY = Math.max(Math.max(ay, by), Math.max(cy, dy));
        scissors.push(minX, minY, maxX - minX, maxY - minY);
        target.scissor(Math.round(scissors.x1()), Math.round(scissors.y1()), Math.round(scissors.x2()), Math.round(scissors.y2()));
        nativeScissors++;
    }

    @Override
    public void popScissor() {
        scissors.pop();
        if (nativeScissors == 0) return;
        target.endScissor();
        nativeScissors--;
    }

    @Override
    public void push() {
        t.push();
        target.push();
    }

    @Override
    public void pop() {
        t.pop();
        target.pop();
    }

    @Override
    public void translate(float x, float y) {
        t.translate(x, y);
        target.translate(x, y);
    }

    @Override
    public void scale(float sx, float sy) {
        t.scale(sx, sy);
        target.scale(sx, sy);
    }

    @Override
    public void rotate(float radians) {
        t.rotate(radians);
        target.rotate(radians);
    }

    @Override
    public void pushAlpha(float alpha) {
        alphas.push(alpha);
    }

    @Override
    public void popAlpha() {
        alphas.pop();
    }

    @Override
    public void layer() {
        target.layer();
    }

    private static int local(float absolute, float translation) {
        return Math.round(absolute - translation);
    }
}
