package dev.henny.r3.mc.fabric;

import dev.henny.r3.mc.R3Mod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class R3Fabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        String version = FabricLoader.getInstance().getModContainer(R3Mod.ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
        R3Mod.init(version);
    }
}
