package dev.henny.cosmetico.mc.compat;

import dev.henny.cosmetico.mc.CosmeticoMod;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else
/*import net.minecraft.resources.ResourceLocation;*/

public final class McIds {
    private McIds() {}

    //? if >=1.21.11 {
    public static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CosmeticoMod.ID, path);
    }

    public static Identifier parse(String id) {
        int c = id.indexOf(':');
        return c < 0 ? Identifier.withDefaultNamespace(id) : id(id.substring(0, c), id.substring(c + 1));
    }
    //?} else {
    /*public static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CosmeticoMod.ID, path);
    }

    public static ResourceLocation parse(String id) {
        int c = id.indexOf(':');
        return c < 0 ? ResourceLocation.withDefaultNamespace(id) : id(id.substring(0, c), id.substring(c + 1));
    }
    *///?}

    public static boolean isOurs(Object id) {
        return id != null && id.toString().startsWith(CosmeticoMod.ID + ":");
    }
}
