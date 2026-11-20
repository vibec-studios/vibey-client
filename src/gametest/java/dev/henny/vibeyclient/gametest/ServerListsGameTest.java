package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.module.servers.ServersModule;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;

import static dev.henny.vibeyclient.gametest.ServerDragChecks.FOUR;
import static dev.henny.vibeyclient.gametest.ServerDragChecks.ONE;
import static dev.henny.vibeyclient.gametest.ServerDragChecks.THREE;
import static dev.henny.vibeyclient.gametest.ServerDragChecks.TWO;
import static dev.henny.vibeyclient.gametest.ServerListsKit.check;

public final class ServerListsGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            ServersModule m = ServersGameTest.m();
            check(m != null && m.isEnabled(), "servers module should be on");
            m.settings().all().forEach(s -> s.reset());
        });
        String[] names = {ONE, TWO, THREE, FOUR};
        try (FakeStatusServer srv = FakeStatusServer.start(ServersGameTest.statusJson())) {
            String address = "127.0.0.1:" + srv.port();
            ServersTestKit.removeServers(ctx, names);
            ServersTestKit.addServers(ctx, names, new String[] {address, address, address, address});
            ctx.setScreen(() -> new JoinMultiplayerScreen(new TitleScreen()));
            ctx.waitFor(mc -> ServersTestKit.data(ONE) != null && ServersTestKit.data(ONE).state() == ServerData.State.SUCCESSFUL, 300);
            ctx.waitTicks(10);
            new ServerDragChecks(ctx).run();
            ctx.setScreen(TitleScreen::new);
            ServersTestKit.removeServers(ctx, names);
            new FriendsTabChecks(ctx, address).run();
            ServersTestKit.removeServers(ctx, address);
            new WorldOrderChecks(ctx).run();
        }
        ctx.runOnClient(mc -> {
            ServersGameTest.m().settings().all().forEach(s -> s.reset());
            GlobalSettings.language.set(GlobalSettings.Language.AUTO);
        });
    }
}
