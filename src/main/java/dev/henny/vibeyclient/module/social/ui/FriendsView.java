package dev.henny.vibeyclient.module.social.ui;

import dev.henny.vibeyclient.module.social.SidebarRoute;
import dev.henny.vibeyclient.module.social.SocialState;
import dev.henny.vibeyclient.module.social.model.Model.Chat;
import dev.henny.vibeyclient.module.social.model.Model.Friend;
import dev.henny.vibeyclient.module.social.model.Model.HostSession;
import dev.henny.vibeyclient.module.social.model.Model.Join;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;

import java.util.List;

/** Friends tab: joinable worlds of friends, then friends grouped online / offline, and the add/invite bar. */
final class FriendsView {
    private final SocialScreen s;
    private final ScrollArea scroll = new ScrollArea();
    private static final Object K_ADD = new Object();
    private static final Object K_INVITE = new Object();

    FriendsView(SocialScreen s) {
        this.s = s;
    }

    void draw(Ui ui, int x, int y, int w, int h) {
        Gfx g = ui.g;
        SocialState st = s.state();
        int barH = 28;
        // bottom bar
        int by = y + h - barH;
        g.hline(x, by, w, Theme.LINE);
        int half = (w - 24) / 2;
        if (Widgets.button(ui, K_ADD, x + 8, by + 6, half, 16, s.module.tr("friends.add"), Widgets.Style.PRIMARY)) {
            s.navigate(SidebarRoute.tab(SidebarRoute.Tab.REQUESTS));
            s.requests.focusSearch();
        }
        if (Widgets.button(ui, K_INVITE, x + 16 + half, by + 6, w - 24 - half, 16, s.module.tr("friends.inviteWorld"), Widgets.Style.SECONDARY)) {
            s.navigate(SidebarRoute.invite(null));
        }

        int cy = scroll.begin(ui, x, y, w, h - barH) + 6;
        List<Friend> friends = st.sortedFriends();
        // joinable worlds (relay sessions of friends, or joinable presence)
        boolean header = false;
        for (HostSession hs : st.friendHosts.values()) {
            if (!header) {
                cy = SocialUi.caption(g, s.module.tr("friends.worlds"), x + 10, cy, w);
                header = true;
            }
            cy = hostRow(ui, hs, x, cy, w);
        }
        for (Friend f : friends) {
            var a = f.presence().activity();
            if (a == null || !a.joinable() || a.join() == null || a.join().address() == null || hasSessionOf(st, f)) continue;
            if (!header) {
                cy = SocialUi.caption(g, s.module.tr("friends.worlds"), x + 10, cy, w);
                header = true;
            }
            cy = joinRow(ui, f, a.world(), a.join(), x, cy, w);
        }
        if (header) cy += 6;

        if (friends.isEmpty()) {
            cy += 24;
            SocialUi.empty(g, s.module.tr("friends.none"), x, cy, w);
            cy += 12;
            SocialUi.empty(g, s.module.tr("friends.noneHint"), x, cy, w);
            cy += 20;
        } else {
            int online = st.onlineCount();
            cy = SocialUi.caption(g, s.module.tr("friends.online", online), x + 10, cy, w);
            boolean offlineHeader = false;
            for (Friend f : friends) {
                if (!f.presence().online() && !offlineHeader) {
                    offlineHeader = true;
                    cy += 6;
                    cy = SocialUi.caption(g, s.module.tr("friends.offline", friends.size() - online), x + 10, cy, w);
                }
                if (ui.visibleY(cy, SocialUi.ROW)) cy = friendRow(ui, f, x, cy, w);
                else cy += SocialUi.ROW;
            }
        }
        cy += 6;
        scroll.end(ui, cy);
    }

    private static boolean hasSessionOf(SocialState st, Friend f) {
        for (HostSession hs : st.friendHosts.values()) if (hs.host() != null && hs.host().id().equals(f.user().id())) return true;
        return false;
    }

    private int friendRow(Ui ui, Friend f, int x, int y, int w) {
        Gfx g = ui.g;
        int h = SocialUi.ROW;
        SocialState st = s.state();
        boolean hov = ui.hover(x, y, w, h);
        Chat dm = st.dmWith(f.user().id());
        int unread = dm == null ? 0 : dm.unreadCount();
        int right = x + w - 8;
        if (hov) {
            right = SocialUi.smallButtonRight(ui, f.user().id(), right, y + 6, s.module.tr("action.chat"), Widgets.Style.SECONDARY,
                    () -> s.service.openDm(f.user().id()).thenAccept(c -> {
                        if (c != null) s.navigate(SidebarRoute.chat(c.id()));
                    }));
        } else if (unread > 0) {
            right -= SocialUi.badge(g, unread, right, y + 8, Theme.DANGER) + 4;
        }
        if (SocialUi.row(ui, f.user().id(), x, y, w, h, false)) s.navigate(SidebarRoute.profile(f.user().id()));
        boolean online = f.presence().online();
        SocialUi.head(g, f.user().id(), f.user().skinUrl(), x + 10, y + 5, 16, f.presence().status());
        int tx = x + 32;
        int tw = right - tx - 4;
        g.text(g.clip(f.user().name(), tw), tx, y + 4, online ? Theme.TEXT : Theme.MUTED);
        String sub = online ? s.service.activityText(f.presence()) : f.user().statusText();
        if (sub == null && !online) sub = s.module.tr("status.offline");
        if (sub != null) g.text(g.clip(sub, tw), tx, y + 14, Theme.DIM);
        return y + h;
    }

    private int hostRow(Ui ui, HostSession hs, int x, int y, int w) {
        Gfx g = ui.g;
        int h = SocialUi.ROW;
        int right = SocialUi.smallButtonRight(ui, hs.id(), x + w - 8, y + 6, s.module.tr("action.join"), Widgets.Style.PRIMARY,
                () -> s.module.invites().join(hs.host() == null ? null : hs.host().name(), hs.address(), 25565));
        SocialUi.row(ui, hs.id(), x, y, w, h, false);
        String hostId = hs.host() == null ? null : hs.host().id();
        SocialUi.head(g, hostId, hs.host() == null ? null : hs.host().skinUrl(), x + 10, y + 5, 16, null);
        g.rect(x + 23, y + 18, 5, 5, Theme.BRAND2);
        int tx = x + 32;
        int tw = right - tx - 4;
        String name = hs.host() == null ? "?" : hs.host().name();
        g.text(g.clip(s.module.tr("friends.worldOf", name), tw), tx, y + 4, Theme.TEXT);
        String sub = s.module.tr("friends.players", hs.players()) + (hs.mcVersion() != null ? " · " + hs.mcVersion() : "");
        g.text(g.clip(sub, tw), tx, y + 14, Theme.DIM);
        return y + h;
    }

    private int joinRow(Ui ui, Friend f, String world, Join join, int x, int y, int w) {
        Gfx g = ui.g;
        int h = SocialUi.ROW;
        int right = SocialUi.smallButtonRight(ui, join.address().intern(), x + w - 8, y + 6, s.module.tr("action.join"), Widgets.Style.PRIMARY,
                () -> s.module.invites().join(world, join.address(), join.port()));
        SocialUi.row(ui, join.address().intern(), x, y, w, h, false);
        SocialUi.head(g, f.user().id(), f.user().skinUrl(), x + 10, y + 5, 16, null);
        g.rect(x + 23, y + 18, 5, 5, Theme.BRAND2);
        int tx = x + 32;
        int tw = right - tx - 4;
        g.text(g.clip(world == null ? s.module.tr("friends.worldOf", f.user().name()) : world, tw), tx, y + 4, Theme.TEXT);
        g.text(g.clip(f.user().name(), tw), tx, y + 14, Theme.DIM);
        return y + h;
    }
}
