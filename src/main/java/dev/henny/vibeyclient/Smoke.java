package dev.henny.vibeyclient;

import net.minecraft.server.MinecraftServer;

/**
 * Dev-only startup check ({@code -Dvibeyclient.smoke=true}, see the run configs): logs a marker once the game or
 * server is up and stops it again. Used to prove each loader/version boots, and that a dedicated server never loads
 * client classes. Does nothing without the system property.
 */
public final class Smoke {
    private Smoke() {}

    private static final String[] CLIENT_CLASSES = {
            "net.minecraft.client.Minecraft",
            "dev.henny.vibeyclient.client.VibeyCore",
            "dev.henny.vibeyclient.api.ModuleManager",
            "dev.henny.vibeyclient.ui.Gfx",
    };

    public static boolean enabled() {
        return Boolean.getBoolean("vibeyclient.smoke");
    }

    /** Dedicated/integrated server started. */
    public static void serverStarted(MinecraftServer server) {
        if (!enabled() || !server.isDedicatedServer()) return;
        int loaded = 0;
        for (String c : CLIENT_CLASSES) {
            Boolean l = Vibey.platform().isClassLoaded(c);
            if (l == null) continue;
            if (l) {
                loaded++;
                Vibey.LOG.error("[vibeyclient-smoke] client class loaded on the dedicated server: {}", c);
            }
        }
        Vibey.LOG.info("[vibeyclient-smoke] dedicated server up on {}, client classes loaded: {}", Vibey.platform().loader(), loaded);
        server.halt(false);
    }
}
