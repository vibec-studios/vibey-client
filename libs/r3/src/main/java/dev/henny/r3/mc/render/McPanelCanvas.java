package dev.henny.r3.mc.render;

import dev.henny.r3.math.Argb;
import dev.henny.r3.mc.texture.McTextures;
import dev.henny.r3.render.PanelCanvas;
import dev.henny.r3.render.TextMetrics;
import dev.henny.r3.render.TextureHandle;
import dev.henny.vui.render.ScissorStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

final class McPanelCanvas implements PanelCanvas {
    static final float Z_STEP = 0.012f;
    static final int Z_STEPS = 160;

    private final Matrix4f world = new Matrix4f();
    private final Matrix4f local = new Matrix4f();
    private final Matrix4f pose = new Matrix4f();
    private final Affine2D affine = new Affine2D();
    private final ScissorStack scissors = new ScissorStack();
    private final PanelQuads quads = new PanelQuads();
    private final PanelText text = new PanelText();
    private final float[] line = new float[8];
    private DrawList list;
    private int width;
    private int height;
    private int light;
    private float baseAlpha;
    private float alpha;
    private int ops;

    void begin(Matrix4fc toWorld, int width, int height, int light, float alpha, DrawList list) {
        world.set(toWorld);
        this.width = width;
        this.height = height;
        this.light = light;
        this.baseAlpha = alpha;
        this.alpha = 1f;
        this.list = list;
        this.ops = 0;
        affine.reset();
        while (scissors.active()) scissors.pop();
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public TextMetrics metrics() {
        return McFontMetrics.INSTANCE;
    }

    @Override
    public void fill(float x, float y, float w, float h, int argb) {
        int c = color(argb);
        if (Argb.alpha(c) == 0) return;
        quads.rect(list, world, affine, scissors, z(), light, null, x, y, w, h, 0, 0, 1, 1, c, c);
    }

    @Override
    public void gradient(float x, float y, float w, float h, int topArgb, int bottomArgb) {
        quads.rect(list, world, affine, scissors, z(), light, null, x, y, w, h, 0, 0, 1, 1, color(topArgb), color(bottomArgb));
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float thickness, int argb) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) return;
        float nx = -dy / len * thickness / 2f, ny = dx / len * thickness / 2f;
        line[0] = x1 + nx; line[1] = y1 + ny;
        line[2] = x1 - nx; line[3] = y1 - ny;
        line[4] = x2 - nx; line[5] = y2 - ny;
        line[6] = x2 + nx; line[7] = y2 + ny;
        quads.quad(list, world, affine, z(), light, line, color(argb));
    }

    @Override
    public void text(String s, float x, float y, int argb) {
        text(s, x, y, argb, false, false);
    }

    @Override
    public void text(String s, float x, float y, int argb, boolean shadow, boolean bold) {
        int c = color(argb);
        if (Argb.alpha(c) < 4 || s.isEmpty()) return;
        Matrix4f p = pose.set(world).mul(affine.toMatrix(z(), local));
        text.draw(list, p, affine, scissors, s, x, y, c, light, shadow, bold);
    }

    @Override
    public void texture(TextureHandle texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int tint) {
        String id = McTextures.id(texture);
        if (id == null) return;
        int c = color(tint);
        quads.rect(list, world, affine, scissors, z(), light, id, x, y, w, h, u0, v0, u1, v1, c, c);
    }

    @Override
    public void pushScissor(float x, float y, float w, float h) {
        float ax = affine.x(x, y), ay = affine.y(x, y);
        float bx = affine.x(x + w, y + h), by = affine.y(x + w, y + h);
        scissors.push(Math.min(ax, bx), Math.min(ay, by), Math.abs(bx - ax), Math.abs(by - ay));
    }

    @Override
    public void popScissor() {
        scissors.pop();
    }

    @Override
    public void push() {
        affine.push();
    }

    @Override
    public void pop() {
        affine.pop();
    }

    @Override
    public void translate(float x, float y) {
        affine.translate(x, y);
    }

    @Override
    public void scale(float x, float y) {
        affine.scale(x, y);
    }

    @Override
    public void rotate(float radians) {
        affine.rotate(radians);
    }

    @Override
    public void alpha(float alpha) {
        this.alpha = alpha;
    }

    private int color(int argb) {
        return Argb.multiplyAlpha(argb, baseAlpha * alpha);
    }

    private float z() {
        return Math.min(ops++, Z_STEPS) * Z_STEP;
    }
}
