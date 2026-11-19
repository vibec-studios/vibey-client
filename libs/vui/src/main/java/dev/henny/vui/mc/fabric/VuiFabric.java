package dev.henny.vui.mc.fabric;

import dev.henny.vui.mc.VuiMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class VuiFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        String version = FabricLoader.getInstance().getModContainer(VuiMod.ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
        VuiMod.init(version);
    }
}
