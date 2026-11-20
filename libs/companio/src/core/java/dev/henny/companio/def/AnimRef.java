package dev.henny.companio.def;

public record AnimRef(String name, Blend blend, float fade, float speed) {
    public static AnimRef layer(String name) {
        return new AnimRef(name, Blend.ADD, 0.15f, 1f);
    }
}
