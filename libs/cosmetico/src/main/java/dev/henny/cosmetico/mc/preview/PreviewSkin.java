package dev.henny.cosmetico.mc.preview;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.mc.compat.McIds;
import dev.henny.cosmetico.mc.player.SkinOverride;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.state.Avatar;
import net.minecraft.client.Minecraft;
//? if >=1.21.9 {
import net.minecraft.world.entity.player.PlayerSkin;
//?} else
/*import net.minecraft.client.resources.PlayerSkin;*/

import java.util.function.Supplier;

final class PreviewSkin {
    private Supplier<PlayerSkin> lookup;

    PlayerSkin skin(Avatar a) {
        Minecraft mc = Minecraft.getInstance();
        PlayerSkin base = mc.player != null ? mc.player.getSkin() : lookup(mc).get();
        PlayerSkin patched = SkinOverride.patch(a, base);
        if (patched != null) return patched;
        //? if >=1.21.9 {
        if (base.cape() != null && Cosmetico.engine().shown.bundle(a, Slot.CAPE) == null && mc.player != null
                && McIds.isOurs(base.cape().texturePath())) {
            return new PlayerSkin(base.body(), null, null, base.model(), base.secure());
        }
        //?}
        return base;
    }

    private Supplier<PlayerSkin> lookup(Minecraft mc) {
        if (lookup == null) {
            //? if >=1.21.9 {
            lookup = mc.getSkinManager().createLookup(mc.getGameProfile(), false);
            //?} else
            /*lookup = mc.getSkinManager().lookupInsecure(mc.getGameProfile());*/
        }
        return lookup;
    }
}
