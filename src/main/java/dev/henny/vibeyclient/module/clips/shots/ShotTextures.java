package dev.henny.vibeyclient.module.clips.shots;

import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.mc.texture.DynamicTextureHandle;
import dev.henny.r3.render.TextureHandle;
import dev.henny.vui.mc.VuiMc;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

final class ShotTextures {
    private static final Map<String, DynamicTextureHandle> HANDLES = new HashMap<>();
    private static int next;

    private ShotTextures() {}

    static String upload(ShotDecoder.Pixels p) {
        String name = "vibeyclient/shots/" + (++next);
        DynamicTextureHandle h = R3Mc.dynamicTexture(name, p.w(), p.h());
        h.upload(p.argb());
        String id = "vibeyclient:shots/" + next;
        VuiMc.textures().register(id, Identifier.parse(h.textureId()), p.w(), p.h());
        HANDLES.put(id, h);
        return id;
    }

    static TextureHandle handle(String id) {
        return HANDLES.get(id);
    }

    static void release(String id) {
        DynamicTextureHandle h = HANDLES.remove(id);
        if (h == null) return;
        h.close();
        VuiMc.textures().unregister(id);
    }

    static int count() {
        return HANDLES.size();
    }
}
