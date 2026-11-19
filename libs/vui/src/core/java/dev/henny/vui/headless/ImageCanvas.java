package dev.henny.vui.headless;

import dev.henny.vui.render.Canvas;
import dev.henny.vui.render.ScissorStack;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

public final class ImageCanvas implements Canvas {
    private final BufferedImage image;
    private final Graphics2D g;
    private final int scale;
    private final HeadlessTextures textures;
    private final Deque<AffineTransform> transforms = new ArrayDeque<>();
    private final Deque<Float> alphas = new ArrayDeque<>();
    private final ScissorStack scissors = new ScissorStack();
    private float alpha = 1f;

    public ImageCanvas(int width, int height, int scale, HeadlessTextures textures, int background) {
        this.scale = Math.max(1, scale);
        this.textures = textures;
        this.image = new BufferedImage(width * this.scale, height * this.scale, BufferedImage.TYPE_INT_ARGB);
        this.g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(new Color(background, true));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.scale(this.scale, this.scale);
    }

    public BufferedImage image() {
        return image;
    }

    public byte[] png() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    public void write(Path file) throws IOException {
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        Files.write(file, png());
    }

    public int pixel(int x, int y) {
        return image.getRGB(x * scale, y * scale);
    }

    @Override
    public void fill(float x, float y, float w, float h, int argb) {
        if (w <= 0 || h <= 0) return;
        g.setColor(color(argb));
        g.fill(new Rectangle2D.Float(x, y, w, h));
    }

    @Override
    public void gradient(float x, float y, float w, float h, int from, int to, boolean horizontal) {
        if (w <= 0 || h <= 0) return;
        float x2 = horizontal ? x + w : x;
        float y2 = horizontal ? y : y + h;
        g.setPaint(new GradientPaint(x, y, color(from), x2, y2, color(to)));
        g.fill(new Rectangle2D.Float(x, y, w, h));
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float width, int argb) {
        g.setColor(color(argb));
        g.setStroke(new BasicStroke(width));
        g.draw(new Line2D.Float(x1, y1, x2, y2));
    }

    @Override
    public void text(CharSequence text, int start, int end, float x, float y, int argb, boolean shadow, boolean bold) {
        float pen = x;
        for (int i = start; i < end; i++) {
            glyph(text.charAt(i), pen, y, argb, shadow, bold);
            pen += PixelFont.ADVANCE + (bold ? 1 : 0);
        }
    }

    @Override
    public void glyph(int codepoint, float x, float y, int argb, boolean shadow, boolean bold) {
        if (codepoint == ' ') return;
        if (shadow) GlyphPainter.draw(g, codepoint, x + 1, y + 1, color(shade(argb)), bold);
        GlyphPainter.draw(g, codepoint, x, y, color(argb), bold);
    }

    @Override
    public void texture(String id, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int tint) {
        BufferedImage tex = textures == null ? null : textures.image(id);
        if (tex == null) {
            Placeholder.draw(g, x, y, w, h);
            return;
        }
        int sx1 = Math.round(u0 * tex.getWidth());
        int sy1 = Math.round(v0 * tex.getHeight());
        int sx2 = Math.round(u1 * tex.getWidth());
        int sy2 = Math.round(v1 * tex.getHeight());
        g.drawImage(tex, Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), sx1, sy1, sx2, sy2, null);
    }

    @Override
    public void nineSlice(String id, float x, float y, float w, float h, int slice, int textureWidth, int textureHeight, int tint) {
        NineSlice.draw(this, id, x, y, w, h, slice, textureWidth, textureHeight, tint);
    }

    @Override
    public void pushScissor(float x, float y, float w, float h) {
        Rectangle device = g.getTransform().createTransformedShape(new Rectangle2D.Float(x, y, w, h)).getBounds();
        scissors.push(device.x, device.y, device.width, device.height);
        applyClip();
    }

    @Override
    public void popScissor() {
        scissors.pop();
        applyClip();
    }

    private void applyClip() {
        AffineTransform t = g.getTransform();
        g.setTransform(new AffineTransform());
        if (scissors.active()) {
            g.setClip(Math.round(scissors.x1()), Math.round(scissors.y1()), Math.round(scissors.x2() - scissors.x1()),
                    Math.round(scissors.y2() - scissors.y1()));
        } else {
            g.setClip(null);
        }
        g.setTransform(t);
    }

    @Override
    public void push() {
        transforms.push(g.getTransform());
    }

    @Override
    public void pop() {
        if (!transforms.isEmpty()) g.setTransform(transforms.pop());
    }

    @Override
    public void translate(float x, float y) {
        g.translate(x, y);
    }

    @Override
    public void scale(float sx, float sy) {
        g.scale(sx, sy);
    }

    @Override
    public void rotate(float radians) {
        g.rotate(radians);
    }

    @Override
    public void pushAlpha(float a) {
        alphas.push(alpha);
        alpha *= a;
    }

    @Override
    public void popAlpha() {
        if (!alphas.isEmpty()) alpha = alphas.pop();
    }

    private Color color(int argb) {
        int a = Math.round((argb >>> 24) * alpha);
        return new Color((a << 24) | (argb & 0xFFFFFF), true);
    }

    private static int shade(int argb) {
        int r = ((argb >> 16) & 0xFF) / 4;
        int gg = ((argb >> 8) & 0xFF) / 4;
        int b = (argb & 0xFF) / 4;
        return (argb & 0xFF000000) | (r << 16) | (gg << 8) | b;
    }
}
