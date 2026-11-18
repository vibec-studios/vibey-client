package dev.henny.vibeyclient.fabric;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.client.ClientPlatform;
import dev.henny.vibeyclient.client.Keybinds;
import dev.henny.vibeyclient.client.VibeyCore;
import dev.henny.vibeyclient.net.EmotePayload;
import dev.henny.vibeyclient.net.HelloPayload;
import dev.henny.vibeyclient.net.LoadoutPayload;
import dev.henny.vibeyclient.net.MessagePayload;
import dev.henny.vibeyclient.net.NetDispatch;
import dev.henny.vibeyclient.ui.Gfx;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
//? if >=1.21.6 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?} else
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;*/
//? if >=26.1 {
/*import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
*///?} else
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

/** Fabric client entry point: starts the client core and forwards Fabric events to it. */
public final class VibeyFabricClient implements ClientModInitializer, ClientPlatform {
    @Override
    public void onInitializeClient() {
        VibeyCore.init(this);

        for (KeyMapping m : Keybinds.freeze()) {
            //? if >=26.1 {
            /*KeyMappingHelper.registerKeyMapping(m);
            *///?} else
            KeyBindingHelper.registerKeyBinding(m);
        }

        ClientPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE, (p, ctx) -> NetDispatch.toClient(p));
        ClientPlayNetworking.registerGlobalReceiver(LoadoutPayload.TYPE, (p, ctx) -> NetDispatch.toClient(p));
        ClientPlayNetworking.registerGlobalReceiver(EmotePayload.TYPE, (p, ctx) -> NetDispatch.toClient(p));
        ClientPlayNetworking.registerGlobalReceiver(MessagePayload.TYPE, (p, ctx) -> NetDispatch.toClient(p));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> VibeyCore.onTick());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> VibeyCore.onWorldJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(VibeyCore::onWorldLeave));

        //? if >=1.21.6 {
        HudElementRegistry.addLast(Compat.id("hud"), (g, tick) -> VibeyCore.onHud(new Gfx(g, Minecraft.getInstance().font)));
        //?} else
        /*HudRenderCallback.EVENT.register((g, tick) -> VibeyCore.onHud(new Gfx(g, Minecraft.getInstance().font)));*/

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            //? if >=26.1 {
            /*var widgets = Screens.getWidgets(screen);
            *///?} else
            var widgets = Screens.getButtons(screen);
            VibeyCore.onScreenInit(screen, new ArrayList<>(widgets), widgets::add, widgets::remove);
        });
    }

    @Override
    public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
        return Minecraft.getInstance().getConnection() != null && ClientPlayNetworking.canSend(type);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
