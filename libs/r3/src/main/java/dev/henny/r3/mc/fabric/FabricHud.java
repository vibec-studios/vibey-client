package dev.henny.r3.mc.fabric;

import dev.henny.r3.mc.compat.Ids;
import dev.henny.r3.mc.hud.HintOverlay;
//? if >=1.21.6 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?} else
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;*/

final class FabricHud {
    private FabricHud() {}

    static void register() {
        //? if >=1.21.6 {
        HudElementRegistry.addLast(Ids.of("r3", "hint"), (g, tick) -> HintOverlay.draw(g));
        //?} else
        /*HudRenderCallback.EVENT.register((g, tick) -> HintOverlay.draw(g));*/
    }
}
