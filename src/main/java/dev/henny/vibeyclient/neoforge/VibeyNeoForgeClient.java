package dev.henny.vibeyclient.neoforge;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.ClientPlatform;
import dev.henny.vibeyclient.client.Keybinds;
import dev.henny.vibeyclient.client.VibeyCore;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
//? if >=1.21.9 {
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//?} else
/*import net.neoforged.neoforge.network.PacketDistributor;*/

import java.util.ArrayList;

/** NeoForge client entry point: starts the client core and forwards NeoForge events to it. */
@Mod(value = Vibey.MOD_ID, dist = Dist.CLIENT)
public final class VibeyNeoForgeClient implements ClientPlatform {
    public VibeyNeoForgeClient(IEventBus modBus) {
        Vibey.initCommon(NeoForgePlatform.INSTANCE);
        VibeyCore.init(this);
        modBus.addListener(this::registerKeys);
        modBus.addListener(this::registerHud);

        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener((ClientTickEvent.Post e) -> VibeyCore.onTick());
        bus.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> VibeyCore.onWorldJoin());
        bus.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> VibeyCore.onWorldLeave());
        bus.addListener((ScreenEvent.Init.Post e) -> VibeyCore.onScreenInit(e.getScreen(),
                new ArrayList<>(e.getListenersList()), e::addListener, e::removeListener));
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        //? if >=1.21.9
        event.registerCategory(ClientCompat.category());
        for (KeyMapping m : Keybinds.freeze()) event.register(m);
    }

    private void registerHud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Compat.id("hud"), (g, tick) -> VibeyCore.onHud(new Gfx(g, Minecraft.getInstance().font)));
    }

    @Override
    public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(type);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        //? if >=1.21.9 {
        ClientPacketDistributor.sendToServer(payload);
        //?} else
        /*PacketDistributor.sendToServer(payload);*/
    }
}
