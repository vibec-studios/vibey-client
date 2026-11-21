package dev.henny.vibeyclient.module.skins;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.UUID;

public final class LocalSkin {
    private record Swap(UUID owner, Identifier texture, PlayerModelType model) {}

    private record Patched(PlayerSkin original, Swap by, PlayerSkin result) {}

    private static volatile Swap current;
    private static volatile Patched last;

    private LocalSkin() {}

    public static boolean set(UUID owner, byte[] png, boolean slim) {
        if (owner == null) return false;
        Identifier id = SkinImages.upload("local", png);
        if (id == null) return false;
        Swap old = current;
        current = new Swap(owner, id, slim ? PlayerModelType.SLIM : PlayerModelType.WIDE);
        last = null;
        if (old != null) SkinImages.release(old.texture());
        return true;
    }

    public static void clear() {
        Swap old = current;
        current = null;
        last = null;
        if (old != null) SkinImages.release(old.texture());
    }

    public static boolean active() {
        return current != null;
    }

    public static PlayerSkin apply(PlayerInfo info, PlayerSkin original) {
        Swap o = current;
        if (o == null || original == null) return null;
        GameProfile p = info.getProfile();
        if (p == null || !o.owner().equals(p.id())) return null;
        Patched l = last;
        if (l != null && l.original() == original && l.by() == o) return l.result();
        ClientAsset.ResourceTexture body = new ClientAsset.ResourceTexture(o.texture(), o.texture());
        PlayerSkin out = new PlayerSkin(body, original.cape(), original.elytra(), o.model(), original.secure());
        last = new Patched(original, o, out);
        return out;
    }
}
