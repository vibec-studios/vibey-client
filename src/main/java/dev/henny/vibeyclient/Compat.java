package dev.henny.vibeyclient;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else
/*import net.minecraft.resources.ResourceLocation;*/

/**
 * Common (server-safe) calls whose shape changed between Minecraft 1.21.1 and 26.3. Client-only glue lives in
 * {@code dev.henny.vibeyclient.client.ClientCompat}; drawing glue in {@code dev.henny.vibeyclient.ui.Gfx}.
 */
public final class Compat {
    private Compat() {}

    //? if >=1.21.11 {
    public static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Vibey.MOD_ID, path);
    }
    //?} else {
    /*public static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Vibey.MOD_ID, path);
    }
    *///?}

    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(id(path));
    }

    public static MinecraftServer server(ServerPlayer player) {
        return ((ServerLevel) player.level()).getServer();
    }
}
