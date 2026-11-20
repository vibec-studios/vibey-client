package dev.henny.cosmetico.registry;

public record ItemSource(String id, int priority) {
    public static final ItemSource BUILTIN = new ItemSource("builtin", 0);
    public static final ItemSource REMOTE = new ItemSource("remote", 100);
    public static final ItemSource DEV = new ItemSource("dev", 1000);
}
