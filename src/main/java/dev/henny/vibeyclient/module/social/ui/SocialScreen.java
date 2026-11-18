package dev.henny.vibeyclient.module.social.ui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.backend.BackendClient;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.social.PresenceTracker;
import dev.henny.vibeyclient.module.social.SidebarRoute;
import dev.henny.vibeyclient.module.social.SocialCompat;
import dev.henny.vibeyclient.module.social.SocialModule;
import dev.henny.vibeyclient.module.social.SocialService;
import dev.henny.vibeyclient.module.social.SocialState;
import dev.henny.vibeyclient.ui.Anim;
import dev.henny.vibeyclient.ui.BaseScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.UiHost;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.Widgets;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * The social sidebar: slides in from the right over the game or the menu that was open (drawn behind it), with
 * tabs Friends / Chats / Requests / Inbox and detail pages (chat, profile, groups, invites). The expand button turns
 * it into a full-screen two-column view. A click outside the panel, Escape or the social key closes it.
 */
public final class SocialScreen extends BaseScreen {
    public static final int HEADER = 34;
    public static final int TABS = 22;
    private static final int SIDEBAR_W = 284;
    private static final int LIST_W = 270;

    private final Screen parent;
    final SocialModule module;
    final SocialService service;
    private final Ui ui = UiHost.create(this::addWidget);
    private final Anim open = new Anim(0f);
    private final Anim widthAnim = new Anim(SIDEBAR_W);
    private boolean closing;
    private static boolean fullscreen;

    private SidebarRoute.Tab tab = SidebarRoute.Tab.FRIENDS;
    private SidebarRoute route = SidebarRoute.tab(SidebarRoute.Tab.FRIENDS);
    private final Deque<SidebarRoute> back = new ArrayDeque<>();
    private boolean statusMenu;

    final FriendsView friends = new FriendsView(this);
    final ChatsView chats = new ChatsView(this);
    final RequestsView requests = new RequestsView(this);
    final InboxView inbox = new InboxView(this);
    final ChatView chat = new ChatView(this);
    final ProfileView profile = new ProfileView(this);
    final GroupView group = new GroupView(this);
    final InviteView invite = new InviteView(this);

    public SocialScreen(Screen parent, SocialModule module, SidebarRoute start) {
        super(Lang.text("social.name"));
        this.parent = parent;
        this.module = module;
        this.service = module.service();
        if (start != null) navigate(start, false);
    }

    public Screen parent() {
        return parent;
    }

    public SocialState state() {
        return service.state;
    }

    public SidebarRoute route() {
        return route;
    }

    public static boolean fullscreen() {
        return fullscreen;
    }

    public static void setFullscreen(boolean on) {
        fullscreen = on;
    }

    // ============================================================================================ navigation

    public void navigate(SidebarRoute r) {
        navigate(r, true);
    }

    private void navigate(SidebarRoute r, boolean remember) {
        if (r.equals(route)) return;
        if (r.isTab()) {
            tab = r.tab();
            back.clear();
        } else if (remember && !route.isTab()) {
            back.push(route);
        } else if (r.tab() != null) {
            tab = r.tab();
        }
        route = r;
        if (r.kind() == SidebarRoute.Kind.CHAT) chat.open(r.id());
        if (r.kind() == SidebarRoute.Kind.PROFILE) profile.open(r.id());
        if (r.kind() == SidebarRoute.Kind.NEW_GROUP || r.kind() == SidebarRoute.Kind.GROUP_SETTINGS) group.open(r.id());
        if (r.kind() == SidebarRoute.Kind.INVITE) invite.open(r.id());
    }

    public void goBack() {
        if (!back.isEmpty()) {
            route = back.pop();
            if (route.kind() == SidebarRoute.Kind.CHAT) chat.open(route.id());
        } else {
            route = SidebarRoute.tab(tab);
        }
    }

    /** From outside (toast click, keybind with a target): show {@code r} in this open screen. */
    public void show(SidebarRoute r) {
        back.clear();
        navigate(r, false);
    }

    // ============================================================================================ frame

    @Override
    protected void init() {
        if (parent != null) SocialCompat.resize(parent, width, height);
        UiHost.reattach(ui);
    }

    private int panelWidth() {
        int full = Math.min(width - 24, 820);
        int target = fullscreen ? full : Math.min(SIDEBAR_W, width - 16);
        return Math.round(widthAnim.to(target));
    }

    @Override
    protected void draw(Gfx g, int mx, int my, float delta) {
        if (parent != null) SocialCompat.renderBehind(parent, g.g, delta);
        UiHost.begin(ui, g, mx, my, delta);
        float o = open.to(closing ? 0f : 1f);
        if (closing && o <= 0.001f) {
            UiHost.end(ui);
            ClientCompat.openScreen(parent);
            return;
        }
        boolean world = SocialCompat.inWorld();
        g.rect(0, 0, width, height, Theme.alpha(world && parent == null ? 0x50000000 : 0x8C000000, o));

        int w = panelWidth();
        int x;
        int y;
        int h;
        if (fullscreen) {
            int full = Math.min(width - 24, 820);
            x = (width - full) / 2 + (full - w) + Math.round((1f - o) * 24);
            y = 12;
            h = height - 24;
        } else {
            x = width - w + Math.round((1f - o) * (w + 8));
            y = 0;
            h = height;
        }
        // A click outside the panel closes it (the four areas around the panel).
        if (ui.clicked(0, 0, x, height) || ui.clicked(x + w, 0, width - x - w, height)
                || ui.clicked(x, 0, w, y) || ui.clicked(x, y + h, w, height - y - h)) {
            if (statusMenu) statusMenu = false;
            else onClose();
        }

        // status menu clicks first (it is drawn last, on top)
        int smX = x + 30, smY = y + 26;
        if (statusMenu) handleStatusMenu(smX, smY);

        if (!fullscreen) dev.henny.vibeyclient.ui.toast.ToastStack.reserveRight(width - x + 4);
        g.rect(x, y, w, h, Theme.SURFACE);
        g.vline(x - 1, y, h, Theme.LINE2);
        if (fullscreen) g.outline(x - 1, y - 1, w + 2, h + 2, Theme.LINE2);

        drawHeader(g, x, y, w);
        int cy = y + HEADER;
        BackendClient b = module.backend();
        if (b == null || !service.ready()) {
            drawConnection(g, x, cy, w, h - HEADER, b);
        } else if (fullscreen && w >= 520) {
            int lw = Math.min(LIST_W, w * 2 / 5);
            drawTabs(g, x, cy, lw);
            drawTab(x, cy + TABS, lw, h - HEADER - TABS);
            g.vline(x + lw, cy, h - HEADER, Theme.LINE);
            int dx = x + lw + 1;
            int dw = w - lw - 1;
            if (route.isTab()) {
                SocialUi.empty(g, Lang.tr("social.empty.selectChat"), dx, cy + (h - HEADER) / 2 - 4, dw);
            } else {
                drawRoute(dx, cy, dw, h - HEADER, false);
            }
        } else if (route.isTab()) {
            drawTabs(g, x, cy, w);
            drawTab(x, cy + TABS, w, h - HEADER - TABS);
        } else {
            drawRoute(x, cy, w, h - HEADER, true);
        }

        if (statusMenu) drawStatusMenu(g, smX, smY);
        if (o < 1f && fullscreen) g.rect(x - 1, y - 1, w + 2, h + 2, Theme.alpha(Theme.SURFACE, 1f - o));
        UiHost.end(ui);
    }

    private void drawTab(int x, int y, int w, int h) {
        switch (tab) {
            case FRIENDS -> friends.draw(ui, x, y, w, h);
            case CHATS -> chats.draw(ui, x, y, w, h);
            case REQUESTS -> requests.draw(ui, x, y, w, h);
            case INBOX -> inbox.draw(ui, x, y, w, h);
        }
    }

    private void drawRoute(int x, int y, int w, int h, boolean showBack) {
        switch (route.kind()) {
            case CHAT -> chat.draw(ui, x, y, w, h, showBack);
            case PROFILE -> profile.draw(ui, x, y, w, h, showBack);
            case NEW_GROUP, GROUP_SETTINGS -> group.draw(ui, x, y, w, h, showBack);
            case INVITE -> invite.draw(ui, x, y, w, h, showBack);
            default -> {
            }
        }
    }

    // ============================================================================================ header

    private static final Object K_EXPAND = new Object();
    private static final Object K_CLOSE = new Object();
    private static final Object K_STATUS = new Object();

    private void drawHeader(Gfx g, int x, int y, int w) {
        SocialState s = service.state;
        int hx = x + 10;
        int hy = y + 9;
        String status = module.presence().effectiveStatus();
        if (s.me != null) {
            SocialUi.head(g, s.me.user().id(), s.me.user().skinUrl(), hx, hy, 16, status.equals("invisible") ? "offline" : status);
            g.text(g.clip(s.me.user().name(), w - 110), hx + 22, y + 7, Theme.TEXT);
            // status (click → menu)
            String label = Lang.tr(SocialService.statusLabelKey(status)) + (module.presence().autoAway() ? " · " + module.tr("status.auto") : "");
            int lw = g.width(label) + 14;
            boolean hov = ui.hover(hx + 20, y + 17, lw, 11);
            if (ui.clicked(hx + 20, y + 17, lw, 11)) statusMenu = !statusMenu;
            g.rect(hx + 22, y + 20, 4, 4, SocialUi.statusColor(status.equals("invisible") ? "offline" : status));
            g.text(label, hx + 29, y + 18, hov || statusMenu ? Theme.TEXT : Theme.MUTED);
            Widgets.arrow(g, hx + 29 + g.width(label) + 3, y + 18, true, Theme.DIM);
        } else {
            g.image(dev.henny.vibeyclient.Compat.id("icon.png"), hx, hy, 16, 16, 512);
            g.text(Lang.tr("social.name"), hx + 22, y + 13, Theme.TEXT);
        }
        // close
        int cx = x + w - 24;
        boolean ch = ui.hover(cx, y + 8, 18, 18);
        if (ui.clicked(cx, y + 8, 18, 18)) onClose();
        if (ch) g.rect(cx, y + 8, 18, 18, Theme.SURFACE3);
        Widgets.cross(g, cx + 5, y + 13, 8, ch ? Theme.TEXT : Theme.MUTED);
        // expand / collapse
        int ex = cx - 20;
        boolean eh = ui.hover(ex, y + 8, 18, 18);
        if (ui.clicked(ex, y + 8, 18, 18)) fullscreen = !fullscreen;
        if (eh) g.rect(ex, y + 8, 18, 18, Theme.SURFACE3);
        int ic = eh ? Theme.TEXT : Theme.MUTED;
        if (fullscreen) {
            g.outline(ex + 6, y + 14, 7, 7, ic);
        } else {
            g.outline(ex + 4, y + 12, 11, 10, ic);
            g.hline(ex + 4, y + 13, 11, ic);
        }
        g.hline(x, y + HEADER - 1, w, Theme.LINE);
    }

    private static final PresenceTracker.Status[] STATUSES = PresenceTracker.Status.values();

    private void handleStatusMenu(int x, int y) {
        for (int i = 0; i < STATUSES.length; i++) {
            if (ui.clicked(x, y + i * 16, 120, 16)) {
                module.setStatus(STATUSES[i]);
                statusMenu = false;
            }
        }
    }

    private void drawStatusMenu(Gfx g, int x, int y) {
        int h = STATUSES.length * 16;
        g.rect(x, y, 120, h, Theme.SURFACE2);
        g.outline(x - 1, y - 1, 122, h + 2, Theme.LINE2);
        for (int i = 0; i < STATUSES.length; i++) {
            String st = STATUSES[i].name().toLowerCase(Locale.ROOT);
            int ry = y + i * 16;
            boolean hov = ui.hover(x, ry, 120, 16);
            if (hov) g.rect(x, ry, 120, 16, Theme.SURFACE3);
            g.rect(x + 8, ry + 6, 4, 4, SocialUi.statusColor(st.equals("invisible") ? "offline" : st));
            boolean sel = module.status.get() == STATUSES[i];
            g.text(Lang.tr(SocialService.statusLabelKey(st)), x + 18, ry + 4, sel ? Theme.TEXT : Theme.MUTED);
            if (sel) g.rect(x + 112, ry + 6, 3, 3, Theme.BRAND);
        }
    }

    // ============================================================================================ tabs

    private void drawTabs(Gfx g, int x, int y, int w) {
        SocialState s = service.state;
        SidebarRoute.Tab[] tabs = SidebarRoute.Tab.values();
        int tw = w / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            SidebarRoute.Tab t = tabs[i];
            int tx = x + i * tw;
            int ww = i == tabs.length - 1 ? w - i * tw : tw;
            boolean sel = t == tab;
            if (ui.clicked(tx, y, ww, TABS)) navigate(SidebarRoute.tab(t));
            float hov = ui.anim(t, 0, 0f).to(sel || ui.hover(tx, y, ww, TABS) ? 1f : 0f);
            String label = module.tr("tab." + t.name().toLowerCase(Locale.ROOT));
            int count = switch (t) {
                case FRIENDS -> 0;
                case CHATS -> s.unreadChats();
                case REQUESTS -> s.incomingRequests();
                case INBOX -> s.notificationsUnread + s.openIncomingInvites().size();
            };
            String cs = count > 0 ? (count > 99 ? "99+" : Integer.toString(count)) : null;
            int bw = cs == null ? 0 : Math.max(10, g.width(cs) + 6) + 3;
            String clipped = g.clip(label, ww - 4 - bw);
            int lx = tx + (ww - g.width(clipped) - bw) / 2;
            g.text(clipped, lx, y + 7, Theme.mix(Theme.MUTED, Theme.TEXT, hov));
            if (cs != null) SocialUi.badge(g, count, lx + g.width(clipped) + bw, y + 6, t == SidebarRoute.Tab.REQUESTS ? Theme.BRAND : Theme.DANGER);
            float sa = ui.anim(t, 5, sel ? 1f : 0f).to(sel ? 1f : 0f);
            if (sa > 0f) {
                int uw = Math.round((ww - 16) * sa);
                g.rect(tx + (ww - uw) / 2, y + TABS - 2, uw, 2, Theme.BRAND);
            }
        }
        g.hline(x, y + TABS - 1, w, Theme.LINE);
    }

    // ============================================================================================ not connected

    private static final Object K_LOGIN = new Object();

    private void drawConnection(Gfx g, int x, int y, int w, int h, BackendClient b) {
        int cx = x + 14;
        int cy = y + 18;
        int cw = w - 28;
        String title;
        String text = null;
        boolean button = false;
        BackendClient.State st = b == null ? BackendClient.State.OFF : b.state();
        switch (st) {
            case OFF -> {
                title = module.tr("conn.off");
                text = module.tr("conn.offHint");
            }
            case LOGGED_OUT -> {
                title = module.tr("conn.loggedOut");
                text = b.errorCode() != null ? dev.henny.vibeyclient.module.social.Errors.text(b.errorCode(), b.errorMessage(), -1) : module.tr("conn.loggedOutHint");
                button = true;
            }
            case LOGGING_IN -> title = module.tr("conn.loggingIn");
            case CONNECTING, READY -> title = module.tr("conn.connecting");
            case RECONNECTING -> title = module.tr("conn.reconnecting");
            default -> {
                title = module.tr("conn.error");
                text = dev.henny.vibeyclient.module.social.Errors.text(b.errorCode(), b.errorMessage(), -1);
                button = !"banned".equals(b.errorCode());
            }
        }
        boolean busy = st == BackendClient.State.LOGGING_IN || st == BackendClient.State.CONNECTING || st == BackendClient.State.RECONNECTING
                || st == BackendClient.State.READY;
        g.text(g.clip(title, cw), cx, cy, Theme.TEXT);
        if (busy) {
            long t = System.currentTimeMillis() / 120;
            for (int i = 0; i < 3; i++) g.rect(cx + g.width(title) + 6 + i * 5, cy + 4, 3, 3, (t % 3) == i ? Theme.BRAND : Theme.SURFACE3);
        }
        cy += 14;
        if (text != null) {
            for (String line : dev.henny.vibeyclient.ui.toast.TextWrap.wrap(g, text, cw, 6)) {
                g.text(line, cx, cy, Theme.MUTED);
                cy += 10;
            }
            cy += 6;
        }
        if (button && Widgets.button(ui, K_LOGIN, cx, cy, Math.min(140, cw), 18, module.tr("conn.login"), Widgets.Style.PRIMARY)) {
            module.login();
        }
        if (st == BackendClient.State.OFF && Widgets.button(ui, K_LOGIN, cx, cy, Math.min(160, cw), 18, module.tr("conn.enableOnline"), Widgets.Style.PRIMARY)) {
            dev.henny.vibeyclient.api.GlobalSettings.onlineFeatures.set(true);
        }
    }

    // ============================================================================================ input

    @Override
    protected boolean click(double x, double y, int button) {
        if (closing) return true;
        boolean hit = UiHost.click(ui, x, y, button);
        return hit || !UiHost.anyInputFocused(ui);
    }

    @Override
    protected boolean release(double x, double y, int button) {
        UiHost.release(ui);
        return false;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        UiHost.scroll(ui, amount);
        return true;
    }

    @Override
    protected boolean key(int keyCode) {
        if (statusMenu && keyCode == Keys.ESCAPE) {
            statusMenu = false;
            return true;
        }
        if (!route.isTab() && route.kind() == SidebarRoute.Kind.CHAT && chat.key(keyCode)) return true;
        if (keyCode == Keys.ENTER) {
            if (tab == SidebarRoute.Tab.REQUESTS && route.isTab() && requests.enter()) return true;
            if (route.kind() == SidebarRoute.Kind.NEW_GROUP || route.kind() == SidebarRoute.Kind.GROUP_SETTINGS) return group.enter();
        }
        if (keyCode == Keys.ESCAPE) {
            if (!route.isTab()) {
                goBack();
                return true;
            }
            onClose();
            return true;
        }
        if (!UiHost.anyInputFocused(ui) && ClientCompat.matches(module.openKey.mapping(), keyCode)) {
            onClose();
            return true;
        }
        return false;
    }

    /** Focuses a text field of this screen (it then gets the keys). */
    void focus(net.minecraft.client.gui.components.EditBox box) {
        setFocused(box);
        box.setFocused(true);
    }

    @Override
    public void onClose() {
        if (closing) return;
        closing = true;
        statusMenu = false;
    }

    @Override
    public void removed() {
        super.removed();
        chat.onHide();
    }

    @Override
    public boolean isPauseScreen() {
        return parent != null && parent.isPauseScreen();
    }

    /** Opens the Vibey menu on the account tab (from the sidebar). */
    void openAccount() {
        ClientCompat.openScreen(new VibeyScreen(parent, "account"));
    }
}
