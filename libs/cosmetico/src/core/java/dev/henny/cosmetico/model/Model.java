package dev.henny.cosmetico.model;

import java.util.Map;

public final class Model {
    public final int texture;
    public final Bone[] bones;
    public final Map<String, Locator> locators;
    public final int cubeCount;

    public Model(int texture, Bone[] bones, Map<String, Locator> locators, int cubeCount) {
        this.texture = texture;
        this.bones = bones;
        this.locators = locators;
        this.cubeCount = cubeCount;
    }

    public Bone bone(String name) {
        for (Bone b : bones) if (b.name.equals(name)) return b;
        return null;
    }
}
