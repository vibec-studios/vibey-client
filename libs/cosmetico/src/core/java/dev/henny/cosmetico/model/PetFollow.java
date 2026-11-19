package dev.henny.cosmetico.model;

public enum PetFollow {
    GROUND, SHOULDER_RIGHT, SHOULDER_LEFT, FLOAT;

    public static PetFollow of(String s) {
        if (s != null) for (PetFollow f : values()) if (f.name().equalsIgnoreCase(s)) return f;
        return GROUND;
    }
}
