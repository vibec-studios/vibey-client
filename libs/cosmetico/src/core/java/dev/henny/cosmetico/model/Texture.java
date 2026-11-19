package dev.henny.cosmetico.model;

public final class Texture {
    public final String id;
    public final int width, height;
    public final byte[] png;
    public final int pngWidth, pngHeight;
    public final int frameCount;
    public final int frameTime;
    public final int[] frames;
    public final boolean interpolate;

    public Texture(String id, int width, int height, byte[] png, int pngWidth, int pngHeight, int frameCount, int frameTime,
                   int[] frames, boolean interpolate) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.png = png;
        this.pngWidth = pngWidth;
        this.pngHeight = pngHeight;
        this.frameCount = frameCount;
        this.frameTime = frameTime;
        this.frames = frames;
        this.interpolate = interpolate;
    }

    public boolean animated() {
        return frameCount > 1 && frames.length > 1;
    }

    public int cycleTicks() {
        return Math.max(1, frames.length * frameTime);
    }
}
