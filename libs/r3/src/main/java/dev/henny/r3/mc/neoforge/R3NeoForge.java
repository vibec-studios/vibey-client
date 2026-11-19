package dev.henny.r3.mc.neoforge;

import dev.henny.r3.mc.R3Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = R3Mod.ID, dist = Dist.CLIENT)
public final class R3NeoForge {
    public R3NeoForge(ModContainer container) {
        R3Mod.init(container.getModInfo().getVersion().toString());
    }
}
