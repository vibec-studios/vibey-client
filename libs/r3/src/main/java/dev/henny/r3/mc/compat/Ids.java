package dev.henny.r3.mc.compat;

//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else
/*import net.minecraft.resources.ResourceLocation;*/

public final class Ids {
    private Ids() {}

    //? if >=1.21.11 {
    public static Identifier parse(String id) {
        return Identifier.tryParse(id);
    }

    public static Identifier of(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
    //?} else {
    /*public static ResourceLocation parse(String id) {
        return ResourceLocation.tryParse(id);
    }

    public static ResourceLocation of(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
    *///?}

    public static String texturePath(String id) {
        int colon = id.indexOf(':');
        String ns = colon < 0 ? "minecraft" : id.substring(0, colon);
        String path = colon < 0 ? id : id.substring(colon + 1);
        if (path.startsWith("textures/") && path.endsWith(".png")) return ns + ":" + path;
        return ns + ":textures/" + path + (path.endsWith(".png") ? "" : ".png");
    }
}
