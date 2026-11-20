package dev.henny.vibeyclient.module.social.vui;

import dev.henny.vibeyclient.api.service.FriendsDrawer;
import dev.henny.vibeyclient.backend.BackendClient;
import dev.henny.vibeyclient.module.social.InviteFlow;
import dev.henny.vibeyclient.module.social.PresenceTracker;
import dev.henny.vibeyclient.module.social.SidebarRoute;
import dev.henny.vibeyclient.module.social.SocialModule;
import dev.henny.vibeyclient.module.social.SocialState;
import dev.henny.vibeyclient.module.social.model.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SocialDrawer implements FriendsDrawer {
    private final SocialModule module;

    public SocialDrawer(SocialModule module) {
        this.module = module;
    }

    @Override
    public String state() {
        if (module.service().ready()) return "ready";
        BackendClient.State s = module.backend().state();
        return switch (s) {
            case LOGGED_OUT, OFF -> "signedOut";
            case ERROR -> "error";
            default -> "connecting";
        };
    }

    @Override
    public String name() {
        Model.Me me = module.service().state.me;
        return me == null ? dev.henny.vibeyclient.client.ClientCompat.playerName() : me.user().name();
    }

    @Override
    public String userId() {
        Model.Me me = module.service().state.me;
        return me == null ? null : me.user().id();
    }

    @Override
    public String skinUrl() {
        Model.Me me = module.service().state.me;
        return me == null ? null : me.user().skinUrl();
    }

    @Override
    public String status() {
        return module.presence() == null ? "online" : module.status.get().name().toLowerCase(Locale.ROOT);
    }

    @Override
    public void setStatus(String status) {
        for (PresenceTracker.Status s : PresenceTracker.Status.values()) {
            if (s.name().equalsIgnoreCase(status)) module.setStatus(s);
        }
    }

    @Override
    public int requests() {
        return module.service().ready() ? module.service().state.incomingRequests() : 0;
    }

    @Override
    public List<Friend> friends() {
        if (!module.service().ready()) return List.of();
        SocialState st = module.service().state;
        InviteFlow.Availability av = module.invites().availability();
        boolean invite = av == InviteFlow.Availability.OK || av == InviteFlow.Availability.START_HOSTING;
        List<Friend> out = new ArrayList<>();
        for (Model.Friend f : st.sortedFriends()) {
            Model.Presence p = f.presence();
            String status = p == null || p.status() == null ? "offline" : p.status();
            boolean on = !"offline".equals(status);
            Model.Activity a = p == null ? null : p.activity();
            boolean join = a != null && a.joinable() && a.join() != null && a.join().address() != null;
            String activity = p == null ? "" : module.service().activityText(p);
            out.add(new Friend(f.user().id(), f.user().name(), f.user().id(), f.user().skinUrl(), status,
                    activity == null ? "" : activity, on && invite, on && join));
        }
        return out;
    }

    @Override
    public void chat(String friendId) {
        module.service().openDm(friendId).thenAccept(c -> dev.henny.vibeyclient.api.Tasks.onMain(
                () -> module.openSidebar(SidebarRoute.chat(c.id()))));
    }

    @Override
    public void invite(String friendId) {
        module.invites().inviteToMyWorld(List.of(friendId));
    }

    @Override
    public void join(String friendId) {
        Model.Friend f = module.service().state.friends.get(friendId);
        if (f == null || f.presence() == null || f.presence().activity() == null) return;
        Model.Join j = f.presence().activity().join();
        if (j != null) module.invites().join(f.presence().activity().world(), j.address(), j.port());
    }

    @Override
    public void add(String name) {
        module.service().sendFriendRequest(name, null);
    }

    @Override
    public void openInbox() {
        module.openSidebar(SidebarRoute.tab(SidebarRoute.Tab.REQUESTS));
    }

    @Override
    public void openAccount() {
        AccountScreen.open(dev.henny.vibeyclient.client.ClientCompat.screen(), module);
    }

    @Override
    public void signIn() {
        module.login();
    }

    @Override
    public void retry() {
        module.login();
    }
}
