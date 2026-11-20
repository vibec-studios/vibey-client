package dev.henny.companio.def;

public record BoxDef(float width, float height) {
    public static final BoxDef DEFAULT = new BoxDef(0.6f, 0.6f);
}
