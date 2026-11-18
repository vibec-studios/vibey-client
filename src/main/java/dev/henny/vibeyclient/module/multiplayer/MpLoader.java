package dev.henny.vibeyclient.module.multiplayer;

import dev.henny.vibeyclient.client.ClientPlatform;
import dev.henny.vibeyclient.client.VibeyCore;
import dev.henny.vibeyclient.module.multiplayer.host.Guests;
import dev.henny.vibeyclient.module.multiplayer.host.HostSession;
import dev.henny.vibeyclient.module.multiplayer.net.ModListPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
//? if fabric {
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.fabricmc.loader.api.metadata.ModMetadata;
//?} else {
/*import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforgespi.language.IModInfo;
*///?}

/**
 * Loader glue of the multiplayer module: the mod-list payload, the hooks into the integrated server (guests joining,
 * leaving, ticks, stopping) and the installed mods. Everything is registered once from {@code onLoad}, i.e. only when
 * the module can run at all (not when the standalone Vibey Multiplayer mod is installed). The handlers themselves
 * only act while we host.
 */
public final class MpLoader {
    /** An installed mod as "Force mods" sees it. */
    public record ModInfo(String id, String name, String version, boolean clientOnly, boolean library) {}

    private static List<ModInfo> mods;
    private static boolean registered;

    private MpLoader() {}

    public static void register() {
        if (registered) return;
        registered = true;
        //? if fabric {
        //? if >=26.1 {
        /*PayloadTypeRegistry.serverboundPlay().register(ModListPayload.TYPE, ModListPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ModListPayload.Legacy.TYPE, ModListPayload.Legacy.CODEC);
        *///?} else {
        PayloadTypeRegistry.playC2S().register(ModListPayload.TYPE, ModListPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModListPayload.Legacy.TYPE, ModListPayload.Legacy.CODEC);
        //?}
        ServerPlayNetworking.registerGlobalReceiver(ModListPayload.TYPE, (payload, context) ->
                Guests.onModList(context.server(), context.player(), payload.mods()));
        ServerPlayNetworking.registerGlobalReceiver(ModListPayload.Legacy.TYPE, (payload, context) ->
                Guests.onModList(context.server(), context.player(), payload.mods()));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Guests.onJoin(server, handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Guests.onLeave(server, handler.getPlayer()));
        ServerTickEvents.END_SERVER_TICK.register(Guests::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(HostSession::onServerStopping);
        //?} else {
        /*IEventBus modBus = ModList.get().getModContainerById("vibeyclient").map(c -> c.getEventBus()).orElse(null);
        if (modBus != null) modBus.addListener(MpLoader::registerPayloads);
        else MultiplayerModule.LOG.warn("No mod event bus, Force mods cannot receive mod lists");
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) Guests.onJoin(((ServerLevel) p.level()).getServer(), p);
        });
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) Guests.onLeave(((ServerLevel) p.level()).getServer(), p);
        });
        bus.addListener((ServerTickEvent.Post e) -> Guests.tick(e.getServer()));
        bus.addListener((ServerStoppingEvent e) -> HostSession.onServerStopping(e.getServer()));
        *///?}
    }

    //? if neoforge {
    /*private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(ModListPayload.TYPE, ModListPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) Guests.onModList(((ServerLevel) p.level()).getServer(), p, payload.mods());
        }));
        r.playToServer(ModListPayload.Legacy.TYPE, ModListPayload.Legacy.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) Guests.onModList(((ServerLevel) p.level()).getServer(), p, payload.mods());
        }));
    }
    *///?}

    /** Client side of "Force mods": tell the host we joined which mods we have (if it asks for them). */
    public static void sendModList() {
        ClientPlatform cp = VibeyCore.clientPlatform();
        if (cp == null) return;
        List<String> list = new ArrayList<>();
        for (ModInfo m : mods()) list.add(m.id() + "@" + m.version());
        CustomPacketPayload payload = null;
        if (cp.canSendToServer(ModListPayload.TYPE)) payload = new ModListPayload(list);
        else if (cp.canSendToServer(ModListPayload.Legacy.TYPE)) payload = new ModListPayload.Legacy(list);
        if (payload != null) cp.sendToServer(payload);
    }

    public static List<ModInfo> mods() {
        if (mods == null) {
            List<ModInfo> list = new ArrayList<>();
            //? if fabric {
            for (ModContainer c : FabricLoader.getInstance().getAllMods()) {
                ModMetadata m = c.getMetadata();
                boolean nested = c.getContainingMod().isPresent();
                boolean builtin = "builtin".equals(m.getType());
                list.add(new ModInfo(m.getId(), m.getName(), m.getVersion().getFriendlyString(),
                        m.getEnvironment() == ModEnvironment.CLIENT, nested || builtin));
            }
            //?} else {
            /*for (IModInfo m : ModList.get().getMods()) {
                list.add(new ModInfo(m.getModId(), m.getDisplayName(), m.getVersion().toString(), false, false));
            }
            *///?}
            mods = List.copyOf(list);
        }
        return mods;
    }

    public static String minecraftVersion() {
        for (ModInfo m : mods()) if (m.id().equals("minecraft")) return m.version();
        return "";
    }
}
