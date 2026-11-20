package dev.henny.cosmetico.mc.fabric;

import dev.henny.cosmetico.mc.CosmeticoMod;
import dev.henny.cosmetico.mc.tick.CosmeticoTick;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class CosmeticoFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        String version = FabricLoader.getInstance().getModContainer(CosmeticoMod.ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
        CosmeticoMod.init(version);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> CosmeticoTick.run());
    }
}
