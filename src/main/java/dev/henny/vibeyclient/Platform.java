package dev.henny.vibeyclient;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.List;

/** The few common (server-safe) things that differ between Fabric and NeoForge. */
public interface Platform {
    /** "Fabric" or "NeoForge". */
    String loader();

    boolean isFabric();

    Path configDir();

    Path gameDir();

    boolean isModLoaded(String modId);

    /** Running from a dev workspace (runClient etc.). Dev-only code also needs a system property, see SPEC §1. */
    boolean isDevelopment();

    /** Physical client (false on a dedicated server). */
    boolean isClient();

    List<ModInfo> mods();

    /** Sends a payload to one player. Only call it for players that registered our channel (they sent hello). */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /** Whether the class has been loaded already, or null if the loader cannot tell (dev smoke checks). */
    default Boolean isClassLoaded(String className) {
        return null;
    }

    record ModInfo(String id, String name, String version) {}
}
