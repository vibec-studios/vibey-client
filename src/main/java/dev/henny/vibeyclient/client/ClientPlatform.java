package dev.henny.vibeyclient.client;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client-only things that differ between Fabric and NeoForge. */
public interface ClientPlatform {
    /** The server we are connected to registered this payload type (i.e. has the mod). */
    boolean canSendToServer(CustomPacketPayload.Type<?> type);

    void sendToServer(CustomPacketPayload payload);
}
