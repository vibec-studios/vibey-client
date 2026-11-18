package dev.henny.vibeyclient.neoforge;

import dev.henny.vibeyclient.Platform;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.net.EmotePayload;
import dev.henny.vibeyclient.net.HelloPayload;
import dev.henny.vibeyclient.net.LoadoutPayload;
import dev.henny.vibeyclient.net.MessagePayload;
import dev.henny.vibeyclient.net.NetDispatch;
import dev.henny.vibeyclient.net.ServerRelay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** NeoForge common entry point (clients and dedicated servers): payloads and the server relay. */
@Mod(Vibey.MOD_ID)
public final class VibeyNeoForge {
    public VibeyNeoForge(IEventBus modBus) {
        Vibey.initCommon(NeoForgePlatform.INSTANCE);
        modBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) ServerRelay.onLeave(p);
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> ServerRelay.onServerStopped());
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        // optional(): vanilla servers/clients without the mod can still connect.
        PayloadRegistrar r = event.registrar("1").optional();
        register(r, HelloPayload.TYPE, HelloPayload.CODEC);
        register(r, LoadoutPayload.TYPE, LoadoutPayload.CODEC);
        register(r, EmotePayload.TYPE, EmotePayload.CODEC);
        register(r, MessagePayload.TYPE, MessagePayload.CODEC);
    }

    private static <T extends CustomPacketPayload> void register(PayloadRegistrar r, CustomPacketPayload.Type<T> type, StreamCodec<FriendlyByteBuf, T> codec) {
        IPayloadHandler<T> handler = (payload, context) -> {
            if (context.player() instanceof ServerPlayer sp) context.enqueueWork(() -> NetDispatch.toServer(sp, payload));
            else context.enqueueWork(() -> NetDispatch.toClient(payload));
        };
        //? if >=1.21.9 {
        r.playBidirectional(type, codec, handler, handler);
        //?} else
        /*r.playBidirectional(type, codec, handler);*/
    }
}
