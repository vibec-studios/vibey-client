package dev.henny.r3.mc.fabric;

import dev.henny.r3.mc.R3Hooks;
import dev.henny.r3.mc.R3Mod;
import dev.henny.r3.mc.demo.DemoCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
//? if <1.21.9 {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
*///?}

public final class R3Fabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        String version = FabricLoader.getInstance().getModContainer(R3Mod.ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
        R3Mod.init(version);
        ClientTickEvents.START_CLIENT_TICK.register(mc -> R3Hooks.startTick());
        ClientTickEvents.END_CLIENT_TICK.register(mc -> R3Hooks.endTick());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
                dispatcher.register(DemoCommand.<FabricClientCommandSource>tree()));
        FabricHud.register();
        //? if <1.21.9 {
        /*WorldRenderEvents.AFTER_ENTITIES.register(ctx -> R3Hooks.drawWorld(ctx.matrixStack(), ctx.consumers()));
        *///?}
    }
}
