package dev.henny.vibeyclient.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.PlayerTabOverlay;

public final class HudAccess {
    private HudAccess() {}

    public static Object hud() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.hud;
        *///?} else
        return Minecraft.getInstance().gui;
    }

    public static BossHealthOverlay bossOverlay() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.hud.getBossOverlay();
        *///?} else
        return Minecraft.getInstance().gui.getBossOverlay();
    }

    public static void overlayMessage(net.minecraft.network.chat.Component message) {
        //? if >=26.2 {
        /*Minecraft.getInstance().gui.hud.setOverlayMessage(message, false);
        *///?} else
        Minecraft.getInstance().gui.setOverlayMessage(message, false);
    }

    public static PlayerTabOverlay tabList() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.hud.getTabList();
        *///?} else
        return Minecraft.getInstance().gui.getTabList();
    }
}
