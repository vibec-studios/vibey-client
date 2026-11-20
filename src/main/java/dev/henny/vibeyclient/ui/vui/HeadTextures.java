package dev.henny.vibeyclient.ui.vui;

import dev.henny.r3.mc.texture.DynamicTextureHandle;
import dev.henny.vibeyclient.ui.toast.Heads;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.mc.compat.Ids;

import java.util.IdentityHashMap;
import java.util.Map;

public final class HeadTextures {
    private HeadTextures() {}

    private static final Map<int[], String> BY_FACE = new IdentityHashMap<>();

    public static String of(String userId, String skinUrl) {
        int[] face = Heads.face(userId, skinUrl);
        if (face == null) return "";
        String known = BY_FACE.get(face);
        if (known != null) return known;
        String key = "head_" + Integer.toHexString(System.identityHashCode(face));
        DynamicTextureHandle handle = new DynamicTextureHandle(key, 8, 8);
        handle.upload(face);
        String id = "vibeyclient:head/" + key;
        String loc = handle.textureId();
        int c = loc.indexOf(':');
        VuiMc.textures().register(id, Ids.of(loc.substring(0, c), loc.substring(c + 1)), 8, 8);
        BY_FACE.put(face, id);
        return id;
    }
}
