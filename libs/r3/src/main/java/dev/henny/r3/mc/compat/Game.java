package dev.henny.r3.mc.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;

public final class Game {
    private Game() {}

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public static ClientLevel level() {
        return mc().level;
    }

    public static Screen screen() {
        //? if >=26.2 {
        /*return mc().gui.screen();
        *///?} else
        return mc().screen;
    }

    public static boolean overlayOpen() {
        //? if >=26.2 {
        /*return mc().gui.overlay() != null;
        *///?} else
        return mc().getOverlay() != null;
    }

    public static boolean hudHidden() {
        //? if >=26.2 {
        /*return mc().gui.hud.isHidden();
        *///?} else
        return mc().options.hideGui;
    }

    public static boolean playing() {
        Minecraft mc = mc();
        return mc.level != null && mc.player != null && screen() == null && !overlayOpen();
    }

    public static float partialTick() {
        //? if >=1.21.5 {
        return mc().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        //?} else
        /*return mc().getTimer().getGameTimeDeltaPartialTick(false);*/
    }

    public static String dimension(ClientLevel level) {
        //? if >=1.21.11 {
        return level.dimension().identifier().toString();
        //?} else
        /*return level.dimension().location().toString();*/
    }

    public static String singleplayerFolder() {
        IntegratedServer server = mc().getSingleplayerServer();
        if (server == null) return null;
        java.nio.file.Path name = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
        return name == null ? server.getWorldData().getLevelName() : name.toString();
    }

    public static String serverAddress() {
        ServerData data = mc().getCurrentServer();
        return data == null ? null : data.ip;
    }

    public static double fovDegrees() {
        return mc().options.fov().get();
    }

    public static double aspect() {
        int h = Math.max(1, mc().getWindow().getHeight());
        return mc().getWindow().getWidth() / (double) h;
    }
}
