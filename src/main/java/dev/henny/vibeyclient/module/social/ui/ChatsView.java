package dev.henny.vibeyclient.module.social.ui;

import dev.henny.vibeyclient.module.social.SidebarRoute;
import dev.henny.vibeyclient.module.social.SocialState;
import dev.henny.vibeyclient.module.social.model.Model.Chat;
import dev.henny.vibeyclient.module.social.model.Model.Message;
import dev.henny.vibeyclient.module.social.model.Model.Presence;
import dev.henny.vibeyclient.module.social.model.Model.User;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;

/** Chats tab: DMs and groups, newest activity first, with preview and unread badge. */
final class ChatsView {
    private final SocialScreen s;
    private final ScrollArea scroll = new ScrollArea();
    private static final Object K_NEW = new Object();

    ChatsView(SocialScreen s) {
        this.s = s;
    }

    void draw(Ui ui, int x, int y, int w, int h) {
        Gfx g = ui.g;
        SocialState st = s.state();
        int barH = 28;
        int by = y + h - barH;
        g.hline(x, by, w, Theme.LINE);
        if (Widgets.button(ui, K_NEW, x + 8, by + 6, w - 16, 16, s.module.tr("chats.newGroup"), Widgets.Style.SECONDARY)) {
            s.navigate(SidebarRoute.newGroup());
        }
        int cy = scroll.begin(ui, x, y, w, h - barH) + 4;
        if (st.chats.isEmpty()) {
            cy += 24;
            SocialUi.empty(g, s.module.tr("chats.none"), x, cy, w);
            cy += 12;
            SocialUi.empty(g, s.module.tr("chats.noneHint"), x, cy, w);
            cy += 20;
        }
        String selected = s.route().kind() == SidebarRoute.Kind.CHAT ? s.route().id() : null;
        for (Chat c : st.sortedChats()) {
            if (ui.visibleY(cy, 30)) cy = row(ui, c, x, cy, w, c.id().equals(selected));
            else cy += 30;
        }
        cy += 4;
        scroll.end(ui, cy);
    }

    private int row(Ui ui, Chat c, int x, int y, int w, boolean selected) {
        Gfx g = ui.g;
        int h = 30;
        SocialState st = s.state();
        if (SocialUi.row(ui, c.id(), x, y, w, h, selected)) s.navigate(SidebarRoute.chat(c.id()));
        String self = st.selfId();
        if (c.dm()) {
            User o = c.other(self);
            Presence p = o == null ? null : st.presenceOf(o.id());
            SocialUi.head(g, o == null ? null : o.id(), o == null ? null : o.skinUrl(), x + 10, y + 6, 18, p == null ? "offline" : p.status());
        } else {
            Widgets.monogram(g, s.service.chatName(c), x + 10, y + 6, 18, Theme.BRAND);
        }
        int right = x + w - 8;
        Message last = c.lastMessage();
        String when = last == null ? "" : SocialUi.time(last.createdAt());
        g.textRight(when, right, y + 5, Theme.DIM);
        int tx = x + 34;
        int nameW = right - tx - g.width(when) - 6;
        g.text(g.clip(s.service.chatName(c), nameW), tx, y + 5, c.unreadCount() > 0 ? Theme.TEXT : Theme.mix(Theme.MUTED, Theme.TEXT, 0.6f));
        int badgeW = SocialUi.badge(g, c.unreadCount(), right, y + 16, Theme.DANGER);
        String preview = preview(c, last, self);
        if (!st.typingIn(c.id()).isEmpty()) preview = s.module.tr("chat.typingShort");
        g.text(g.clip(preview, right - tx - badgeW - 6), tx, y + 17, c.unreadCount() > 0 ? Theme.MUTED : Theme.DIM);
        return y + h;
    }

    private String preview(Chat c, Message last, String self) {
        if (last == null) return c.dm() ? s.module.tr("chats.noMessages") : s.module.tr("chats.members", c.members().size());
        String text;
        if (last.deleted()) text = s.module.tr("chat.deleted");
        else if ("invite".equals(last.kind()) && last.invite() != null) text = s.module.tr("chat.inviteCard", last.invite().title());
        else if ("system".equals(last.kind())) text = ChatView.systemText(s, last);
        else text = last.content().replace('\n', ' ');
        if ("system".equals(last.kind())) return text;
        if (last.authorId() != null && last.authorId().equals(self)) return s.module.tr("chats.you", text);
        if (!c.dm()) return s.service.state.nameOf(last.authorId()) + ": " + text;
        return text;
    }
}
