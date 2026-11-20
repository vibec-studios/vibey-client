package dev.henny.companio.mc.screen;

import com.mojang.blaze3d.platform.NativeImage;
import dev.henny.companio.mc.CompanioMod;
import dev.henny.companio.pack.Pack;
import dev.henny.vui.mc.VuiMc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

final class PackIcons {
    private static final Map<String, String> BY_HASH = new HashMap<>();
    private static int next;

    private PackIcons() {}

    static String of(Pack p) {
        if (p.icon() == null) return "";
        String key = p.location() + "#" + p.hash() + "#" + p.icon().length;
        String id = BY_HASH.get(key);
        if (id != null) return id;
        try {
            NativeImage img = NativeImage.read(p.icon());
            int w = img.getWidth(), h = img.getHeight();
            String path = "pack_icon/" + (next++);
            Identifier loc = Identifier.fromNamespaceAndPath(CompanioMod.ID, path);
            Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(() -> "companio pack icon", img));
            id = CompanioMod.ID + ":" + path;
            VuiMc.textures().register(id, loc, w, h);
            BY_HASH.put(key, id);
            return id;
        } catch (Exception e) {
            CompanioMod.LOG.debug("Pack icon of {} unreadable", p.location(), e);
            BY_HASH.put(key, "");
            return "";
        }
    }
}
