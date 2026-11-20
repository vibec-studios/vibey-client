package dev.henny.vui.mc.compat;

public final class Ids {
    private Ids() {
    }

    //? if >=1.21.11 {
    public static net.minecraft.resources.Identifier of(String namespace, String path) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(namespace, path);
    }
    //?} else {
    /*public static net.minecraft.resources.ResourceLocation of(String namespace, String path) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
    *///?}

    public static boolean valid(String namespace, String path) {
        return namespace.matches("[a-z0-9_.-]+") && path.matches("[a-z0-9_./-]+");
    }
}
