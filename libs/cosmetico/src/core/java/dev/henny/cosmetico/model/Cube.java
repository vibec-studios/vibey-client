package dev.henny.cosmetico.model;

public final class Cube {
    public final float[] from, to;
    public final float inflate;
    public final boolean mirror;
    public final float[] pivot, rotation;
    public final float[] box;
    public final int texture;
    public final Face[] faces;

    public Cube(float[] from, float[] to, float inflate, boolean mirror, float[] pivot, float[] rotation, float[] box, int texture, Face[] faces) {
        this.from = from;
        this.to = to;
        this.inflate = inflate;
        this.mirror = mirror;
        this.pivot = pivot;
        this.rotation = rotation;
        this.box = box;
        this.texture = texture;
        this.faces = faces;
    }
}
