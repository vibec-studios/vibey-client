package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.FriendsActivity;
import dev.henny.vibeyclient.module.servers.friends.FriendItem;
import dev.henny.vibeyclient.module.servers.screen.ServerSession;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;

import java.util.List;

import static dev.henny.vibeyclient.gametest.ServerListsKit.SHOTS;
import static dev.henny.vibeyclient.gametest.ServerListsKit.box;
import static dev.henny.vibeyclient.gametest.ServerListsKit.check;
import static dev.henny.vibeyclient.gametest.ServerListsKit.log;
import static dev.henny.vibeyclient.gametest.ServerListsKit.session;

final class FriendsTabChecks {
    private final ClientGameTestContext ctx;
    private final String pingAddress;
    private FakeFriends fake;

    FriendsTabChecks(ClientGameTestContext ctx, String pingAddress) {
        this.ctx = ctx;
        this.pingAddress = pingAddress;
    }

    void run() {
        use(FakeFriends.full(pingAddress));
        open();
        ServerListsKit.clickCenter(ctx, box(ctx, ServerSession::barView, "tab-friends"));
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> session().bar().friends()), "friends tab is open");
        ctx.waitFor(mc -> !session().friends().controller().rows.snapshot().isEmpty(), 60);
        List<String> keys = keys();
        log("rows %s", keys);
        check(keys.containsAll(List.of("h:invites", "h:servers", "h:worlds", "i:inv-1", "w:00000000-0000-0000-0000-00000000000c")),
                "all three sections: " + keys);
        ctx.waitFor(mc -> find("s:00000000-0000-0000-0000-00000000000a").ping().endsWith("ms"), 200);
        check(!item("s:00000000-0000-0000-0000-00000000000b").canJoin(), "hidden server has no Join");
        SHOTS.shot(ctx, "friends_tab");
        hoverCard();
        actions();
        dragToServers();
        states();
        ctx.runOnClient(mc -> session().bar().showServers());
        ctx.setScreen(TitleScreen::new);
        ctx.runOnClient(mc -> Services.unregister(FriendsActivity.class, fake));
    }

    private void use(FakeFriends f) {
        fake = f;
        ctx.runOnClient(mc -> Services.register(FriendsActivity.class, f));
    }

    private void open() {
        ctx.setScreen(() -> new JoinMultiplayerScreen(new TitleScreen()));
        ctx.waitTicks(6);
    }

    private List<String> keys() {
        return ctx.computeOnClient(mc -> keysHere());
    }

    private static List<String> keysHere() {
        return session().friends().controller().rows.snapshot().stream().map(FriendItem::key).toList();
    }

    private FriendItem item(String key) {
        return ctx.computeOnClient(mc -> find(key));
    }

    private static FriendItem find(String key) {
        for (FriendItem i : session().friends().controller().rows.snapshot()) if (i.key().equals(key)) return i;
        throw new AssertionError("no row " + key);
    }

    private float[] row(String id) {
        return box(ctx, s -> s.friends().view(), id);
    }

    private void hoverCard() {
        float[] r = row("w:00000000-0000-0000-0000-00000000000c");
        ServerListsKit.cursor(ctx, r[0] + 60, r[1] + r[3] / 2);
        ctx.waitFor(mc -> session().friends().controller().cardOpen.peek(), 60);
        ctx.waitTicks(3);
        String text = ctx.computeOnClient(mc -> session().friends().controller().cardText.peek());
        check(text.contains("Cherry Valley"), "card shows the world: " + text);
        SHOTS.shot(ctx, "friends_card");
    }

    private void actions() {
        ServerListsKit.clickCenter(ctx, row("s:00000000-0000-0000-0000-00000000000a:join"));
        check(fake.calls.contains("joinServer:00000000-0000-0000-0000-00000000000a"), "join server: " + fake.calls);
        ServerListsKit.clickCenter(ctx, row("w:00000000-0000-0000-0000-00000000000c:join"));
        check(fake.calls.contains("joinWorld:00000000-0000-0000-0000-00000000000c"), "join world: " + fake.calls);
        ServerListsKit.clickCenter(ctx, row("i:inv-1:join"));
        check(fake.calls.contains("accept:inv-1"), "accept invite: " + fake.calls);
        ServerListsKit.clickCenter(ctx, row("i:inv-1:decline"));
        check(fake.calls.contains("decline:inv-1"), "decline invite: " + fake.calls);
        ctx.waitFor(mc -> keysHere().stream().noneMatch("i:inv-1"::equals), 40);
    }

    private void dragToServers() {
        check(!ServerListsKit.savedAddress(ctx, pingAddress), "friend's server not yet in my list");
        float[] r = row("s:00000000-0000-0000-0000-00000000000a");
        float[] tab = box(ctx, ServerSession::barView, "tab-servers");
        ServerListsKit.drag(ctx, r[0] + 80, r[1] + r[3] / 2, tab[0] + tab[2] / 2, tab[1] + tab[3] / 2, () -> {
            check(ctx.computeOnClient(mc -> session().bar().dropHot.peek()), "servers tab lights up as drop target");
            SHOTS.shot(ctx, "friends_drag_to_servers");
        });
        check(ServerListsKit.savedAddress(ctx, pingAddress), "dropping on Servers adds the friend's server");
    }

    private void states() {
        FakeFriends empty = new FakeFriends();
        use(empty);
        ctx.waitTicks(4);
        ctx.waitFor(mc -> session().friends().view().document().find("fr-empty") != null, 40);
        SHOTS.shot(ctx, "friends_empty");
        empty.state = FriendsActivity.SIGNED_OUT;
        ctx.waitFor(mc -> session().friends().view().document().find("fr-signin") != null, 40);
        SHOTS.shot(ctx, "friends_signed_out");
        float[] b = box(ctx, s -> s.friends().view(), "#fr-signin .empty-action");
        if (b != null) {
            ServerListsKit.clickCenter(ctx, b);
            check(empty.calls.contains("signIn"), "sign in button calls signIn: " + empty.calls);
        }
        ctx.runOnClient(mc -> Services.unregister(FriendsActivity.class, empty));
        ctx.waitFor(mc -> session().friends().view().document().find("fr-off") != null, 40);
    }
}
