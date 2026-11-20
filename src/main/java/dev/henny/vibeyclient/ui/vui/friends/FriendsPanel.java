package dev.henny.vibeyclient.ui.vui.friends;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.service.FriendsDrawer;
import dev.henny.vibeyclient.ui.vui.settings.Option;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;

import java.util.ArrayList;
import java.util.List;

public final class FriendsPanel {
    public final Account account = new Account();
    public final ListState<FriendRow> friendsOnline = new ListState<>();
    public final ListState<FriendRow> friendsOffline = new ListState<>();
    public final State<Integer> requests = new State<>(0);
    public final State<Boolean> showOffline = new State<>(false);
    public final State<Boolean> adding = new State<>(false);
    public final State<String> addName = new State<>("");
    private List<FriendsDrawer.Friend> last = List.of();

    public List<Option> statusOptions() {
        return List.of(new Option("online", Lang.tr("ui.status.online")), new Option("away", Lang.tr("ui.status.away")),
                new Option("busy", Lang.tr("ui.status.busy")), new Option("invisible", Lang.tr("ui.status.invisible")));
    }

    public void sync() {
        account.sync();
        FriendsDrawer d = Account.drawer().orElse(null);
        requests.set(d == null ? 0 : d.requests());
        List<FriendsDrawer.Friend> now = d == null ? List.of() : d.friends();
        if (now.equals(last) && headsReady()) return;
        last = List.copyOf(now);
        List<FriendRow> on = new ArrayList<>();
        List<FriendRow> off = new ArrayList<>();
        for (FriendsDrawer.Friend f : last) {
            FriendRow r = new FriendRow(f);
            (r.online() ? on : off).add(r);
        }
        friendsOnline.replaceAll(on);
        friendsOffline.replaceAll(off);
    }

    private boolean headsReady() {
        for (FriendRow r : friendsOnline.get()) if (r.getHead().isEmpty()) return false;
        for (FriendRow r : friendsOffline.get()) if (r.getHead().isEmpty()) return false;
        return true;
    }

    public void chat(FriendRow f) { Account.drawer().ifPresent(d -> d.chat(f.getId())); }
    public void invite(FriendRow f) { Account.drawer().ifPresent(d -> d.invite(f.getId())); }
    public void join(FriendRow f) { Account.drawer().ifPresent(d -> d.join(f.getId())); }
    public void openInbox() { Account.drawer().ifPresent(FriendsDrawer::openInbox); }
    public void openAccount() { Account.drawer().ifPresent(FriendsDrawer::openAccount); }
    public void signIn() { Account.drawer().ifPresent(FriendsDrawer::signIn); }
    public void retry() { Account.drawer().ifPresent(FriendsDrawer::retry); }

    public void addFriend(String name) {
        String n = name == null ? "" : name.trim();
        if (!n.isEmpty()) Account.drawer().ifPresent(d -> d.add(n));
        addName.set("");
        adding.set(false);
    }

    public void goOnline() {
        GlobalSettings.onlineFeatures.set(true);
        ModuleManager.get().setEnabled("social", true);
    }
}
