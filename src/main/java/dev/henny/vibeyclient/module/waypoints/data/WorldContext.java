package dev.henny.vibeyclient.module.waypoints.data;

import dev.henny.vibeyclient.module.waypoints.core.Pos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.Locale;

public final class WorldContext {
    private WorldContext() {}

    public static String worldKey(Minecraft mc) {
        if (mc.getSingleplayerServer() != null) {
            Path root = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            return "sp:" + root.getFileName();
        }
        ServerData server = mc.getCurrentServer();
        return server == null ? "mp:unknown" : "mp:" + server.ip.toLowerCase(Locale.ROOT);
    }

    public static String dimension(Level level) {
        return level.dimension().identifier().toString();
    }

    public static String dimension() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? null : dimension(mc.level);
    }

    public static Pos feet(Player p) {
        return Pos.clamped((long) Math.floor(p.getX()), (long) Math.floor(p.getY()), (long) Math.floor(p.getZ()));
    }

    public static String safe(String key) {
        String s = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        if (s.length() > 48) s = s.substring(0, 48);
        return s + "-" + Integer.toHexString(key.hashCode());
    }
}
