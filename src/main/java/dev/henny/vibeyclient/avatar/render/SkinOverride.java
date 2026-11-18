package dev.henny.vibeyclient.avatar.render;

import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.AvatarStates;
import net.minecraft.client.player.AbstractClientPlayer;
//? if >=1.21.9 {
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
//?} else
/*import net.minecraft.client.resources.PlayerSkin;*/

/**
 * Puts the equipped cape's texture into the player's skin (cape + elytra) so the vanilla cape model with its physics
 * and the elytra draw it. The result is cached per player and only rebuilt when the original skin or the cape changes.
 */
public final class SkinOverride {
    private SkinOverride() {}

    /** Original skin → patched skin per avatar (identity compare, no allocation when unchanged). */
    private static final java.util.Map<AvatarState, Object[]> CACHE = new java.util.WeakHashMap<>();

    public static PlayerSkin apply(AbstractClientPlayer player, PlayerSkin original) {
        if (original == null || !dev.henny.vibeyclient.avatar.EngineCompat.onRenderThread()) return null;
        return patch(AvatarStates.peek(player.getUUID()), original);
    }

    /** {@code original} with the cape of {@code st}, or null if nothing changes. Render thread. */
    public static PlayerSkin patch(AvatarState st, PlayerSkin original) {
        if (st == null || original == null) return null;
        Tex cape = AvatarHooks.capeTexture(st);
        if (cape == null) return null;
        Object[] c = CACHE.get(st);
        if (c != null && c[0] == original && c[1] == cape) return (PlayerSkin) c[2];
        //? if >=1.21.9 {
        ClientAsset.ResourceTexture t = new ClientAsset.ResourceTexture(cape.id, cape.id);
        PlayerSkin out = new PlayerSkin(original.body(), t, null, original.model(), original.secure());
        //?} else
        /*PlayerSkin out = new PlayerSkin(original.texture(), original.textureUrl(), cape.id, null, original.model(), original.secure());*/
        CACHE.put(st, new Object[]{original, cape, out});
        return out;
    }
}
