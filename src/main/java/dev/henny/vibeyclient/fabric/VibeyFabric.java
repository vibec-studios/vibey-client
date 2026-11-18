package dev.henny.vibeyclient.fabric;

import dev.henny.vibeyclient.Platform;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.net.EmotePayload;
import dev.henny.vibeyclient.net.HelloPayload;
import dev.henny.vibeyclient.net.LoadoutPayload;
import dev.henny.vibeyclient.net.MessagePayload;
import dev.henny.vibeyclient.net.NetDispatch;
import dev.henny.vibeyclient.net.ServerRelay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Fabric common entry point (runs on clients and dedicated servers): payloads and the server relay. */
public final class VibeyFabric implements ModInitializer, Platform {
    private List<ModInfo> mods;

    @Override
    public void onInitialize() {
        Vibey.initCommon(this);
        register(HelloPayload.TYPE, HelloPayload.CODEC);
        register(LoadoutPayload.TYPE, LoadoutPayload.CODEC);
        register(EmotePayload.TYPE, EmotePayload.CODEC);
        register(MessagePayload.TYPE, MessagePayload.CODEC);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ServerRelay.onLeave(handler.getPlayer()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerRelay.onServerStopped());
    }

    private static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, StreamCodec<FriendlyByteBuf, T> codec) {
        //? if >=26.1 {
        /*PayloadTypeRegistry.serverboundPlay().register(type, codec);
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
        *///?} else {
        PayloadTypeRegistry.playC2S().register(type, codec);
        PayloadTypeRegistry.playS2C().register(type, codec);
        //?}
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> NetDispatch.toServer(context.player(), payload));
    }

    @Override
    public String loader() {
        return "Fabric";
    }

    @Override
    public boolean isFabric() {
        return true;
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Path gameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public List<ModInfo> mods() {
        if (mods == null) {
            List<ModInfo> list = new ArrayList<>();
            for (ModContainer c : FabricLoader.getInstance().getAllMods()) {
                list.add(new ModInfo(c.getMetadata().getId(), c.getMetadata().getName(), c.getMetadata().getVersion().getFriendlyString()));
            }
            mods = List.copyOf(list);
        }
        return mods;
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
    }
}
