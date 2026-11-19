package dev.henny.cosmetico.mesh;

public final class MeshPart {
    public final int texture;
    public final MeshMode mode;
    public final float[] data;
    public final int quads;

    public MeshPart(int texture, MeshMode mode, float[] data, int quads) {
        this.texture = texture;
        this.mode = mode;
        this.data = data;
        this.quads = quads;
    }
}
