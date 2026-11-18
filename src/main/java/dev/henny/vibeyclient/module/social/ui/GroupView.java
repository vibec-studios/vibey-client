package dev.henny.vibeyclient.module.social.ui;

import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.module.social.SidebarRoute;
import dev.henny.vibeyclient.module.social.SocialState;
import dev.henny.vibeyclient.module.social.model.Model.Chat;
import dev.henny.vibeyclient.module.social.model.Model.ChatMember;
import dev.henny.vibeyclient.module.social.model.Model.Friend;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** New group (name + friends) and group settings (rename, members, add friends, leave). */
final class GroupView {
    private final SocialScreen s;
    private final ScrollArea scroll = new ScrollArea();
    private String chatId; // null = new group
    private String name = "";
    private final Set<String> picked = new LinkedHashSet<>();
    private boolean busy;
    private static final Object K_NAME = new Object(), K_SAVE = new Object(), K_CREATE = new Object(), K_ADD = new Object(),
            K_LEAVE = new Object(), K_BACK = new Object();

    GroupView(SocialScreen s) {
        this.s = s;
    }

    void open(String id) {
        chatId = id;
        picked.clear();
        busy = false;
        scroll.reset();
        Chat c = id == null ? null : s.state().chats.get(id);
        name = c == null || c.name() == null ? "" : c.name();
    }

    boolean enter() {
        if (chatId == null) create();
        else rename();
        return true;
    }

    private void create() {
        if (name.isBlank() || busy) return;
        busy = true;
        s.service.createGroup(name, new ArrayList<>(picked)).whenComplete((c, t) -> Tasks.onMain(() -> {
            busy = false;
            if (c != null) s.show(SidebarRoute.chat(c.id()));
        }));
    }

    private void rename() {
        Chat c = s.state().chats.get(chatId);
        if (c == null || name.isBlank() || name.equals(c.name())) return;
        s.service.renameGroup(chatId, name);
    }

    void draw(Ui ui, int x, int y, int w, int h, boolean showBack) {
        Gfx g = ui.g;
        SocialState st = s.state();
        Chat c = chatId == null ? null : st.chats.get(chatId);
        boolean create = chatId == null;
        if (!create && c == null) {
            if (SocialUi.backHeader(ui, K_BACK, x, y, w, s.module.tr("chat.missing"), showBack)) s.goBack();
            return;
        }
        if (SocialUi.backHeader(ui, K_BACK, x, y, w, create ? s.module.tr("group.new") : s.module.tr("group.settings"), showBack)) s.goBack();
        boolean owner = c != null && st.selfId() != null && st.selfId().equals(c.ownerId());
        int cy = scroll.begin(ui, x, y + 26, w, h - 26) + 10;
        int cx = x + 12;
        int cw = w - 24;
        Widgets.caption(g, s.module.tr("group.name"), cx, cy);
        cy += 12;
        String action = create ? s.module.tr("group.create") : s.module.tr("action.save");
        int bw = SocialUi.smallButtonWidth(g, action) + 6;
        SocialUi.field(ui, K_NAME, cx, cy, cw - bw - 4, name, 60, s.module.tr("group.namePlaceholder"), v -> name = v);
        boolean can = !name.isBlank() && !busy && (create || (owner && c != null && !name.equals(c.name())));
        if (Widgets.button(ui, create ? K_CREATE : K_SAVE, cx + cw - bw, cy, bw, 16, action, can ? Widgets.Style.PRIMARY : Widgets.Style.DISABLED)) {
            if (create) create();
            else rename();
        }
        cy += 24;

        if (c != null) {
            cy = SocialUi.caption(g, s.module.tr("group.members", c.members().size()), cx, cy, cw);
            for (ChatMember m : c.members()) {
                int right = x + w - 8;
                boolean self = m.user().id().equals(st.selfId());
                if (owner && !self) {
                    right = SocialUi.smallButtonRight(ui, m.user().id(), right, cy + 5, s.module.tr("action.remove"), Widgets.Style.GHOST,
                            () -> s.service.removeMember(c.id(), m.user().id()));
                }
                SocialUi.head(g, m.user().id(), m.user().skinUrl(), x + 12, cy + 5, 14, null);
                g.text(g.clip(m.user().name() + (self ? " " + s.module.tr("group.you") : ""), right - x - 36), x + 32, cy + 8, Theme.TEXT);
                if ("owner".equals(m.role())) Widgets.chip(g, s.module.tr("group.owner"), right - g.width(s.module.tr("group.owner")) - 8, cy + 6, Theme.BRAND);
                cy += 24;
            }
            cy += 8;
        }

        // friends to add
        List<Friend> candidates = new ArrayList<>();
        for (Friend f : st.sortedFriends()) if (c == null || c.member(f.user().id()) == null) candidates.add(f);
        cy = SocialUi.caption(g, create ? s.module.tr("group.pick") : s.module.tr("group.add"), cx, cy, cw);
        if (candidates.isEmpty()) {
            g.text(s.module.tr("group.noCandidates"), cx, cy + 2, Theme.DIM);
            cy += 16;
        }
        for (Friend f : candidates) {
            String id = f.user().id();
            boolean on = picked.contains(id);
            int tx = x + w - 12 - Widgets.TOGGLE_W;
            if (Widgets.toggle(ui, id, tx, cy + 6, on, true) | SocialUi.row(ui, (id + "#pick").intern(), x, cy, w, 24, false)) {
                if (on) picked.remove(id);
                else picked.add(id);
            }
            SocialUi.head(g, id, f.user().skinUrl(), x + 12, cy + 5, 14, f.presence().status());
            g.text(g.clip(f.user().name(), tx - x - 40), x + 32, cy + 8, on ? Theme.TEXT : Theme.MUTED);
            cy += 24;
        }
        if (!create && !candidates.isEmpty()) {
            cy += 4;
            if (Widgets.button(ui, K_ADD, cx, cy, cw, 16, s.module.tr("group.addPicked", picked.size()),
                    picked.isEmpty() ? Widgets.Style.DISABLED : Widgets.Style.PRIMARY)) {
                List<String> ids = new ArrayList<>(picked);
                picked.clear();
                s.service.addMembers(c.id(), ids);
            }
            cy += 22;
        }
        if (c != null) {
            cy += 10;
            if (Widgets.button(ui, K_LEAVE, cx, cy, cw, 16, s.module.tr("group.leave"), Widgets.Style.DANGER)) {
                String n = s.service.chatName(c);
                ConfirmScreen.ask(s.module.tr("group.leaveTitle", n), s.module.tr("group.leaveText"), s.module.tr("group.leave"), () -> {
                    s.service.removeMember(c.id(), st.selfId());
                    s.show(SidebarRoute.tab(SidebarRoute.Tab.CHATS));
                });
            }
            cy += 22;
        }
        cy += 6;
        scroll.end(ui, cy);
    }
}
