package dev.henny.vibeyclient.neoforge;

import dev.henny.vibeyclient.Platform;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** {@link Platform} on NeoForge (common, server-safe). */
public final class NeoForgePlatform implements Platform {
    public static final NeoForgePlatform INSTANCE = new NeoForgePlatform();

    private List<ModInfo> mods;

    private NeoForgePlatform() {}

    @Override
    public String loader() {
        return "NeoForge";
    }

    @Override
    public boolean isFabric() {
        return false;
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get() != null ? ModList.get().isLoaded(modId)
                : net.neoforged.fml.loading.LoadingModList.get().getModFileById(modId) != null;
    }

    @Override
    public boolean isDevelopment() {
        //? if >=1.21.9 {
        return !FMLLoader.getCurrent().isProduction();
        //?} else
        /*return !FMLLoader.isProduction();*/
    }

    @Override
    public boolean isClient() {
        //? if >=1.21.9 {
        return FMLLoader.getCurrent().getDist().isClient();
        //?} else
        /*return FMLLoader.getDist().isClient();*/
    }

    @Override
    public List<ModInfo> mods() {
        if (mods == null) {
            List<ModInfo> list = new ArrayList<>();
            for (IModInfo m : ModList.get().getMods()) list.add(new ModInfo(m.getModId(), m.getDisplayName(), m.getVersion().toString()));
            mods = List.copyOf(list);
        }
        return mods;
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && player.connection.hasChannel(payload.type())) PacketDistributor.sendToPlayer(player, payload);
    }
}
