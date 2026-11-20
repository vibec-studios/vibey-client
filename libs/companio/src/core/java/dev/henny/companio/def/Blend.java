package dev.henny.companio.def;

public enum Blend {
    ADD, REPLACE;

    public static Blend of(String id) {
        return "replace".equals(id) ? REPLACE : "add".equals(id) ? ADD : null;
    }
}
