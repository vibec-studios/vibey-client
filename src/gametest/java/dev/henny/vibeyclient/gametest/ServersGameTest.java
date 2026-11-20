package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.servers.ServersModule;
import dev.henny.vibeyclient.module.servers.screen.ServerSession;
import dev.henny.vibeyclient.module.servers.screen.ServerSessions;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Locale;

import static dev.henny.vibeyclient.gametest.ServersTestKit.SHOTS;
import static dev.henny.vibeyclient.gametest.ServersTestKit.check;
import static dev.henny.vibeyclient.gametest.ServersTestKit.data;
import static dev.henny.vibeyclient.gametest.ServersTestKit.log;
import static dev.henny.vibeyclient.gametest.ServersTestKit.online;

public final class ServersGameTest implements FabricClientGameTest {
    static final String TEST = "Vibey Test", DEAD = "Alpha Offline";

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            ServersModule m = m();
            check(m != null && m.isEnabled(), "servers module should be on");
            m.settings().all().forEach(s -> s.reset());
            m.pingEvery.set(10);
        });
        page(ctx, GlobalSettings.Language.ENGLISH, "page_en");
        page(ctx, GlobalSettings.Language.GERMAN, "page_de");
        try (FakeStatusServer srv = FakeStatusServer.start(json())) {
            String address = "127.0.0.1:" + srv.port();
            ServersTestKit.addServers(ctx, new String[] {DEAD, TEST}, new String[] {"127.0.0.1:1", address});
            open(ctx);
            ctx.waitFor(mc -> data(TEST) != null && data(TEST).state() == ServerData.State.SUCCESSFUL, 300);
            ctx.waitFor(mc -> data(DEAD) != null && data(DEAD).state() == ServerData.State.UNREACHABLE, 300);
            check(online(ctx).get(0).equals(DEAD), "list starts in saved order");
            SHOTS.shot(ctx, "list");

            ServersTestKit.clickStar(ctx, TEST);
            check(m().favs().contains(address), "star click should favourite the server");
            check(online(ctx).get(0).equals(TEST), "favourites go first: " + online(ctx));
            SHOTS.shot(ctx, "favourite");

            search(ctx);
            preview(ctx);
            livePing(ctx, srv);
            cache(ctx, srv);

            ctx.setScreen(() -> new TitleScreen());
            ctx.runOnClient(mc -> m().toggleFavourite(address));
            ServersTestKit.removeServers(ctx, TEST, DEAD);
        }
        ctx.runOnClient(mc -> {
            m().settings().all().forEach(s -> s.reset());
            GlobalSettings.language.set(GlobalSettings.Language.AUTO);
        });
    }

    static ServersModule m() {
        return ModuleManager.get().get(ServersModule.class);
    }

    private static void open(ClientGameTestContext ctx) {
        ctx.setScreen(() -> new JoinMultiplayerScreen(new TitleScreen()));
        ctx.waitTicks(5);
    }

    private static void search(ClientGameTestContext ctx) {
        ctx.getInput().holdControl();
        ctx.getInput().pressKey(Keys.toNative(Keys.F));
        ctx.getInput().releaseControl();
        ctx.waitTicks(2);
        ctx.getInput().typeChars("alpha");
        ctx.waitTicks(5);
        check(online(ctx).size() == 1 && online(ctx).get(0).equals(DEAD), "search should leave only Alpha: " + online(ctx));
        check(ctx.computeOnClient(mc -> mc.screen instanceof JoinMultiplayerScreen), "typing must not leave the screen");
        SHOTS.shot(ctx, "search");
        ctx.getInput().pressKey(Keys.toNative(Keys.ESCAPE));
        ctx.runOnClient(mc -> session().bar().clear());
        ctx.waitTicks(3);
        check(online(ctx).size() == 2, "clearing the search shows all again");
    }

    private static void preview(ClientGameTestContext ctx) {
        ServersTestKit.hoverRow(ctx, TEST);
        ctx.waitFor(mc -> session().preview().open.peek(), 60);
        ctx.waitTicks(3);
        String lines = ctx.computeOnClient(mc -> session().preview().lines.snapshot().toString());
        log("preview lines %s", lines);
        check(lines.contains("Season 3"), "MOTD preview should show the second line");
        check(ctx.computeOnClient(mc -> session().preview().players.peek()).equals("3/20"), "player count in preview");
        SHOTS.shot(ctx, "motd_preview");
        ctx.getInput().setCursorPos(4, 4);
        ctx.waitTicks(3);
    }

    private static void livePing(ClientGameTestContext ctx, FakeStatusServer srv) {
        int before = srv.pings();
        long start = System.nanoTime();
        ctx.waitFor(mc -> srv.pings() > before, 20 * 25);
        log("live re-ping after %.1f s (setting %d s)", (System.nanoTime() - start) / 1e9, m().pingEvery.get());
        check(ctx.computeOnClient(mc -> session().pinger().started()) >= 1, "live pinger should have started a ping");
        ctx.waitFor(mc -> data(TEST).state() == ServerData.State.SUCCESSFUL, 100);
    }

    private static void cache(ClientGameTestContext ctx, FakeStatusServer srv) {
        ctx.setScreen(() -> new TitleScreen());
        int before = srv.pings();
        open(ctx);
        ctx.waitTicks(20);
        check(ctx.computeOnClient(mc -> data(TEST).state()) == ServerData.State.SUCCESSFUL, "cached ping restored");
        check(srv.pings() == before, "reopening within the interval must not ping again");
    }

    private static ServerSession session() {
        return ServerSessions.current();
    }

    private static String json() {
        int protocol = SharedConstants.getCurrentVersion().protocolVersion();
        return String.format(Locale.ROOT, "{\"version\":{\"name\":\"%s\",\"protocol\":%d},"
                + "\"players\":{\"max\":20,\"online\":3,\"sample\":[{\"name\":\"Alex\",\"id\":\"00000000-0000-0000-0000-000000000001\"},"
                + "{\"name\":\"Kai\",\"id\":\"00000000-0000-0000-0000-000000000002\"}]},"
                + "\"description\":{\"text\":\"\\u00a7bWelcome to \\u00a7lVibey\\n\",\"extra\":[{\"text\":\"Season 3 is live!\",\"color\":\"gold\"}]}}",
                SharedConstants.getCurrentVersion().name(), protocol);
    }

    private static void page(ClientGameTestContext ctx, GlobalSettings.Language lang, String name) {
        ctx.runOnClient(mc -> GlobalSettings.language.set(lang));
        ModulesPanel.preselect("servers");
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(10);
        SHOTS.shot(ctx, name);
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
        ctx.waitTicks(5);
    }
}
