package dev.henny.vibeyclient.net;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * Where the loaders hand received payloads. Server side goes to {@link ServerRelay}; the client handler is set by
 * the client core at startup, so this class never references client classes.
 */
public final class NetDispatch {
    private NetDispatch() {}

    private static volatile Consumer<CustomPacketPayload> client = p -> {};

    public static void setClientHandler(Consumer<CustomPacketPayload> handler) {
        client = handler;
    }

    /** Called on the server thread. */
    public static void toServer(ServerPlayer from, CustomPacketPayload payload) {
        ServerRelay.handle(from, payload);
    }

    /** Called on the client (render) thread. */
    public static void toClient(CustomPacketPayload payload) {
        client.accept(payload);
    }
}
