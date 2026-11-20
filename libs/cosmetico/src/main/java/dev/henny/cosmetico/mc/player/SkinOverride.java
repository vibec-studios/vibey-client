package dev.henny.cosmetico.mc.player;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.mc.compat.McSelf;
import dev.henny.cosmetico.mc.render.Tex;
import dev.henny.cosmetico.state.Avatar;
import net.minecraft.client.player.AbstractClientPlayer;
//? if >=1.21.9 {
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
//?} else
/*import net.minecraft.client.resources.PlayerSkin;*/

import java.util.Map;
import java.util.WeakHashMap;

public final class SkinOverride {
    private SkinOverride() {}

    private static final Map<Avatar, Object[]> CACHE = new WeakHashMap<>();

    public static PlayerSkin apply(AbstractClientPlayer player, PlayerSkin original) {
        if (original == null || !Cosmetico.active() || !McSelf.onRenderThread()) return null;
        return patch(Cosmetico.engine().avatars.peek(player.getUUID()), original);
    }

    public static PlayerSkin patch(Avatar a, PlayerSkin original) {
        if (a == null || original == null) return null;
        Tex cape = CapeShieldTextures.cape(a);
        if (cape == null) return null;
        Object[] c = CACHE.get(a);
        if (c != null && c[0] == original && c[1] == cape) return (PlayerSkin) c[2];
        //? if >=1.21.9 {
        ClientAsset.ResourceTexture t = new ClientAsset.ResourceTexture(cape.id, cape.id);
        PlayerSkin out = new PlayerSkin(original.body(), t, null, original.model(), original.secure());
        //?} else
        /*PlayerSkin out = new PlayerSkin(original.texture(), original.textureUrl(), cape.id, null, original.model(), original.secure());*/
        CACHE.put(a, new Object[]{original, cape, out});
        return out;
    }
}
