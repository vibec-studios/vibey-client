package dev.henny.companio.mc.fabric;

import dev.henny.companio.mc.CompanioMod;
import dev.henny.companio.mc.client.CompanioClient;
import dev.henny.companio.mc.command.CompanioCommand;
import dev.henny.companio.mc.screen.Overlay;
import dev.henny.companio.mc.screen.RadialController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CompanioFabric implements ClientModInitializer {
    private int errors;

    @Override
    public void onInitializeClient() {
        CompanioClient client = CompanioClient.start(FabricLoader.getInstance().getGameDir(), CompanioFabric::assetRoots);
        client.overlay(new Overlay(client));
        client.hooks().menu(RadialController::open);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            try {
                client.tick(mc);
            } catch (RuntimeException e) {
                if (errors++ < 3) CompanioMod.LOG.error("Companion tick failed", e);
            }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(CompanioCommand.tree()));
    }

    private static List<Path> assetRoots() {
        List<Path> out = new ArrayList<>();
        for (ModContainer m : FabricLoader.getInstance().getAllMods()) {
            for (Path root : m.getRootPaths()) {
                Path assets = root.resolve("assets");
                if (java.nio.file.Files.isDirectory(assets)) out.add(assets);
            }
        }
        return out;
    }
}
