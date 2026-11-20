package dev.henny.vibeyclient.ui.vui.friends;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.FriendsDrawer;
import dev.henny.vibeyclient.ui.vui.HeadTextures;
import dev.henny.vibeyclient.ui.vui.WatchedState;
import dev.henny.vui.bind.State;

public final class Account {
    public final State<String> state = new State<>("connecting");
    public final State<String> name = new State<>("");
    public final State<String> head = new State<>("");
    public final WatchedState<String> status = new WatchedState<>("online");

    Account() {
        status.onChange(s -> drawer().ifPresent(d -> d.setStatus(s)));
    }

    void sync() {
        FriendsDrawer d = drawer().orElse(null);
        if (d == null) {
            state.set(GlobalSettings.online() ? "signedOut" : "off");
            return;
        }
        state.set(GlobalSettings.online() ? d.state() : "off");
        name.set(d.name() == null ? "" : d.name());
        head.set(HeadTextures.of(d.userId(), d.skinUrl()));
        String s = d.status();
        if (s != null) status.quiet(s);
    }

    static java.util.Optional<FriendsDrawer> drawer() {
        return Services.get(FriendsDrawer.class);
    }
}
