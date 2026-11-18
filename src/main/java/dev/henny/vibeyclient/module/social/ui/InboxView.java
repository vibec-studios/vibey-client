package dev.henny.vibeyclient.module.social.ui;

import dev.henny.vibeyclient.backend.J;
import dev.henny.vibeyclient.module.social.SidebarRoute;
import dev.henny.vibeyclient.module.social.SocialState;
import dev.henny.vibeyclient.module.social.model.Model.Invite;
import dev.henny.vibeyclient.module.social.model.Model.Notification;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;
import dev.henny.vibeyclient.ui.toast.TextWrap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Inbox tab: open world invites (join/decline) and the backend's notification inbox. */
final class InboxView {
    private final SocialScreen s;
    private final ScrollArea scroll = new ScrollArea();
    private static final Object K_READ = new Object();
    private static final Object K_MORE = new Object();
    private final Map<String, List<String>> wrapped = new HashMap<>();
    private int wrappedW = -1;

    InboxView(SocialScreen s) {
        this.s = s;
    }

    void draw(Ui ui, int x, int y, int w, int h) {
        Gfx g = ui.g;
        SocialState st = s.state();
        if (wrappedW != w) {
            wrappedW = w;
            wrapped.clear();
        }
        int cy = scroll.begin(ui, x, y, w, h) + 6;
        List<Invite> invites = st.openIncomingInvites();
        if (!invites.isEmpty()) {
            cy = SocialUi.caption(g, s.module.tr("inbox.invites", invites.size()), x + 10, cy, w);
            for (Invite i : invites) cy = inviteCard(ui, i, x + 8, cy, w - 16) + 6;
            cy += 4;
        }
        int right = x + w - 8;
        if (st.notificationsUnread > 0) {
            SocialUi.smallButtonRight(ui, K_READ, right, cy - 3, s.module.tr("inbox.markAll"), Widgets.Style.GHOST,
                    s.service::markAllNotificationsRead);
        }
        cy = SocialUi.caption(g, s.module.tr("inbox.notifications", st.notificationsUnread), x + 10, cy, w) + 2;
        if (st.notifications.isEmpty()) {
            cy += 16;
            SocialUi.empty(g, s.module.tr("inbox.none"), x, cy, w);
            cy += 20;
        }
        for (Notification n : st.notifications) {
            if (ui.visibleY(cy, 60)) cy = notificationRow(ui, n, x, cy, w);
            else cy += 12 + lines(g, n, w).size() * 10 + 10;
        }
        if (st.notificationsHasMore) {
            cy += 4;
            if (Widgets.button(ui, K_MORE, x + 8, cy, w - 16, 14, s.module.tr("inbox.more"), Widgets.Style.GHOST)) s.service.loadNotifications(true);
            cy += 18;
        }
        cy += 6;
        scroll.end(ui, cy);
    }

    private List<String> lines(Gfx g, Notification n, int w) {
        return wrapped.computeIfAbsent(n.id(), k -> TextWrap.wrap(g, n.body() == null ? "" : n.body(), w - 44, 3));
    }

    private int notificationRow(Ui ui, Notification n, int x, int y, int w) {
        Gfx g = ui.g;
        List<String> body = lines(g, n, w);
        int h = 14 + (n.body() == null ? 0 : body.size() * 10) + 8;
        boolean hov = ui.hover(x, y, w, h);
        int right = x + w - 8;
        if (hov) {
            boolean xh = ui.hover(right - 12, y + 3, 12, 12);
            if (ui.clicked(right - 12, y + 3, 12, 12)) s.service.deleteNotification(n);
            Widgets.cross(g, right - 9, y + 6, 6, xh ? Theme.DANGER : Theme.DIM);
        }
        if (SocialUi.row(ui, n.id(), x, y, w, h, false)) {
            s.service.markNotificationRead(n);
            JsonTarget t = target(n);
            if (t != null) s.navigate(t.route);
        }
        if (n.unread()) g.rect(x + 10, y + 7, 4, 4, Theme.BRAND);
        int tx = x + 20;
        String when = SocialUi.ago(n.createdAt());
        g.textRight(when, hov ? right - 16 : right, y + 4, Theme.DIM);
        g.text(g.clip(s.service.notificationTitle(n), right - tx - g.width(when) - 20), tx, y + 4, n.unread() ? Theme.TEXT : Theme.MUTED);
        int ly = y + 15;
        if (n.body() != null) {
            for (String l : body) {
                g.text(l, tx, ly, Theme.DIM);
                ly += 10;
            }
        }
        SocialUi.separator(g, x + 10, y + h - 1, w - 20);
        return y + h;
    }

    private record JsonTarget(SidebarRoute route) {}

    private JsonTarget target(Notification n) {
        String chat = J.str(n.data(), "chatId");
        if (chat != null && s.state().chats.containsKey(chat)) return new JsonTarget(SidebarRoute.chat(chat));
        if (n.type().startsWith("friend_request")) return new JsonTarget(SidebarRoute.tab(SidebarRoute.Tab.REQUESTS));
        String user = J.str(n.data(), "userId");
        if (user == null) user = J.str(n.data(), "fromId");
        if (user != null && s.state().friends.containsKey(user)) return new JsonTarget(SidebarRoute.profile(user));
        return null;
    }

    /** Invite card (shared with the chat view). Returns the bottom y. */
    int inviteCard(Ui ui, Invite i, int x, int y, int w) {
        Gfx g = ui.g;
        int h = 46;
        g.rect(x, y, w, h, Theme.alpha(Theme.BRAND, 0.08f));
        g.outline(x, y, w, h, Theme.alpha(Theme.BRAND, 0.45f));
        g.rect(x, y, 2, h, Theme.BRAND);
        String from = i.from() == null ? "?" : i.from().name();
        SocialUi.head(g, i.from() == null ? null : i.from().id(), i.from() == null ? null : i.from().skinUrl(), x + 8, y + 7, 14, null);
        g.text(g.clip(s.module.tr("inbox.inviteFrom", from), w - 34), x + 28, y + 6, Theme.TEXT);
        String sub = i.targetName() + (i.mcVersion() != null ? " · " + i.mcVersion() : "");
        g.text(g.clip(sub, w - 34), x + 28, y + 16, Theme.MUTED);
        int by = y + h - 17;
        int bx = x + 28;
        String join = s.module.tr("action.join");
        if (SocialUi.smallButton(ui, i.id(), bx, by, join, Widgets.Style.PRIMARY)) s.service.acceptInvite(i.id());
        bx += SocialUi.smallButtonWidth(g, join) + 4;
        String decline = s.module.tr("action.decline");
        if (SocialUi.smallButton(ui, (i.id() + "#d").intern(), bx, by, decline, Widgets.Style.GHOST)) s.service.declineInvite(i.id());
        long left = Math.max(0, i.expiresAt() - System.currentTimeMillis()) / 60_000;
        g.textRight(s.module.tr("inbox.expires", left), x + w - 6, by + 3, Theme.DIM);
        return y + h;
    }
}
