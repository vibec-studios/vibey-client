package dev.henny.vibeyclient.module.cinematic.afk;

import dev.henny.vibeyclient.api.Lang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

final class AfkInfo {
    private AfkInfo() {}

    static String place() {
        Minecraft mc = Minecraft.getInstance();
        ServerData s = mc.getCurrentServer();
        if (s != null && s.name != null && !s.name.isBlank()) return s.name;
        if (mc.getSingleplayerServer() != null) return mc.getSingleplayerServer().getWorldData().getLevelName();
        return Lang.tr("cinematic.afk.title");
    }
}
