package dev.henny.vui.mc.fabric;

import dev.henny.vui.mc.VuiHooks;
//? if >=1.21.6 {
import dev.henny.vui.mc.VuiMod;
import dev.henny.vui.mc.compat.Ids;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?} else
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;*/

final class FabricHud {
    private FabricHud() {
    }

    static void register() {
        //? if >=1.21.6 {
        HudElementRegistry.addLast(Ids.of(VuiMod.ID, "hud"), (graphics, tick) -> VuiHooks.hud(graphics));
        //?} else
        /*HudRenderCallback.EVENT.register((graphics, tick) -> VuiHooks.hud(graphics));*/
    }
}
