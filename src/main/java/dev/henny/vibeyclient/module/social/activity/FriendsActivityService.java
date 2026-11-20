package dev.henny.vibeyclient.module.social.activity;

import dev.henny.vibeyclient.api.service.FriendsActivity;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.social.SocialModule;
import dev.henny.vibeyclient.module.social.SocialState;
import dev.henny.vibeyclient.ui.vui.ClassicUi;
import dev.henny.vibeyclient.ui.vui.Menus;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class FriendsActivityService implements FriendsActivity {
    private final SocialModule module;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private int seen = -1;
    private String seenState = "";

    public FriendsActivityService(SocialModule module) {
        this.module = module;
    }

    public void tick() {
        int rev = revision();
        String st = state();
        if (rev == seen && st.equals(seenState)) return;
        seen = rev;
        seenState = st;
        for (Runnable r : listeners) r.run();
    }

    @Override
    public String state() {
        if (module.service().ready()) return READY;
        return switch (module.backend().state()) {
            case LOGGED_OUT, OFF -> SIGNED_OUT;
            case ERROR -> ERROR;
            default -> CONNECTING;
        };
    }

    private SocialState st() {
        return module.service().state;
    }

    @Override
    public int revision() {
        return st().revision();
    }

    @Override
    public List<OnServer> servers() {
        return module.service().ready() ? ActivityLists.servers(st()) : List.of();
    }

    @Override
    public List<HostedWorld> worlds() {
        return module.service().ready() ? ActivityLists.worlds(st()) : List.of();
    }

    @Override
    public List<Invite> invites() {
        return module.service().ready() ? ActivityLists.invites(st()) : List.of();
    }

    @Override
    public AutoCloseable listen(Runnable onChange) {
        listeners.add(onChange);
        return () -> listeners.remove(onChange);
    }

    @Override
    public void joinServer(String friendId) {
        for (OnServer s : servers()) {
            if (s.friend().id().equals(friendId) && s.shared()) module.invites().join(s.server(), s.address(), 0);
        }
    }

    @Override
    public void joinWorld(String hostId) {
        for (HostedWorld w : worlds()) {
            if (w.host().id().equals(hostId) && w.joinable()) module.invites().join(w.world(), w.address(), w.port());
        }
    }

    @Override
    public void accept(String inviteId) {
        module.service().acceptInvite(inviteId);
    }

    @Override
    public void decline(String inviteId) {
        module.service().declineInvite(inviteId);
    }

    @Override
    public void signIn() {
        module.login();
    }

    @Override
    public void openFriends() {
        if (ClassicUi.enabled()) module.openSidebar(null);
        else Menus.openFriends(ClientCompat.screen());
    }
}
