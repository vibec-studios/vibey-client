package dev.henny.r3.mc.render;

import dev.henny.r3.mc.compat.Ids;
import dev.henny.r3.mc.texture.WhiteTexture;
import dev.henny.r3.render.RenderMode;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else
/*import net.minecraft.client.renderer.RenderType;*/

final class RenderTypeCompat {
    private static final Map<RenderMode, Map<String, RenderType>> CACHE = new EnumMap<>(RenderMode.class);

    private RenderTypeCompat() {}

    static RenderType of(RenderMode mode, String texture) {
        if (mode == RenderMode.ADDITIVE && texture == null) return lightning();
        String id = texture == null ? WhiteTexture.ID : texture;
        Map<String, RenderType> byTexture = CACHE.computeIfAbsent(mode, m -> new HashMap<>());
        RenderType type = byTexture.get(id);
        if (type == null) {
            type = create(mode, id);
            byTexture.put(id, type);
        }
        return type;
    }

    private static RenderType create(RenderMode mode, String id) {
        //? if >=1.21.11 {
        if (mode == RenderMode.SEE_THROUGH) return RenderTypes.textSeeThrough(Ids.parse(id));
        return mode == RenderMode.ADDITIVE ? RenderTypes.eyes(Ids.parse(id)) : RenderTypes.text(Ids.parse(id));
        //?} else {
        /*if (mode == RenderMode.SEE_THROUGH) return RenderType.textSeeThrough(Ids.parse(id));
        return mode == RenderMode.ADDITIVE ? RenderType.eyes(Ids.parse(id)) : RenderType.text(Ids.parse(id));
        *///?}
    }

    private static RenderType lightning() {
        //? if >=1.21.11 {
        return RenderTypes.lightning();
        //?} else
        /*return RenderType.lightning();*/
    }
}
