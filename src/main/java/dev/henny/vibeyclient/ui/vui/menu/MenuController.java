package dev.henny.vibeyclient.ui.vui.menu;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.ui.vui.WatchedState;
import dev.henny.vibeyclient.ui.vui.friends.Account;
import dev.henny.vibeyclient.ui.vui.friends.FriendRow;
import dev.henny.vibeyclient.ui.vui.friends.FriendsPanel;
import dev.henny.vibeyclient.ui.vui.onboarding.Onboarding;
import dev.henny.vibeyclient.ui.vui.settings.Option;
import dev.henny.vibeyclient.ui.vui.settings.SettingRow;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;

import java.util.List;

public final class MenuController {
    private final ModuleGrid grid = new ModuleGrid();
    private final QuickSettings quickPart = new QuickSettings();
    private final FriendsPanel friends = new FriendsPanel();
    private final NoticeList noticeList = new NoticeList();
    private MenuHost host;

    public final State<String> panel = new State<>("");
    public final WatchedState<String> query = new WatchedState<>("");
    public final State<ModulePage> page = new State<>(null);
    public final State<Boolean> moreOpen = new State<>(false);
    public final ListState<ModuleTile> modules = grid.modules;
    public final ListState<ModuleTile> creators = grid.creators;
    public final State<Boolean> showCreators = grid.showCreators;
    public final ListState<SettingRow> quick = quickPart.quick;
    public final ListState<SettingRow> quickMore = quickPart.quickMore;
    public final State<Boolean> quickMoreOpen = quickPart.quickMoreOpen;
    public final ListState<NoticeRow> notices = noticeList.notices;
    public final Account account = friends.account;
    public final ListState<FriendRow> friendsOnline = friends.friendsOnline;
    public final ListState<FriendRow> friendsOffline = friends.friendsOffline;
    public final State<Integer> requests = friends.requests;
    public final State<Boolean> showOffline = friends.showOffline;
    public final State<Boolean> adding = friends.adding;
    public final State<String> addName = friends.addName;
    public final String version = BuildInfo.VERSION;

    public MenuController() {
        query.onChange(grid::filter);
        sync();
    }

    void attach(MenuHost h) {
        host = h;
    }

    public String getView() { return page.get() == null ? "grid" : "page"; }
    public String getStatus() { return Connection.status(); }
    public String getStatusLabel() { return Connection.label(); }
    public String getStatusTip() { return Connection.tip(); }
    public List<java.util.Map<String, Object>> getStatusOptions() { return Option.maps(friends.statusOptions()); }
    public ModuleGrid grid() { return grid; }

    public void sync() {
        grid.sync();
        quickPart.sync();
        friends.sync();
        noticeList.sync();
        ModulePage p = page.peek();
        if (p != null && (p.stale() || !p.module().visible())) page.set(p.module().visible() ? new ModulePage(p.module()) : null);
        else if (p != null) p.sync();
    }

    public void open(ModuleTile m) {
        openModule(m.module());
    }

    public void openModule(VibeyModule m) {
        if (m == null) return;
        moreOpen.set(false);
        panel.set("");
        page.set(new ModulePage(m));
    }

    public void toggled(ModuleTile m) {
        grid.sync();
    }

    public void back() {
        page.set(null);
    }

    public void run(Object row) {
        if (row instanceof SettingRow r) r.run();
        sync();
    }

    public void close() {
        if (host != null) host.closeNow();
    }

    public void openWardrobe() {
        if (host != null) Places.entry("wardrobe", host.screen());
    }

    public void openHud() {
        if (host != null) Places.hud(host.screen());
    }

    public void link(String id) {
        if (host != null) Places.link(host.screen(), id);
    }

    public void showWelcome() {
        panel.set("");
        if (host != null) Onboarding.open(host.screen());
    }

    public void openNotice(NoticeRow n) { noticeList.open(n); }
    public void dismissNotice(NoticeRow n) { noticeList.dismiss(n); }
    public void openInbox() { friends.openInbox(); }
    public void chat(FriendRow f) { friends.chat(f); }
    public void invite(FriendRow f) { friends.invite(f); }
    public void join(FriendRow f) { friends.join(f); }
    public void addFriend(String name) { friends.addFriend(name); }
    public void openAccount() { friends.openAccount(); }
    public void goOnline() { friends.goOnline(); }
    public void signIn() { friends.signIn(); }
    public void retry() { friends.retry(); }

    public boolean escape() {
        if (page.peek() != null) {
            back();
            return true;
        }
        if (!query.peek().isEmpty()) {
            query.set("");
            return true;
        }
        if (!panel.peek().isEmpty()) {
            panel.set("");
            return true;
        }
        return false;
    }

    public void preset(String moduleId, String openPanel) {
        if (moduleId != null) openModule(ModuleManager.get().get(moduleId));
        if (openPanel != null) panel.set(openPanel);
    }
}
