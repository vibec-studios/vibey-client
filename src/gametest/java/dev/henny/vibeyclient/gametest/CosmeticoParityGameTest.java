package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.gametest.parity.OldEngineParity;
import dev.henny.vibeyclient.gametest.parity.ParityClock;
import dev.henny.vibeyclient.gametest.parity.ParityEngine;
import dev.henny.vibeyclient.gametest.parity.ParityShots;
import dev.henny.vibeyclient.gametest.parity.ParityTitle;
import dev.henny.vibeyclient.gametest.parity.ParityWorld;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class CosmeticoParityGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        ParityEngine engine = new OldEngineParity();
        String folder = System.getenv().getOrDefault("COSMETICO_PARITY_DIR", engine.name().equals("old") ? "baseline" : "after");
        ParityShots shots = new ParityShots(context, folder);
        context.getInput().resizeWindow(1280, 720);
        context.waitFor(mc -> engine.itemCount() >= 20, 600);
        try {
            new ParityTitle(context, engine, shots).run();
            new ParityWorld(context, engine, shots).run();
        } finally {
            ParityClock.stop();
        }
    }
}
