package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.net.VibeyNet;
import dev.henny.vibeyclient.client.VibeyCore;
import dev.henny.vibeyclient.net.ServerRelay;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.PauseScreen;
import dev.henny.vibeyclient.client.Keys;

/**
 * Runs the real game: checks the module manager, opens the Vibey menu on the title screen and in a world (via the
 * keybind), checks that the integrated server answered our hello on the vibeyclient channel, and takes screenshots.
 * Screenshots land in versions/fabric-1.21.11/build/run/clientGameTest/screenshots (or run/...).
 */
public final class VibeyClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(mc -> {
            ModuleManager mm = ModuleManager.get();
            if (mm.all().size() != 9) throw new AssertionError("Expected 9 modules on Fabric 1.21.11 (with pvp and hud), got " + mm.all().size());
            if (!mm.isEnabled("nametags")) throw new AssertionError("nametags should be enabled by default");
            for (VibeyModule m : mm.all()) {
                System.out.println("[vibeyclient-test] module " + m.id() + ": " + m.status() + (m.statusReason() == null ? "" : " (" + m.statusReason() + ")"));
            }
            if (!mm.isEnabled("cosmetics")) throw new AssertionError("cosmetics should be enabled by default");
            // runtime toggle round trip
            mm.setEnabled("emotes", false);
            if (mm.isEnabled("emotes")) throw new AssertionError("emotes did not disable");
            mm.setEnabled("emotes", true);
            if (!mm.isEnabled("emotes")) throw new AssertionError("emotes did not enable");
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
        });

        ModulesPanel.preselect(null);
        context.setScreen(() -> new VibeyScreen(null));
        context.waitTicks(10);
        context.takeScreenshot("1_menu_title_global");

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            context.waitFor(mc -> VibeyNet.available(), 200);
            int peers = world.getServer().computeOnServer(server -> ServerRelay.peerCount());
            if (peers != 1) throw new AssertionError("Relay should know 1 player that said hello, has " + peers);
            System.out.println("[vibeyclient-test] server channel ok, protocol " + VibeyNet.serverProtocol() + ", relay peers " + peers);

            context.setScreen(() -> new PauseScreen(true));
            context.waitTicks(5);
            context.takeScreenshot("2_pause_menu");
            context.setScreen(() -> null);
            context.waitTicks(2);

            // Right Shift opens the menu
            context.getInput().pressKey(Keys.RIGHT_SHIFT);
            context.waitTicks(10);
            boolean open = context.computeOnClient(mc -> mc.screen instanceof VibeyScreen);
            if (!open) throw new AssertionError("Right Shift did not open the Vibey menu");
            context.takeScreenshot("3_menu_world_global");

            context.setScreen(() -> null);
            ModulesPanel.preselect("emotes");
            context.setScreen(() -> new VibeyScreen(null));
            context.waitTicks(10);
            context.takeScreenshot("4_menu_world_emotes");

            context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.GERMAN));
            ModulesPanel.preselect("devlink");
            context.setScreen(() -> new VibeyScreen(null));
            context.waitTicks(10);
            context.takeScreenshot("5_menu_world_devlink_de");
            context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
            context.setScreen(() -> null);
        }
        System.out.println("[vibeyclient-test] done, core " + (VibeyCore.clientPlatform() != null ? "ok" : "missing"));
    }
}
