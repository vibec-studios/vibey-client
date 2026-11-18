package dev.henny.vibeyclient.module.social.ui;

import dev.henny.vibeyclient.module.social.InviteFlow;
import dev.henny.vibeyclient.module.social.SidebarRoute;
import dev.henny.vibeyclient.module.social.SocialState;
import dev.henny.vibeyclient.module.social.model.Model.Friend;
import dev.henny.vibeyclient.module.social.model.Model.FriendRequest;
import dev.henny.vibeyclient.module.social.model.Model.Presence;
import dev.henny.vibeyclient.module.social.model.Model.User;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;
import dev.henny.vibeyclient.ui.toast.TextWrap;

import java.util.List;

/** A player's profile with the actions that fit the relationship (chat, invite, join, remove, block …). */
final class ProfileView {
    private final SocialScreen s;
    private final ScrollArea scroll = new ScrollArea();
    private String userId;
    private User loaded;
    private static final Object K_CHAT = new Object(), K_INVITE = new Object(), K_JOIN = new Object(), K_REMOVE = new Object(),
            K_BLOCK = new Object(), K_ADD = new Object(), K_UNBLOCK = new Object(), K_ACCEPT = new Object(), K_BACK = new Object();

    ProfileView(SocialScreen s) {
        this.s = s;
    }

    void open(String id) {
        userId = id;
        loaded = null;
        scroll.reset();
        if (s.state().user(id) == null) {
            s.service.profile(id).thenAccept(u -> loaded = u);
        }
    }

    void draw(Ui ui, int x, int y, int w, int h, boolean showBack) {
        Gfx g = ui.g;
        SocialState st = s.state();
        User u = st.user(userId);
        if (u == null) u = loaded;
        if (SocialUi.backHeader(ui, K_BACK, x, y, w, s.module.tr("profile.title"), showBack)) s.goBack();
        if (u == null) {
            SocialUi.empty(g, s.module.tr("chat.loading"), x, y + 50, w);
            return;
        }
        Friend f = st.friends.get(u.id());
        Presence p = f == null ? null : f.presence();
        int cy = scroll.begin(ui, x, y + 26, w, h - 26) + 12;
        int cx = x + 14;
        int cw = w - 28;
        SocialUi.head(g, u.id(), u.skinUrl(), cx, cy, 32, p == null ? null : p.status());
        g.outline(cx - 1, cy - 1, 34, 34, Theme.LINE2);
        int nx = cx + 42;
        g.textScaled(g.clip(u.name(), (cw - 42) / 2 + 20), nx, cy + 1, 2, Theme.TEXT, false);
        int ly = cy + 21;
        if (!"user".equals(u.badge())) {
            int col = "staff".equals(u.badge()) ? Theme.BRAND : Theme.BRAND2;
            Widgets.chip(g, s.module.tr("badge." + u.badge()), nx, ly, col);
        } else if (p != null) {
            g.rect(nx, ly + 3, 5, 5, SocialUi.statusColor(p.status()));
            g.text(s.module.tr("status." + p.status()), nx + 9, ly + 1, Theme.MUTED);
        }
        cy += 42;
        if (u.statusText() != null && !u.statusText().isBlank()) {
            for (String l : TextWrap.wrap(g, "„" + u.statusText() + "“", cw, 3)) {
                g.text(l, cx, cy, Theme.MUTED);
                cy += 10;
            }
            cy += 6;
        }
        if (p != null && p.online()) {
            String act = s.service.activityText(p);
            if (act != null) {
                Widgets.caption(g, s.module.tr("profile.activity"), cx, cy);
                cy += 12;
                g.text(g.clip(act, cw), cx, cy, Theme.TEXT);
                cy += 14;
            }
        }
        if (f != null) {
            g.text(s.module.tr("profile.since", SocialUi.date(f.since())), cx, cy, Theme.DIM);
            cy += 16;
        }
        SocialUi.separator(g, cx, cy, cw);
        cy += 10;

        // actions
        String id = u.id();
        boolean self = id.equals(st.selfId());
        boolean blocked = st.blocked.containsKey(id);
        FriendRequest incoming = null, outgoing = null;
        for (FriendRequest r : st.requests.values()) {
            if (r.other() != null && r.other().id().equals(id)) {
                if (r.incoming()) incoming = r;
                else outgoing = r;
            }
        }
        int bh = 18;
        if (self) {
            SocialUi.empty(g, s.module.tr("profile.self"), x, cy, w);
            cy += 14;
        } else if (blocked) {
            if (Widgets.button(ui, K_UNBLOCK, cx, cy, cw, bh, s.module.tr("action.unblock"), Widgets.Style.SECONDARY)) s.service.unblock(id);
            cy += bh + 6;
        } else if (f != null) {
            if (Widgets.button(ui, K_CHAT, cx, cy, cw, bh, s.module.tr("action.message"), Widgets.Style.PRIMARY)) {
                s.service.openDm(id).thenAccept(c -> {
                    if (c != null) s.navigate(SidebarRoute.chat(c.id()));
                });
            }
            cy += bh + 4;
            var a = p == null ? null : p.activity();
            if (a != null && a.joinable() && a.join() != null && a.join().address() != null) {
                if (Widgets.button(ui, K_JOIN, cx, cy, cw, bh, s.module.tr("profile.joinWorld", a.world() == null ? u.name() : a.world()), Widgets.Style.SECONDARY)) {
                    s.module.invites().join(a.world(), a.join().address(), a.join().port());
                }
                cy += bh + 4;
            }
            InviteFlow.Availability av = s.module.invites().availability();
            boolean canInvite = av == InviteFlow.Availability.OK || av == InviteFlow.Availability.START_HOSTING;
            String inviteLabel = av == InviteFlow.Availability.START_HOSTING ? s.module.tr("profile.hostAndInvite") : s.module.tr("profile.invite");
            String noInvite = av == InviteFlow.Availability.NO_MODULE ? s.module.tr("profile.inviteNoModule") : s.module.tr("profile.inviteNoWorld");
            if (Widgets.button(ui, K_INVITE, cx, cy, cw, bh, canInvite ? inviteLabel : noInvite,
                    canInvite ? Widgets.Style.SECONDARY : Widgets.Style.DISABLED)) {
                s.module.invites().inviteToMyWorld(List.of(id));
            }
            cy += bh + 12;
            int half = (cw - 4) / 2;
            String name = u.name();
            if (Widgets.button(ui, K_REMOVE, cx, cy, half, bh, s.module.tr("action.remove"), Widgets.Style.DANGER)) {
                ConfirmScreen.ask(s.module.tr("profile.removeTitle", name), s.module.tr("profile.removeText", name),
                        s.module.tr("action.remove"), () -> s.service.removeFriend(id));
            }
            if (Widgets.button(ui, K_BLOCK, cx + half + 4, cy, cw - half - 4, bh, s.module.tr("action.block"), Widgets.Style.DANGER)) {
                confirmBlock(id, name);
            }
            cy += bh + 6;
        } else {
            if (incoming != null) {
                FriendRequest r = incoming;
                if (Widgets.button(ui, K_ACCEPT, cx, cy, cw, bh, s.module.tr("action.accept"), Widgets.Style.PRIMARY)) s.service.acceptRequest(r.id());
            } else if (outgoing != null) {
                Widgets.button(ui, K_ADD, cx, cy, cw, bh, s.module.tr("profile.pending"), Widgets.Style.DISABLED);
            } else if (Widgets.button(ui, K_ADD, cx, cy, cw, bh, s.module.tr("action.addFriend"), Widgets.Style.PRIMARY)) {
                s.service.sendFriendRequest(null, id);
            }
            cy += bh + 4;
            String name = u.name();
            if (Widgets.button(ui, K_BLOCK, cx, cy, cw, bh, s.module.tr("action.block"), Widgets.Style.DANGER)) confirmBlock(id, name);
            cy += bh + 6;
        }
        cy += 6;
        scroll.end(ui, cy);
    }

    private void confirmBlock(String id, String name) {
        ConfirmScreen.ask(s.module.tr("profile.blockTitle", name), s.module.tr("profile.blockText", name),
                s.module.tr("action.block"), () -> s.service.block(id));
    }
}
