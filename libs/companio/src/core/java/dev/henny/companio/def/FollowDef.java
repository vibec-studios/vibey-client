package dev.henny.companio.def;

public record FollowDef(float near, float far, float run, float teleport) {
    public static final FollowDef DEFAULT = new FollowDef(1.8f, 4.5f, 9f, 24f);
}
