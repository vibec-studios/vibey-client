package dev.henny.companio.def;

public record LookDef(String bone, float maxYaw, float maxPitch, float speed) {
    public static final LookDef NONE = new LookDef(null, 0f, 0f, 240f);
}
