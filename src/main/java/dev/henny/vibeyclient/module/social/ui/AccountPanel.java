package dev.henny.vibeyclient.module.social.ui;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.backend.BackendClient;
import dev.henny.vibeyclient.backend.J;
import dev.henny.vibeyclient.module.social.Errors;
import dev.henny.vibeyclient.module.social.PresenceTracker;
import dev.henny.vibeyclient.module.social.SidebarRoute;
import dev.henny.vibeyclient.module.social.SocialModule;
import dev.henny.vibeyclient.module.social.SocialService;
import dev.henny.vibeyclient.module.social.SocialState;
import dev.henny.vibeyclient.module.social.model.Model.Device;
import dev.henny.vibeyclient.module.social.model.Model.Privacy;
import dev.henny.vibeyclient.module.social.model.Model.User;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Panel;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;
import dev.henny.vibeyclient.ui.screen.SettingsView;
import dev.henny.vibeyclient.ui.toast.TextWrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * "Account" tab of the Vibey menu, in plain words: who you are signed in as (with sign in / out), your status, who
 * may contact you, your signed-in devices and blocked players. Technical details (server, account id, device
 * addresses) are in the collapsed advanced area at the bottom.
 */
public final class AccountPanel implements Panel {
    private static final String[] LEVELS = {"everyone", "friends_of_friends", "friends", "nobody"};
    private static final String[] PRIVACY = {"friendRequests", "directMessages", "invites"};
    private static final int ROW = 26;

    private final SocialModule module;
    private final ScrollArea scroll = new ScrollArea();
    private List<Device> devices = new ArrayList<>();
    private boolean devicesLoading;
    private String devicesError;
    private String statusText;
    private String statusTextFor;
    private static final Object K_OPEN = new Object(), K_LOGIN = new Object(), K_LOGOUT = new Object(), K_STATUS = new Object(),
            K_TEXT = new Object(), K_SAVE = new Object(), K_HOSTING = new Object(), K_SHARE = new Object(), K_ONLINE = new Object(), K_RELOAD = new Object();

    public AccountPanel(SocialModule module) {
        this.module = module;
    }

    @Override
    public void onShow() {
        loadDevices();
    }

    private void loadDevices() {
        SocialService svc = module.service();
        if (svc == null || !svc.ready()) return;
        devicesLoading = true;
        devicesError = null;
        svc.devices().whenComplete((list, t) -> Tasks.onMain(() -> {
            devicesLoading = false;
            if (t != null) devicesError = Errors.text(t);
            else devices = list;
        }));
    }

    @Override
    public void draw(Ui ui, int x, int y, int w, int h) {
        Gfx g = ui.g;
        int pad = Theme.PAD + 4;
        int cx = x + pad;
        int cw = w - pad * 2;
        int cy = scroll.begin(ui, x, y, w, h) + pad;
        g.bold(module.tr("account.title"), cx, cy, Theme.TEXT);
        cy += 13;
        g.text(g.clip(module.tr("account.description"), cw), cx, cy, Theme.MUTED);
        cy += 18;

        if (!GlobalSettings.online()) {
            cy = notice(g, module.tr("conn.offHint"), cx, cy, cw, Theme.WARN);
            if (Widgets.button(ui, K_ONLINE, cx, cy, Math.max(160, g.width(module.tr("conn.enableOnline")) + 20), 16,
                    module.tr("conn.enableOnline"), Widgets.Style.PRIMARY)) {
                GlobalSettings.onlineFeatures.set(true);
            }
            cy += 24;
            scroll.end(ui, cy + pad);
            return;
        }
        SocialService svc = module.service();
        BackendClient b = module.backend();
        cy = profile(ui, svc, b, cx, cy, cw);
        if (svc != null && svc.ready()) {
            SocialState st = svc.state;
            cy = presence(ui, st, cx, cy + 12, cw);
            cy = privacy(ui, st, cx, cy + 12, cw);
            cy = devices(ui, svc, cx, cy + 12, cw);
            cy = blocked(ui, svc, st, cx, cy + 12, cw);
        }
        cy = SettingsView.drawAdvanced(ui, module.settings(), cx, cy, cw, true, (u, ax, ay, aw) -> technical(u, svc, b, ax, ay, aw));
        scroll.end(ui, cy + pad);
    }

    private int notice(Gfx g, String text, int x, int y, int w, int color) {
        List<String> lines = TextWrap.wrap(g, text, w - 14, 5);
        int h = lines.size() * 10 + 10;
        g.rect(x, y, w, h, Theme.alpha(color, 0.10f));
        g.vline(x, y, h, color);
        int ly = y + 5;
        for (String l : lines) {
            g.text(l, x + 8, ly, color);
            ly += 10;
        }
        return y + h + 8;
    }

    // ============================================================================================ profile

    private int profile(Ui ui, SocialService svc, BackendClient b, int x, int y, int w) {
        Gfx g = ui.g;
        int h = 52;
        Widgets.card(g, x, y, w, h);
        User me = svc != null && svc.state.me != null ? svc.state.me.user() : null;
        String name = me != null ? me.name() : b != null && b.accountName() != null ? b.accountName() : "?";
        SocialUi.head(g, me == null ? null : me.id(), me == null ? null : me.skinUrl(), x + 10, y + 10, 32, null);
        g.outline(x + 9, y + 9, 34, 34, Theme.LINE2);
        BackendClient.State state = b == null ? BackendClient.State.OFF : b.state();
        int chipColor = switch (state) {
            case READY -> Theme.OK;
            case ERROR -> Theme.DANGER;
            case LOGGED_OUT, OFF -> Theme.MUTED;
            default -> Theme.WARN;
        };
        // buttons on the right
        int right = x + w - 10;
        int buttonsW = 0;
        if (state == BackendClient.State.READY) {
            String open = module.tr("account.openFriends"), out = module.tr("account.logout");
            int ow = g.width(open) + 20, lw = g.width(out) + 20;
            buttonsW = ow + lw + 6;
            if (Widgets.button(ui, K_LOGOUT, right - lw, y + 18, lw, 16, out, Widgets.Style.GHOST)) {
                ConfirmScreen.ask(module.tr("account.logoutTitle"), module.tr("account.logoutText"), out, module::logout);
            }
            if (Widgets.button(ui, K_OPEN, right - lw - 6 - ow, y + 18, ow, 16, open, Widgets.Style.PRIMARY)) {
                module.openSidebar(SidebarRoute.tab(SidebarRoute.Tab.FRIENDS));
            }
        } else if ((state == BackendClient.State.LOGGED_OUT || state == BackendClient.State.ERROR) && !"banned".equals(b.errorCode())) {
            String in = module.tr("conn.login");
            int iw = g.width(in) + 24;
            buttonsW = iw;
            if (Widgets.button(ui, K_LOGIN, right - iw, y + 18, iw, 16, in, Widgets.Style.PRIMARY)) module.login();
        }
        int tx = x + 52;
        int textW = Math.max(40, right - buttonsW - 10 - tx);
        g.bold(g.clip(name, textW), tx, y + 13, Theme.TEXT);
        String chip = module.tr("account.state." + state.name().toLowerCase(Locale.ROOT));
        g.rect(tx, y + 29, 5, 5, chipColor);
        String sub = state == BackendClient.State.READY ? chip + " · " + module.tr("account.signedInWith") : chip;
        g.text(g.clip(sub, textW - 9), tx + 9, y + 28, Theme.MUTED);
        y += h + 8;
        if (b != null && (state == BackendClient.State.ERROR || (state == BackendClient.State.LOGGED_OUT && b.errorCode() != null))) {
            y = notice(g, Errors.text(b.errorCode(), b.errorMessage(), -1), x, y, w, Theme.DANGER);
        } else if (state == BackendClient.State.LOGGED_OUT) {
            y = notice(g, module.tr("account.loginHint"), x, y, w, Theme.BRAND2);
        }
        return y;
    }

    // ============================================================================================ status

    private int presence(Ui ui, SocialState st, int x, int y, int w) {
        Gfx g = ui.g;
        Widgets.caption(g, module.tr("account.presence"), x + 1, y);
        y += 12;
        Widgets.card(g, x, y, w, ROW * 2);
        int cyw = Math.min(150, w / 2);
        g.text(module.tr("account.status"), x + 10, y + 9, Theme.TEXT);
        String cur = module.status.get().name().toLowerCase(Locale.ROOT);
        int d = Widgets.cycler(ui, K_STATUS, x + w - 10 - cyw, y + 5, cyw, Lang.tr(SocialService.statusLabelKey(cur)));
        if (d != 0) {
            PresenceTracker.Status[] v = PresenceTracker.Status.values();
            module.setStatus(v[Math.floorMod(module.status.get().ordinal() + d, v.length)]);
        }
        g.hline(x + 1, y + ROW, w - 2, Theme.LINE);
        int ry = y + ROW;
        String current = st.me == null || st.me.user().statusText() == null ? "" : st.me.user().statusText();
        if (statusText == null || !current.equals(statusTextFor)) {
            statusText = current;
            statusTextFor = current;
        }
        g.text(module.tr("account.statusText"), x + 10, ry + 9, Theme.TEXT);
        int fw = Math.min(240, w - g.width(module.tr("account.statusText")) - 40);
        String save = module.tr("action.save");
        int sw = SocialUi.smallButtonWidth(g, save) + 6;
        SocialUi.field(ui, K_TEXT, x + w - 10 - fw, ry + 5, fw - sw - 4, statusText, 120, module.tr("account.statusTextPlaceholder"), v -> statusText = v);
        boolean dirty = !statusText.equals(current);
        if (Widgets.button(ui, K_SAVE, x + w - 10 - sw, ry + 5, sw, 16, save, dirty ? Widgets.Style.PRIMARY : Widgets.Style.DISABLED)) {
            String t = statusText.trim();
            module.service().updateMe(J.of("statusText", t.isEmpty() ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(t)));
        }
        y += ROW * 2 + 5;
        g.text(g.clip(module.tr("account.presenceHint"), w), x + 1, y, Theme.DIM);
        return y + 10;
    }

    // ============================================================================================ privacy

    private int privacy(Ui ui, SocialState st, int x, int y, int w) {
        Gfx g = ui.g;
        Widgets.caption(g, module.tr("account.privacy"), x + 1, y);
        y += 12;
        Privacy p = st.me == null ? null : st.me.privacy();
        if (p == null) return y;
        Widgets.card(g, x, y, w, ROW * 5);
        int cyw = Math.min(150, w / 2 - 10);
        int ry = y;
        for (String field : PRIVACY) {
            String value = switch (field) {
                case "friendRequests" -> p.friendRequests();
                case "directMessages" -> p.directMessages();
                default -> p.invites();
            };
            g.text(g.clip(module.tr("privacy." + field), w - cyw - 30), x + 10, ry + 9, Theme.TEXT);
            int d = Widgets.cycler(ui, field, x + w - 10 - cyw, ry + 5, cyw, module.tr("privacy.level." + value));
            if (d != 0) {
                int idx = 0;
                for (int i = 0; i < LEVELS.length; i++) if (LEVELS[i].equals(value)) idx = i;
                String next = LEVELS[Math.floorMod(idx + d, LEVELS.length)];
                JsonObject priv = new JsonObject();
                priv.addProperty(field, next);
                module.service().updateMe(J.of("privacy", priv));
            }
            ry += ROW;
            g.hline(x + 1, ry, w - 2, Theme.LINE);
        }
        toggleRow(ui, K_HOSTING, "showHosting", p.showHosting(), x, ry, w);
        g.hline(x + 1, ry + ROW, w - 2, Theme.LINE);
        toggleRow(ui, K_SHARE, "shareServerAddress", p.shareServerAddress(), x, ry + ROW, w);
        y += ROW * 5 + 5;
        g.text(g.clip(module.tr("privacy.hint"), w), x + 1, y, Theme.DIM);
        return y + 10;
    }

    private void toggleRow(Ui ui, Object key, String field, boolean on, int x, int ry, int w) {
        Gfx g = ui.g;
        boolean clicked = ui.clicked(x + 1, ry, w - 2, ROW);
        boolean hot = ui.focusedLast() || ui.hover(x + 1, ry, w - 2, ROW);
        if (hot) g.rect(x + 1, ry + 1, w - 2, ROW - 2, Theme.alpha(Theme.SURFACE3, 1f));
        g.text(g.clip(module.tr("privacy." + field), w - 60), x + 10, ry + 9, Theme.TEXT);
        Widgets.drawToggle(ui, key, x + w - 10 - Widgets.TOGGLE_W, ry + 7, on, true, hot);
        if (clicked) {
            JsonObject priv = new JsonObject();
            priv.addProperty(field, !on);
            module.service().updateMe(J.of("privacy", priv));
        }
    }

    // ============================================================================================ devices

    private int devices(Ui ui, SocialService svc, int x, int y, int w) {
        Gfx g = ui.g;
        Widgets.caption(g, module.tr("account.devices"), x + 1, y);
        SocialUi.smallButtonRight(ui, K_RELOAD, x + w, y - 3, module.tr("account.reload"), Widgets.Style.GHOST, this::loadDevices);
        y += 12;
        if ((devicesLoading && devices.isEmpty()) || devicesError != null || devices.isEmpty()) {
            Widgets.card(g, x, y, w, ROW);
            String text = devicesError != null ? devicesError : devicesLoading ? module.tr("chat.loading") : module.tr("account.noDevices");
            g.text(g.clip(text, w - 20), x + 10, y + 9, devicesError != null ? Theme.DANGER : Theme.DIM);
            return y + ROW;
        }
        int rh = 30;
        Widgets.card(g, x, y, w, devices.size() * rh);
        int ry = y;
        boolean first = true;
        for (Device d : devices) {
            if (!first) g.hline(x + 1, ry, w - 2, Theme.LINE);
            first = false;
            String label = d.current() ? module.tr("account.logout") : module.tr("account.revoke");
            int right = SocialUi.smallButtonRight(ui, d.id(), x + w - 8, ry + 8, label, Widgets.Style.GHOST, () -> {
                if (d.current()) {
                    ConfirmScreen.ask(module.tr("account.logoutTitle"), module.tr("account.logoutText"), module.tr("account.logout"), module::logout);
                } else {
                    svc.revokeDevice(d.id()).thenRun(() -> Tasks.onMain(this::loadDevices));
                }
            });
            String title = d.clientName() + " " + d.clientVersion();
            int tw = right - x - 20;
            g.text(g.clip(title, tw - (d.current() ? 70 : 0)), x + 10, ry + 6, Theme.TEXT);
            if (d.current()) Widgets.chip(g, module.tr("account.thisDevice"), x + 10 + Math.min(g.width(title), tw - 70) + 6, ry + 4, Theme.OK);
            String sub = d.platform() + " · " + SocialUi.ago(d.lastUsedAt());
            g.text(g.clip(sub, tw), x + 10, ry + 17, Theme.DIM);
            ry += rh;
        }
        return ry;
    }

    // ============================================================================================ blocked

    private int blocked(Ui ui, SocialService svc, SocialState st, int x, int y, int w) {
        Gfx g = ui.g;
        Widgets.caption(g, module.tr("account.blocked", st.blocked.size()), x + 1, y);
        y += 12;
        if (st.blocked.isEmpty()) {
            Widgets.card(g, x, y, w, ROW);
            g.text(g.clip(module.tr("account.noBlocked"), w - 20), x + 10, y + 9, Theme.DIM);
            return y + ROW;
        }
        List<User> list = new ArrayList<>(st.blocked.values());
        Widgets.card(g, x, y, w, list.size() * ROW);
        int ry = y;
        for (User u : list) {
            if (ry > y) g.hline(x + 1, ry, w - 2, Theme.LINE);
            int right = SocialUi.smallButtonRight(ui, u.id(), x + w - 8, ry + 6, module.tr("action.unblock"), Widgets.Style.GHOST,
                    () -> svc.unblock(u.id()));
            SocialUi.head(g, u.id(), u.skinUrl(), x + 10, ry + 5, 16, null);
            g.text(g.clip(u.name(), right - x - 40), x + 32, ry + 9, Theme.TEXT);
            ry += ROW;
        }
        return ry;
    }

    // ============================================================================================ advanced

    /** Server address, account id, sign-in method, device addresses. */
    private int technical(Ui ui, SocialService svc, BackendClient b, int x, int y, int w) {
        Gfx g = ui.g;
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {module.tr("account.tech.server"), b == null ? BackendClient.configuredBaseUrl() : b.baseUrl()});
        User me = svc != null && svc.state.me != null ? svc.state.me.user() : null;
        if (me != null) rows.add(new String[] {module.tr("account.tech.id"), me.id()});
        rows.add(new String[] {module.tr("account.tech.login"), module.tr(BackendClient.devAuth() ? "account.tech.loginDev" : "account.tech.loginMojang")});
        for (Device d : devices) {
            if (d.ip() != null) rows.add(new String[] {d.clientName() + " · " + d.platform(), d.ip()});
        }
        int rh = 16;
        int h = rows.size() * rh + 12;
        Widgets.card(g, x, y, w, h);
        int keyW = 0;
        for (String[] r : rows) keyW = Math.max(keyW, g.width(r[0]));
        keyW = Math.min(keyW + 12, w / 2);
        int ry = y + 6;
        for (String[] r : rows) {
            g.text(g.clip(r[0], keyW - 6), x + 10, ry + 4, Theme.DIM);
            g.text(g.clip(r[1], w - keyW - 20), x + 10 + keyW, ry + 4, Theme.TEXT);
            ry += rh;
        }
        return y + h;
    }
}
