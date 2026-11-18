package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.module.social.SidebarRoute;
import dev.henny.vibeyclient.module.social.SocialModule;
import dev.henny.vibeyclient.module.social.SocialState;
import dev.henny.vibeyclient.module.social.model.Model.Chat;
import dev.henny.vibeyclient.module.social.model.Model.Friend;
import dev.henny.vibeyclient.module.social.model.Model.FriendRequest;
import dev.henny.vibeyclient.module.social.ui.SocialScreen;
import dev.henny.vibeyclient.ui.VibeyScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;

import java.util.List;
import java.util.function.Function;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.HostingService;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.social.ui.ConfirmScreen;

/**
 * Social module against a LOCAL backend (AUTH_DEV_BYPASS) with scripted friend accounts (Mira, Juno, Kai, Nova,
 * Lumen – see the bot driver). Only runs with {@code -Dvibeyclient.socialTest=true -Dvibeyclient.devAuth=true
 * -Dvibeyclient.api=http://localhost:3100 -Dvibeyclient.username=VibeyTester}. Screenshots: social_*.png.
 */
public final class SocialGameTest implements FabricClientGameTest {
    private static final int SEC = 20;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        mappingChecks();
        if (!Boolean.getBoolean("vibeyclient.socialTest")) return;
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
        SocialModule m = ctx.computeOnClient(mc -> ModuleManager.get().get(SocialModule.class));
        ctx.waitFor(mc -> m.service().ready(), 90 * SEC);
        log("ready as " + ctx.computeOnClient(mc -> m.service().state.me.user().name()));
        refreshCheck(m);
        resumeCheck(ctx, m);
        devicesCheck(ctx, m);

        // incoming requests from the bots
        ctx.waitFor(mc -> incoming(m, "Mira") != null && incoming(m, "Kai") != null && incoming(m, "Nova") != null, 60 * SEC);
        ctx.runOnClient(mc -> {
            m.service().sendFriendRequest("Juno", null);
            m.service().sendFriendRequest("Lumen", null);
        });
        ctx.waitFor(mc -> outgoing(m, "Lumen") != null || friend(m, "Juno") != null, 20 * SEC);
        open(ctx, m, true, SidebarRoute.tab(SidebarRoute.Tab.REQUESTS));
        ctx.waitTicks(20);
        ctx.takeScreenshot("social_01_requests_title");

        ctx.runOnClient(mc -> {
            m.service().acceptRequest(incoming(m, "Mira").id());
            m.service().acceptRequest(incoming(m, "Nova").id());
        });
        ctx.waitFor(mc -> friend(m, "Mira") != null && friend(m, "Juno") != null && friend(m, "Nova") != null, 30 * SEC);
        ctx.waitTicks(40);
        open(ctx, m, true, SidebarRoute.tab(SidebarRoute.Tab.FRIENDS));
        ctx.waitTicks(30);
        ctx.takeScreenshot("social_02_friends_title");

        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            ctx.waitTicks(40);
            open(ctx, m, false, SidebarRoute.tab(SidebarRoute.Tab.FRIENDS));
            ctx.waitTicks(30);
            ctx.takeScreenshot("social_03_friends_world");

            // DM from Mira
            ctx.waitFor(mc -> dm(m, "Mira") != null && dm(m, "Mira").lastMessage() != null, 30 * SEC);
            String dmId = ctx.computeOnClient(mc -> dm(m, "Mira").id());
            ctx.waitTicks(80); // all three messages
            open(ctx, m, false, SidebarRoute.chat(dmId));
            ctx.waitFor(mc -> m.service().state.history(dmId).messages.size() >= 3, 20 * SEC);
            ctx.waitTicks(20);
            ctx.takeScreenshot("social_04_chat");
            ctx.runOnClient(mc -> m.service().sendMessage(dmId, "hello from the game client! see you on Seal Island"));
            ctx.waitFor(mc -> !m.service().state.typingIn(dmId).isEmpty(), 20 * SEC);
            ctx.waitTicks(4);
            ctx.takeScreenshot("social_05_chat_typing");
            ctx.waitFor(mc -> lastText(m, dmId).startsWith("got it"), 20 * SEC);
            ctx.waitTicks(20);
            ctx.takeScreenshot("social_06_chat_reply");

            // group from Juno
            ctx.waitFor(mc -> group(m, "Build crew") != null, 30 * SEC);
            String groupId = ctx.computeOnClient(mc -> group(m, "Build crew").id());
            ctx.waitTicks(40);
            open(ctx, m, false, SidebarRoute.chat(groupId));
            ctx.waitFor(mc -> m.service().state.history(groupId).messages.size() >= 2, 20 * SEC);
            ctx.waitTicks(20);
            ctx.takeScreenshot("social_07_group_chat");
            open(ctx, m, false, SidebarRoute.groupSettings(groupId));
            ctx.waitTicks(20);
            ctx.takeScreenshot("social_08_group_settings");

            // invite toast over the game, then over the pause menu
            ctx.setScreen(() -> null);
            ctx.waitFor(mc -> !m.service().state.openIncomingInvites().isEmpty(), 40 * SEC);
            ctx.waitTicks(10);
            ctx.takeScreenshot("social_09_invite_toast_game");
            ctx.setScreen(() -> new PauseScreen(true));
            ctx.waitTicks(10);
            ctx.takeScreenshot("social_10_invite_toast_pause");

            open(ctx, m, false, SidebarRoute.tab(SidebarRoute.Tab.INBOX));
            ctx.waitTicks(20);
            ctx.takeScreenshot("social_11_inbox");
            String junoId = ctx.computeOnClient(mc -> friend(m, "Juno").user().id());
            open(ctx, m, false, SidebarRoute.profile(junoId));
            ctx.waitTicks(20);
            ctx.takeScreenshot("social_12_profile");
            open(ctx, m, false, SidebarRoute.invite(junoId));
            ctx.waitTicks(20);
            ctx.takeScreenshot("social_13_invite_pick");

            ctx.runOnClient(mc -> SocialScreen.setFullscreen(true));
            open(ctx, m, false, SidebarRoute.chat(dmId));
            ctx.waitTicks(30);
            ctx.takeScreenshot("social_14_fullscreen_chat");
            ctx.runOnClient(mc -> SocialScreen.setFullscreen(false));

            ctx.setScreen(() -> new VibeyScreen(null, "account"));
            ctx.waitTicks(40);
            ctx.takeScreenshot("social_15_account");

            hostingPhase(ctx, m, junoId);

            ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.GERMAN));
            ctx.waitTicks(2);
            open(ctx, m, false, SidebarRoute.tab(SidebarRoute.Tab.FRIENDS));
            ctx.waitTicks(30);
            ctx.takeScreenshot("social_16_friends_de");
            open(ctx, m, false, SidebarRoute.chat(groupId));
            ctx.waitTicks(20);
            ctx.takeScreenshot("social_17_group_de");
            ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));

            // presence reached the backend: our own activity is the singleplayer world
            ctx.setScreen(() -> null);
        }
        // online features off → disconnect, on → reconnect with the stored login
        ctx.runOnClient(mc -> GlobalSettings.onlineFeatures.set(false));
        ctx.waitFor(mc -> !m.backend().running(), 5 * SEC);
        ctx.runOnClient(mc -> GlobalSettings.onlineFeatures.set(true));
        ctx.waitFor(mc -> m.service().ready(), 30 * SEC);
        log("reconnected after online features off/on");

        // revoke this very session (as if done from another device): 4010 → logged out, no automatic reconnect
        var b = m.backend();
        var devs = await(ctx, m.service().devices());
        String current = devs.stream().filter(d -> d.current()).findFirst().orElseThrow().id();
        ctx.runOnClient(mc -> m.service().revokeDevice(current));
        ctx.waitFor(mc -> b.state() == dev.henny.vibeyclient.backend.BackendClient.State.ERROR
                || b.state() == dev.henny.vibeyclient.backend.BackendClient.State.LOGGED_OUT, 15 * SEC);
        ctx.waitTicks(60);
        check(!b.isReady() && !b.hasLogin(), "revoked session must stay logged out, state " + b.state());
        log("revoked: state " + b.state() + " " + b.errorCode());
        open(ctx, m, true, SidebarRoute.tab(SidebarRoute.Tab.FRIENDS));
        ctx.waitTicks(20);
        ctx.takeScreenshot("social_21_revoked");
        ctx.runOnClient(mc -> m.login());
        ctx.waitFor(mc -> m.service().ready(), 30 * SEC);
        // logout → logged out; login again
        ctx.runOnClient(mc -> m.logout());
        ctx.waitFor(mc -> b.state() == dev.henny.vibeyclient.backend.BackendClient.State.LOGGED_OUT, 15 * SEC);
        ctx.runOnClient(mc -> m.login());
        ctx.waitFor(mc -> m.service().ready(), 30 * SEC);
        log("revoke / logout / login ok");
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
        log("done");
    }

    /** Stand-in for the multiplayer module's relay hosting (that module lives in its own branch). */
    static final class StubHosting implements HostingService {
        volatile boolean hosting;

        public boolean canHost() { return true; }
        public boolean isHosting() { return hosting; }
        public String address() { return hosting ? "vibeytester-test.vibey.localhost" : null; }
        public int port() { return 25565; }
        public void startHosting() {
            hosting = true;
            dev.henny.vibeyclient.api.Tasks.onMain(() -> dev.henny.vibeyclient.api.event.Events.post(new HostingChanged(true, address())));
        }
        public void stopHosting() { hosting = false; }
        public void openHostScreen() {}
    }

    /** Invite → "host first?" dialog → hosting → invite sent (Juno accepts it); accepting Juno's invite asks before leaving. */
    private static void hostingPhase(ClientGameTestContext ctx, SocialModule m, String junoId) {
        StubHosting stub = new StubHosting();
        ctx.runOnClient(mc -> Services.register(HostingService.class, stub));
        open(ctx, m, false, SidebarRoute.invite(junoId));
        ctx.waitTicks(20);
        ctx.takeScreenshot("social_18_invite_pick_canhost");
        ctx.runOnClient(mc -> m.invites().inviteToMyWorld(List.of(junoId)));
        ctx.waitFor(mc -> mc.screen instanceof ConfirmScreen, 5 * SEC);
        ctx.waitTicks(10);
        ctx.takeScreenshot("social_19_confirm_host");
        ctx.getInput().pressKey(Keys.ENTER);
        ctx.waitFor(mc -> stub.hosting, 5 * SEC);
        ctx.waitFor(mc -> find(m.service().state.invites.values(), i -> junoId.equals(i.toUserId())) != null, 30 * SEC);
        log("invite to Juno sent while hosting " + stub.address());
        ctx.waitTicks(40); // Juno accepts it (see the bot log), presence shows hosting
        String inviteId = ctx.computeOnClient(mc -> m.service().state.openIncomingInvites().get(0).id());
        ctx.runOnClient(mc -> m.service().acceptInvite(inviteId));
        ctx.waitFor(mc -> mc.screen instanceof ConfirmScreen, 10 * SEC);
        ctx.waitTicks(10);
        ctx.takeScreenshot("social_20_confirm_join");
        ctx.getInput().pressKey(Keys.ESCAPE);
        ctx.waitTicks(5);
        ctx.runOnClient(mc -> {
            stub.stopHosting();
            Services.unregister(HostingService.class, stub);
        });
    }

    /** JSON → model mapping with the shapes of packages/shared (nullable fields, unknown fields, missing fields). */
    private static void mappingChecks() {
        var friend = Friend.from(com.google.gson.JsonParser.parseString("""
                {"user":{"id":"bb6e170d-6947-3845-900a-d52624565dc4","name":"Mira","skin":{"url":null,"model":"slim","capeUrl":null},
                 "statusText":null,"badge":"user","createdAt":"2026-11-18T02:00:00.000Z","futureField":1},
                 "since":"2026-11-18T02:10:00.000Z",
                 "presence":{"userId":"bb6e170d-6947-3845-900a-d52624565dc4","status":"busy","updatedAt":"2026-11-18T02:10:00.000Z",
                 "activity":{"type":"hosting","world":"Seal Island","joinable":true,"join":{"type":"relay","address":"a.b","port":null,"hostSessionId":"h1"}}}}
                """).getAsJsonObject());
        check(friend.user().name().equals("Mira") && friend.user().slim() && friend.user().skinUrl() == null, "user mapping");
        check(friend.presence().status().equals("busy") && friend.presence().online(), "presence mapping");
        check(friend.presence().activity().join().port() == 25565 && "h1".equals(friend.presence().activity().join().hostSessionId()), "join mapping");
        var msg = dev.henny.vibeyclient.module.social.model.Model.Message.from(com.google.gson.JsonParser.parseString("""
                {"id":"42","chatId":"c1","authorId":null,"kind":"system","content":"","system":{"event":"renamed","actorId":"u1","name":"Crew"},
                 "invite":null,"replyToId":null,"nonce":null,"createdAt":"2026-11-18T02:10:00.000Z","editedAt":null,"deleted":false}
                """).getAsJsonObject());
        check(msg.system().event().equals("renamed") && msg.editedAt() == 0 && msg.createdAt() > 0, "message mapping");
        var err = dev.henny.vibeyclient.module.social.Errors.text("rate_limited", "Du schreibst zu schnell", 12);
        check(err.contains("12"), "rate limit text: " + err);
        log("mapping checks ok");
    }

    /** Eight requests at once with an expired access token: exactly one refresh, the session survives (rotation). */
    private static void refreshCheck(SocialModule m) {
        var b = m.backend();
        int before = b.debugExpireAccessToken();
        var futures = new java.util.ArrayList<java.util.concurrent.CompletableFuture<com.google.gson.JsonElement>>();
        for (int i = 0; i < 8; i++) futures.add(b.request("GET", "/api/v1/me", null));
        for (var f : futures) check(f.join().getAsJsonObject().has("id"), "request after refresh");
        check(b.refreshCount() == before + 1, "expected exactly one refresh, got " + (b.refreshCount() - before));
        // still logged in afterwards (a reused refresh token would have ended the session)
        check(b.request("GET", "/api/v1/auth/sessions", null).join().isJsonArray(), "session alive");
        log("refresh serialisation ok (" + (b.refreshCount() - before) + " refresh for 8 parallel requests)");
    }

    /** Network drop → reconnect with resume; an event produced while offline (me.updated) is replayed. */
    private static void resumeCheck(ClientGameTestContext ctx, SocialModule m) {
        var b = m.backend();
        int before = b.resumes();
        ctx.runOnClient(mc -> b.debugDropGateway());
        ctx.waitFor(mc -> !b.isReady(), 5 * SEC);
        String text = "resume test " + System.currentTimeMillis() % 1000;
        b.request("PATCH", "/api/v1/me", dev.henny.vibeyclient.backend.J.of("statusText", text)).join();
        ctx.waitFor(mc -> b.resumes() > before, 30 * SEC);
        check(b.lastReplayed() >= 1, "expected replayed events, got " + b.lastReplayed());
        ctx.waitFor(mc -> text.equals(m.service().state.me.user().statusText()), 5 * SEC);
        b.request("PATCH", "/api/v1/me", dev.henny.vibeyclient.backend.J.of("statusText", "testing Vibey Client")).join();
        log("resume ok (" + b.lastReplayed() + " events replayed)");
    }

    /** Device list + revoking the sessions of earlier test runs. */
    private static void devicesCheck(ClientGameTestContext ctx, SocialModule m) {
        var list = await(ctx, m.service().devices());
        check(list.stream().anyMatch(d -> d.current() && d.clientName().equals("vibey-client")), "current device listed");
        int revoked = 0;
        for (var d : list) {
            if (!d.current()) {
                await(ctx, m.service().revokeDevice(d.id()));
                revoked++;
            }
        }
        var after = await(ctx, m.service().devices());
        check(after.size() == 1, "only this device left, got " + after.size());
        log("devices ok (" + list.size() + " listed, " + revoked + " old ones revoked)");
    }

    /** Futures that complete on the client thread: let the client tick while waiting (never join on the test thread). */
    private static <T> T await(ClientGameTestContext ctx, java.util.concurrent.CompletableFuture<T> f) {
        ctx.waitFor(mc -> f.isDone(), 20 * SEC);
        return f.join();
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    private static void open(ClientGameTestContext ctx, SocialModule m, boolean title, SidebarRoute route) {
        ctx.setScreen(() -> new SocialScreen(title ? new TitleScreen() : null, m, route));
    }

    private static void log(String s) {
        System.out.println("[vibeyclient-social-test] " + s);
    }

    private static <T> T find(Iterable<T> it, Function<T, Boolean> f) {
        for (T t : it) if (f.apply(t)) return t;
        return null;
    }

    private static FriendRequest incoming(SocialModule m, String name) {
        return find(m.service().state.requests.values(), r -> r.incoming() && r.other() != null && r.other().name().equals(name));
    }

    private static FriendRequest outgoing(SocialModule m, String name) {
        return find(m.service().state.requests.values(), r -> !r.incoming() && r.other() != null && r.other().name().equals(name));
    }

    private static Friend friend(SocialModule m, String name) {
        return find(m.service().state.friends.values(), f -> f.user().name().equals(name));
    }

    private static Chat dm(SocialModule m, String name) {
        Friend f = friend(m, name);
        return f == null ? null : m.service().state.dmWith(f.user().id());
    }

    private static Chat group(SocialModule m, String name) {
        return find(m.service().state.chats.values(), c -> !c.dm() && name.equals(c.name()));
    }

    private static String lastText(SocialModule m, String chatId) {
        SocialState.History h = m.service().state.history(chatId);
        return h.messages.isEmpty() ? "" : h.messages.get(h.messages.size() - 1).content();
    }
}
