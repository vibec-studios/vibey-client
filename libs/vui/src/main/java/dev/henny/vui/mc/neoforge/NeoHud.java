package dev.henny.vui.mc.neoforge;

import dev.henny.vui.mc.VuiHooks;
import dev.henny.vui.mc.VuiMod;
import dev.henny.vui.mc.compat.Ids;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

final class NeoHud {
    private NeoHud() {
    }

    static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Ids.of(VuiMod.ID, "hud"), (graphics, tick) -> VuiHooks.hud(graphics));
    }
}
