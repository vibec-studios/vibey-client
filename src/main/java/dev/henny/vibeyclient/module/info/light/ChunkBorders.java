package dev.henny.vibeyclient.module.info.light;

import dev.henny.vibeyclient.module.info.InfoModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.network.chat.Component;

public final class ChunkBorders {
    private static boolean ours;

    private ChunkBorders() {}

    public static void toggle(InfoModule m) {
        Minecraft mc = Minecraft.getInstance();
        if (!m.chunksActive() || mc.player == null || mc.player.isReducedDebugInfo()) return;
        boolean on = mc.debugEntries.toggleStatus(DebugScreenEntries.CHUNK_BORDERS);
        ours = on;
        mc.gui.setOverlayMessage(Component.translatable(on ? "debug.chunk_boundaries.on" : "debug.chunk_boundaries.off"), false);
    }

    public static boolean shown() {
        return Minecraft.getInstance().debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_BORDERS);
    }

    public static void offIfOurs() {
        if (!ours) return;
        ours = false;
        Minecraft mc = Minecraft.getInstance();
        if (shown()) mc.debugEntries.toggleStatus(DebugScreenEntries.CHUNK_BORDERS);
    }
}
