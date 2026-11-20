package dev.henny.vui.mc.neoforge;

import dev.henny.vui.mc.VuiHooks;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.mc.VuiMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = VuiMod.ID, dist = Dist.CLIENT)
public final class VuiNeoForge {
    public VuiNeoForge(IEventBus modBus, ModContainer container) {
        VuiHooks.init(container.getModInfo().getVersion().toString());
        modBus.addListener(NeoHud::register);
        NeoScreens.register();
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> VuiHooks.clientTick());
        if (VuiMc.dev().enabled()) NeoForge.EVENT_BUS.addListener(NeoCommands::register);
    }
}
