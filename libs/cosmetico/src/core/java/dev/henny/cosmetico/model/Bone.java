package dev.henny.cosmetico.model;

public final class Bone {
    public final String name;
    public final int index;
    public final int parent;
    public final Attach attach;
    public final float[] pivot, rotation;
    public final Cube[] cubes;
    public final boolean emissive, translucent;
    public final boolean effectiveEmissive, effectiveTranslucent;

    public Bone(String name, int index, int parent, Attach attach, float[] pivot, float[] rotation, Cube[] cubes,
                boolean emissive, boolean translucent, boolean effectiveEmissive, boolean effectiveTranslucent) {
        this.name = name;
        this.index = index;
        this.parent = parent;
        this.attach = attach;
        this.pivot = pivot;
        this.rotation = rotation;
        this.cubes = cubes;
        this.emissive = emissive;
        this.translucent = translucent;
        this.effectiveEmissive = effectiveEmissive;
        this.effectiveTranslucent = effectiveTranslucent;
    }
}
