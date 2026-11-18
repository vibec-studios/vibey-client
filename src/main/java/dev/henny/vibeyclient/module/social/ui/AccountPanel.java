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
import dev.henny.vibeyclient.ui.toast.TextWrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * "Account" tab of the Vibey menu: login state, presence status and status text, privacy, logged-in devices (log
 * out single ones) and blocked players.
 */
public final class AccountPanel implements Panel {
    private static final String[] LEVELS = {"everyone", "friends_of_friends", "friends", "nobody"};
    private static final String[] PRIVACY = {"friendRequests", "directMessages", "invites"};

    private final SocialModule module;
    private final ScrollArea scroll = new ScrollArea();
    private List<Device> devices = new ArrayList<>();
    private boolean devicesLoading;
    private String devicesError;
    private String statusText;
    private String statusTextFor;
    private static final Object K_OPEN = new Object(), K_LOGIN = new Object(), K_LOGOUT = new Object(), K_STATUS = new Object(),
            K_TEXT = new Object(), K_SAVE = new Object(), K_HOSTING = new Object(), K_ONLINE = new Object(), K_RELOAD = new Object();

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
        cy += 20;

        if (!GlobalSettings.online()) {
            cy = notice(g, module.tr("conn.offHint"), cx, cy, cw, Theme.WARN);
            if (Widgets.button(ui, K_ONLINE, cx, cy, 160, 16, module.tr("conn.enableOnline"), Widgets.Style.PRIMARY)) {
                GlobalSettings.onlineFeatures.set(true);
            }
            cy += 24;
            scroll.end(ui, cy + pad);
            return;
        }
        SocialService svc = module.service();
        BackendClient b = module.backend();
        cy = accountCard(ui, svc, b, cx, cy, cw);
        if (svc == null || !svc.ready()) {
            scroll.end(ui, cy + pad);
            return;
        }
        SocialState st = svc.state;
        cy += 10;
        cy = presence(ui, st, cx, cy, cw);
        cy += 10;
        cy = privacy(ui, st, cx, cy, cw);
        cy += 10;
        cy = devices(ui, svc, cx, cy, cw);
        cy += 10;
        cy = blocked(ui, svc, st, cx, cy, cw);
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

    private int section(Gfx g, String title, int x, int y, int w) {
        Widgets.caption(g, title, x, y);
        g.hline(x, y + 11, w, Theme.LINE);
        return y + 18;
    }

    private int accountCard(Ui ui, SocialService svc, BackendClient b, int x, int y, int w) {
        Gfx g = ui.g;
        int h = 54;
        g.rect(x, y, w, h, Theme.SURFACE2);
        g.outline(x, y, w, h, Theme.LINE2);
        User me = svc != null && svc.state.me != null ? svc.state.me.user() : null;
        String name = me != null ? me.name() : b != null && b.accountName() != null ? b.accountName() : "?";
        SocialUi.head(g, me == null ? null : me.id(), me == null ? null : me.skinUrl(), x + 10, y + 11, 32, null);
        g.outline(x + 9, y + 10, 34, 34, Theme.LINE2);
        int tx = x + 52;
        g.text(name, tx, y + 10, Theme.TEXT);
        BackendClient.State state = b == null ? BackendClient.State.OFF : b.state();
        int chipColor = switch (state) {
            case READY -> Theme.OK;
            case ERROR -> Theme.DANGER;
            case LOGGED_OUT, OFF -> Theme.MUTED;
            default -> Theme.WARN;
        };
        Widgets.chip(g, module.tr("account.state." + state.name().toLowerCase(Locale.ROOT)), tx + g.width(name) + 8, y + 8, chipColor);
        String server = b == null ? BackendClient.configuredBaseUrl() : b.baseUrl();
        g.text(g.clip(server + (BackendClient.devAuth() ? "  · DEV LOGIN" : ""), w - 60 - 120), tx, y + 22, Theme.DIM);
        if (me != null) g.text(g.clip(me.id(), w - 60 - 120), tx, y + 33, Theme.DIM);
        // buttons on the right
        int bw = 110;
        int bx = x + w - bw - 10;
        if (state == BackendClient.State.READY) {
            if (Widgets.button(ui, K_OPEN, bx, y + 10, bw, 16, module.tr("account.openFriends"), Widgets.Style.PRIMARY)) {
                module.openSidebar(SidebarRoute.tab(SidebarRoute.Tab.FRIENDS));
            }
            if (Widgets.button(ui, K_LOGOUT, bx, y + 30, bw, 16, module.tr("account.logout"), Widgets.Style.DANGER)) {
                ConfirmScreen.ask(module.tr("account.logoutTitle"), module.tr("account.logoutText"), module.tr("account.logout"), module::logout);
            }
        } else if (state == BackendClient.State.LOGGED_OUT || state == BackendClient.State.ERROR) {
            if (!"banned".equals(b.errorCode()) && Widgets.button(ui, K_LOGIN, bx, y + 10, bw, 16, module.tr("conn.login"), Widgets.Style.PRIMARY)) {
                module.login();
            }
        }
        y += h + 8;
        if (b != null && (state == BackendClient.State.ERROR || (state == BackendClient.State.LOGGED_OUT && b.errorCode() != null))) {
            y = notice(g, Errors.text(b.errorCode(), b.errorMessage(), -1), x, y, w, Theme.DANGER);
        } else if (state == BackendClient.State.LOGGED_OUT) {
            y = notice(g, module.tr("account.loginHint"), x, y, w, Theme.BRAND2);
        }
        return y;
    }

    private int presence(Ui ui, SocialState st, int x, int y, int w) {
        Gfx g = ui.g;
        y = section(g, module.tr("account.presence"), x, y, w);
        int cyw = 150;
        g.text(module.tr("account.status"), x, y + 4, Theme.TEXT);
        String cur = module.status.get().name().toLowerCase(Locale.ROOT);
        int d = Widgets.cycler(ui, K_STATUS, x + w - cyw, y, cyw, Lang.tr(SocialService.statusLabelKey(cur)));
        if (d != 0) {
            PresenceTracker.Status[] v = PresenceTracker.Status.values();
            module.setStatus(v[Math.floorMod(module.status.get().ordinal() + d, v.length)]);
        }
        y += 22;
        String current = st.me == null || st.me.user().statusText() == null ? "" : st.me.user().statusText();
        if (statusText == null || !current.equals(statusTextFor)) {
            statusText = current;
            statusTextFor = current;
        }
        g.text(module.tr("account.statusText"), x, y + 4, Theme.TEXT);
        int fw = Math.min(260, w - 140);
        String save = module.tr("action.save");
        int sw = SocialUi.smallButtonWidth(g, save) + 6;
        SocialUi.field(ui, K_TEXT, x + w - fw, y, fw - sw - 4, statusText, 120, module.tr("account.statusTextPlaceholder"), v -> statusText = v);
        boolean dirty = !statusText.equals(current);
        if (Widgets.button(ui, K_SAVE, x + w - sw, y, sw, 16, save, dirty ? Widgets.Style.PRIMARY : Widgets.Style.DISABLED)) {
            String t = statusText.trim();
            module.service().updateMe(J.of("statusText", t.isEmpty() ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(t)));
        }
        y += 22;
        g.text(g.clip(module.tr("account.presenceHint"), w), x, y, Theme.DIM);
        return y + 14;
    }

    private int privacy(Ui ui, SocialState st, int x, int y, int w) {
        Gfx g = ui.g;
        y = section(g, module.tr("account.privacy"), x, y, w);
        Privacy p = st.me == null ? null : st.me.privacy();
        if (p == null) return y;
        int cyw = 150;
        for (String field : PRIVACY) {
            String value = switch (field) {
                case "friendRequests" -> p.friendRequests();
                case "directMessages" -> p.directMessages();
                default -> p.invites();
            };
            g.text(g.clip(module.tr("privacy." + field), w - cyw - 8), x, y + 4, Theme.TEXT);
            int d = Widgets.cycler(ui, field, x + w - cyw, y, cyw, module.tr("privacy.level." + value));
            if (d != 0) {
                int idx = 0;
                for (int i = 0; i < LEVELS.length; i++) if (LEVELS[i].equals(value)) idx = i;
                String next = LEVELS[Math.floorMod(idx + d, LEVELS.length)];
                JsonObject priv = new JsonObject();
                priv.addProperty(field, next);
                module.service().updateMe(J.of("privacy", priv));
            }
            y += 20;
        }
        g.text(g.clip(module.tr("privacy.showHosting"), w - 40), x, y + 2, Theme.TEXT);
        if (Widgets.toggle(ui, K_HOSTING, x + w - Widgets.TOGGLE_W, y + 1, p.showHosting(), true)) {
            JsonObject priv = new JsonObject();
            priv.addProperty("showHosting", !p.showHosting());
            module.service().updateMe(J.of("privacy", priv));
        }
        y += 18;
        g.text(g.clip(module.tr("privacy.hint"), w), x, y, Theme.DIM);
        return y + 14;
    }

    private int devices(Ui ui, SocialService svc, int x, int y, int w) {
        Gfx g = ui.g;
        y = section(g, module.tr("account.devices"), x, y, w);
        SocialUi.smallButtonRight(ui, K_RELOAD, x + w, y - 19, module.tr("account.reload"), Widgets.Style.GHOST, this::loadDevices);
        if (devicesLoading && devices.isEmpty()) {
            g.text(module.tr("chat.loading"), x, y, Theme.DIM);
            return y + 14;
        }
        if (devicesError != null) {
            g.text(g.clip(devicesError, w), x, y, Theme.DANGER);
            return y + 14;
        }
        for (Device d : devices) {
            int right = x + w;
            String label = d.current() ? module.tr("account.logout") : module.tr("account.revoke");
            right = SocialUi.smallButtonRight(ui, d.id(), right, y + 4, label, Widgets.Style.GHOST, () -> {
                if (d.current()) {
                    ConfirmScreen.ask(module.tr("account.logoutTitle"), module.tr("account.logoutText"), module.tr("account.logout"), module::logout);
                } else {
                    svc.revokeDevice(d.id()).thenRun(() -> Tasks.onMain(this::loadDevices));
                }
            });
            String title = d.clientName() + " " + d.clientVersion();
            g.text(g.clip(title, right - x - 70), x, y + 2, Theme.TEXT);
            if (d.current()) Widgets.chip(g, module.tr("account.thisDevice"), x + Math.min(g.width(title), right - x - 70) + 6, y, Theme.OK);
            String sub = d.platform() + (d.ip() != null ? " · " + d.ip() : "") + " · " + SocialUi.ago(d.lastUsedAt());
            g.text(g.clip(sub, right - x - 4), x, y + 12, Theme.DIM);
            y += 26;
        }
        if (devices.isEmpty()) {
            g.text(module.tr("account.noDevices"), x, y, Theme.DIM);
            y += 14;
        }
        return y;
    }

    private int blocked(Ui ui, SocialService svc, SocialState st, int x, int y, int w) {
        Gfx g = ui.g;
        y = section(g, module.tr("account.blocked", st.blocked.size()), x, y, w);
        if (st.blocked.isEmpty()) {
            g.text(module.tr("account.noBlocked"), x, y, Theme.DIM);
            return y + 14;
        }
        for (User u : new ArrayList<>(st.blocked.values())) {
            int right = SocialUi.smallButtonRight(ui, u.id(), x + w, y + 3, module.tr("action.unblock"), Widgets.Style.GHOST,
                    () -> svc.unblock(u.id()));
            SocialUi.head(g, u.id(), u.skinUrl(), x, y + 2, 16, null);
            g.text(g.clip(u.name(), right - x - 26), x + 22, y + 6, Theme.TEXT);
            y += 22;
        }
        return y;
    }
}
