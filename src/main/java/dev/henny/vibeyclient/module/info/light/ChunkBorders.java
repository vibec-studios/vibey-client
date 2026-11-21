package dev.henny.vibeyclient.module.info.light;

import dev.henny.vibeyclient.client.hud.HudAccess;
import dev.henny.vibeyclient.module.info.InfoModule;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
//? if >=1.21.9 {
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
//?} else
/*import dev.henny.vibeyclient.module.info.mixin.DebugRendererAccessor;*/

public final class ChunkBorders {
    private static boolean ours;

    private ChunkBorders() {}

    public static void toggle(InfoModule m) {
        Minecraft mc = Minecraft.getInstance();
        if (!m.chunksActive() || mc.player == null || mc.player.isReducedDebugInfo()) return;
        boolean on = flip(mc);
        ours = on;
        HudAccess.overlayMessage(Component.translatable(on ? "debug.chunk_boundaries.on" : "debug.chunk_boundaries.off"));
    }

    public static boolean shown() {
        //? if >=1.21.9 {
        return Minecraft.getInstance().debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_BORDERS);
        //?} else
        /*return ((DebugRendererAccessor) Minecraft.getInstance().debugRenderer).vibey$chunkBorders();*/
    }

    public static void offIfOurs() {
        if (!ours) return;
        ours = false;
        Minecraft mc = Minecraft.getInstance();
        if (shown()) flip(mc);
    }

    private static boolean flip(Minecraft mc) {
        //? if >=1.21.9 {
        return mc.debugEntries.toggleStatus(DebugScreenEntries.CHUNK_BORDERS);
        //?} else
        /*return mc.debugRenderer.switchRenderChunkborder();*/
    }
}
