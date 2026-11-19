package dev.henny.cosmetico.mc.neoforge;

import dev.henny.cosmetico.mc.CosmeticoMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = CosmeticoMod.ID, dist = Dist.CLIENT)
public final class CosmeticoNeoForge {
    public CosmeticoNeoForge(ModContainer container) {
        CosmeticoMod.init(container.getModInfo().getVersion().toString());
    }
}
