package dev.henny.r3.object;

import dev.henny.r3.render.TextureHandle;
import dev.henny.r3.text.TextWrap;

public final class VideoSurface implements ObjectContent {
    public static final float DEFAULT_BEZEL = 0.04f;
    public static final int DEFAULT_BEZEL_COLOR = 0xFF0E0E12;
    public static final int DEFAULT_PANEL_COLOR = 0xFF16161C;
    public static final int DEFAULT_STATUS_COLOR = 0xFFD8D8E0;

    private TextureHandle texture;
    private float u0 = 0f;
    private float v0 = 0f;
    private float u1 = 1f;
    private float v1 = 1f;
    private float bezel = DEFAULT_BEZEL;
    private int bezelColor = DEFAULT_BEZEL_COLOR;
    private int panelColor = DEFAULT_PANEL_COLOR;
    private int statusColor = DEFAULT_STATUS_COLOR;
    private final TextWrap statusWrap = new TextWrap();
    private String status;
    private long drawnFrames;

    public TextWrap statusWrap() {
        return statusWrap;
    }

    public TextureHandle texture() {
        return texture;
    }

    public VideoSurface texture(TextureHandle texture) {
        this.texture = texture;
        return this;
    }

    public VideoSurface crop(float u0, float v0, float u1, float v1) {
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
        return this;
    }

    public float u0() {
        return u0;
    }

    public float v0() {
        return v0;
    }

    public float u1() {
        return u1;
    }

    public float v1() {
        return v1;
    }

    public float aspect() {
        if (texture == null || texture.height() <= 0) return 16f / 9f;
        return texture.width() * Math.abs(u1 - u0) / (texture.height() * Math.abs(v1 - v0));
    }

    public float bezel() {
        return bezel;
    }

    public VideoSurface bezel(float meters, int argb) {
        this.bezel = Math.max(0f, meters);
        this.bezelColor = argb;
        return this;
    }

    public int bezelColor() {
        return bezelColor;
    }

    public int panelColor() {
        return panelColor;
    }

    public VideoSurface panelColor(int argb) {
        this.panelColor = argb;
        return this;
    }

    public int statusColor() {
        return statusColor;
    }

    public String status() {
        return status;
    }

    public VideoSurface status(String line, int argb) {
        this.status = line;
        this.statusColor = argb;
        return this;
    }

    public VideoSurface status(String line) {
        this.status = line;
        return this;
    }

    public boolean showsPicture() {
        return texture != null;
    }

    public long drawnFrames() {
        return drawnFrames;
    }

    public void markDrawn() {
        drawnFrames++;
    }

    @Override
    public boolean translucent() {
        return false;
    }
}
