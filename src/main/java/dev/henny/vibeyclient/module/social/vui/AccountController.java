package dev.henny.vibeyclient.module.social.vui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.module.social.SocialModule;
import dev.henny.vibeyclient.module.social.model.Model;
import dev.henny.vibeyclient.ui.vui.HeadTextures;
import dev.henny.vibeyclient.ui.vui.WatchedState;
import dev.henny.vibeyclient.ui.vui.dialog.Confirm;
import dev.henny.vibeyclient.ui.vui.settings.Option;
import dev.henny.vibeyclient.ui.vui.settings.SettingRow;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;

import java.util.ArrayList;
import java.util.List;

public final class AccountController {
    private final SocialModule module;
    private final SocialDrawer drawer;
    private final Runnable back;
    public final State<String> name = new State<>("");
    public final State<String> head = new State<>("");
    public final State<Boolean> ready = new State<>(false);
    public final WatchedState<String> status = new WatchedState<>("online");
    public final List<SettingRow> privacy;
    public final ListState<PersonRow> devices = new ListState<>();
    public final ListState<PersonRow> blocked = new ListState<>();

    AccountController(SocialModule module, Runnable back) {
        this.module = module;
        this.drawer = new SocialDrawer(module);
        this.back = back;
        this.privacy = PrivacyRows.of(module);
        status.onChange(drawer::setStatus);
        loadDevices();
    }

    public List<java.util.Map<String, Object>> getStatusOptions() {
        return Option.maps(List.of(new Option("online", Lang.tr("ui.status.online")), new Option("away", Lang.tr("ui.status.away")),
                new Option("busy", Lang.tr("ui.status.busy")), new Option("invisible", Lang.tr("ui.status.invisible"))));
    }

    void sync() {
        ready.set(module.service().ready());
        name.set(drawer.name() == null ? "" : drawer.name());
        head.set(HeadTextures.of(drawer.userId(), drawer.skinUrl()));
        status.quiet(drawer.status());
        for (SettingRow r : privacy) r.sync();
        List<PersonRow> list = new ArrayList<>();
        for (Model.User u : module.service().state.blocked.values()) list.add(new PersonRow(u.id(), u.name(), HeadTextures.of(u.id(), u.skinUrl()), "", false));
        if (!list.equals(blocked.get())) blocked.replaceAll(list);
    }

    private void loadDevices() {
        if (!module.service().ready()) return;
        module.service().devices().whenComplete((list, t) -> Tasks.onMain(() -> {
            if (t != null || list == null) return;
            List<PersonRow> rows = new ArrayList<>();
            for (Model.Device d : list) {
                String note = d.current() ? Lang.tr("ui.account.thisDevice") : d.platform() == null ? "" : d.platform();
                rows.add(new PersonRow(d.id(), d.clientName() == null ? "?" : d.clientName(), "", note, d.current()));
            }
            devices.replaceAll(rows);
        }));
    }

    public void back() {
        back.run();
    }

    public void signIn() {
        module.login();
    }

    public void signOut() {
        Confirm.ask("person", Lang.tr("ui.confirm.signOut"), Lang.tr("ui.confirm.signOutText"), Lang.tr("ui.confirm.signOutYes"),
                true, module::logout);
    }

    public void revoke(PersonRow d) {
        if (d.current()) {
            signOut();
            return;
        }
        module.service().revokeDevice(d.id()).thenRun(() -> Tasks.onMain(this::loadDevices));
    }

    public void unblock(PersonRow b) {
        module.service().unblock(b.id());
    }

    public void run(Object row) {
        if (row instanceof SettingRow r) r.run();
    }
}
