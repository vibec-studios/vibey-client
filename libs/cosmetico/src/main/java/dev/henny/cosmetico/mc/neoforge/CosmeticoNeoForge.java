package dev.henny.cosmetico.mc.neoforge;

import dev.henny.cosmetico.mc.CosmeticoMod;
import dev.henny.cosmetico.mc.tick.CosmeticoTick;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = CosmeticoMod.ID, dist = Dist.CLIENT)
public final class CosmeticoNeoForge {
    public CosmeticoNeoForge(ModContainer container) {
        CosmeticoMod.init(container.getModInfo().getVersion().toString());
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> CosmeticoTick.run());
    }
}
