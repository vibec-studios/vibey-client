package dev.henny.r3.mc.neoforge;

import dev.henny.r3.mc.R3Hooks;
import dev.henny.r3.mc.R3Mod;
import dev.henny.r3.mc.compat.Ids;
import dev.henny.r3.mc.demo.DemoCommand;
import dev.henny.r3.mc.hud.HintOverlay;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = R3Mod.ID, dist = Dist.CLIENT)
public final class R3NeoForge {
    public R3NeoForge(IEventBus modBus, ModContainer container) {
        R3Mod.init(container.getModInfo().getVersion().toString());
        modBus.addListener(R3NeoForge::registerHud);
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener((ClientTickEvent.Pre e) -> R3Hooks.startTick());
        bus.addListener((ClientTickEvent.Post e) -> R3Hooks.endTick());
        bus.addListener((RegisterClientCommandsEvent e) -> e.getDispatcher().register(DemoCommand.<CommandSourceStack>tree()));
    }

    private static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Ids.of("r3", "hint"), (g, tick) -> HintOverlay.draw(g));
    }
}
