package dev.henny.vui.mc.neoforge;

import dev.henny.vui.mc.VuiMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = VuiMod.ID, dist = Dist.CLIENT)
public final class VuiNeoForge {
    public VuiNeoForge(ModContainer container) {
        VuiMod.init(container.getModInfo().getVersion().toString());
    }
}
