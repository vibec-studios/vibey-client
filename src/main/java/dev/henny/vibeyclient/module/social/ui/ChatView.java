package dev.henny.vibeyclient.module.social.ui;

import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.social.InviteFlow;
import dev.henny.vibeyclient.module.social.SidebarRoute;
import dev.henny.vibeyclient.module.social.SocialState;
import dev.henny.vibeyclient.module.social.model.Model.Chat;
import dev.henny.vibeyclient.module.social.model.Model.ChatMember;
import dev.henny.vibeyclient.module.social.model.Model.Invite;
import dev.henny.vibeyclient.module.social.model.Model.Message;
import dev.henny.vibeyclient.module.social.model.Model.Presence;
import dev.henny.vibeyclient.module.social.model.Model.User;
import dev.henny.vibeyclient.ui.Anim;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;
import dev.henny.vibeyclient.ui.toast.TextWrap;
import net.minecraft.client.gui.components.EditBox;

import java.util.ArrayList;
import java.util.List;

/**
 * One chat: messages (bottom-anchored, older pages load when scrolled to the top), typing indicator, read
 * receipts, invite cards, edit/delete of own messages and the input line (Enter sends, ↑ edits the last message).
 */
final class ChatView {
    private static final int HEAD = 34;
    private static final int INPUT = 30;
    private static final long GROUP_GAP_MS = 5 * 60_000;

    private final SocialScreen s;
    private String chatId;
    private String input = "";
    private String editingId;
    private boolean wantFocus;
    private EditBox box;
    private final Object inputKey = new Object();
    private static final Object K_SETTINGS = new Object();
    private static final Object K_INVITE = new Object();

    // scrolling (distance from the bottom)
    private float scrollTarget;
    private final Anim smooth = new Anim(0f, 120);
    private int contentH;
    private String newestId;

    // layout cache
    private record Block(Message m, boolean header, List<String> lines, int h) {}

    private final List<Block> blocks = new ArrayList<>();
    private final java.util.Map<String, List<String>> wrapCache = new java.util.HashMap<>();
    private int layoutRev = -1;
    private int layoutW = -1;
    private String layoutChat;

    ChatView(SocialScreen s) {
        this.s = s;
    }

    void open(String id) {
        if (!id.equals(chatId)) {
            chatId = id;
            input = "";
            editingId = null;
            scrollTarget = 0;
            smooth.snap(0);
            newestId = null;
            layoutRev = -1;
        }
        wantFocus = true;
        s.service.loadHistory(id, false);
    }

    void onHide() {
        if (box != null) box.setFocused(false);
    }

    // ============================================================================================ draw

    void draw(Ui ui, int x, int y, int w, int h, boolean showBack) {
        Gfx g = ui.g;
        SocialState st = s.state();
        Chat c = chatId == null ? null : st.chats.get(chatId);
        if (c == null) {
            if (SocialUi.backHeader(ui, this, x, y, w, s.module.tr("chat.missing"), showBack)) s.goBack();
            return;
        }
        s.service.markVisible(c.id());
        drawHeader(ui, c, x, y, w, showBack);
        int top = y + HEAD;
        int bottom = y + h - INPUT - (editingId != null ? 12 : 0);
        List<String> typing = st.typingIn(c.id());
        int typingH = typing.isEmpty() ? 0 : 11;
        drawMessages(ui, c, x, top, w, bottom - top - typingH);
        if (!typing.isEmpty()) {
            String who = typing.size() == 1 ? s.module.tr("chat.typing", st.nameOf(typing.get(0))) : s.module.tr("chat.typingMany", typing.size());
            long t = System.currentTimeMillis() / 300;
            g.text(g.clip(who + ".".repeat((int) (t % 3) + 1), w - 20), x + 10, bottom - 10, Theme.DIM);
        }
        drawInput(ui, c, x, bottom, w, y + h - bottom);
    }

    private void drawHeader(Ui ui, Chat c, int x, int y, int w, boolean showBack) {
        Gfx g = ui.g;
        SocialState st = s.state();
        int tx = x + 10;
        if (showBack) {
            boolean hb = ui.hover(x + 4, y + 8, 18, 18);
            if (ui.clicked(x + 4, y + 8, 18, 18)) s.goBack();
            if (hb) g.rect(x + 4, y + 8, 18, 18, Theme.SURFACE3);
            Widgets.arrow(g, x + 10, y + 13, false, hb ? Theme.TEXT : Theme.MUTED);
            tx = x + 26;
        }
        int right = x + w - 8;
        String sub;
        if (c.dm()) {
            User o = c.other(st.selfId());
            Presence p = o == null ? null : st.presenceOf(o.id());
            SocialUi.head(g, o == null ? null : o.id(), o == null ? null : o.skinUrl(), tx, y + 8, 18, p == null ? "offline" : p.status());
            sub = p == null ? s.module.tr("status.offline") : p.online() ? orStatus(s.service.activityText(p), p.status()) : s.module.tr("status.offline");
            InviteFlow.Availability av = s.module.invites().availability();
            if (o != null && st.friends.containsKey(o.id()) && (av == InviteFlow.Availability.OK || av == InviteFlow.Availability.START_HOSTING)) {
                right = SocialUi.smallButtonRight(ui, K_INVITE, right, y + 10, s.module.tr("chat.invite"), Widgets.Style.SECONDARY,
                        () -> s.module.invites().inviteToMyWorld(List.of(o.id())));
            }
        } else {
            Widgets.monogram(g, s.service.chatName(c), tx, y + 8, 18, Theme.BRAND);
            sub = s.module.tr("chats.members", c.members().size());
            right = SocialUi.smallButtonRight(ui, K_SETTINGS, right, y + 10, s.module.tr("chat.settings"), Widgets.Style.SECONDARY,
                    () -> s.navigate(SidebarRoute.groupSettings(c.id())));
            InviteFlow.Availability av = s.module.invites().availability();
            if (av == InviteFlow.Availability.OK || av == InviteFlow.Availability.START_HOSTING) {
                right = SocialUi.smallButtonRight(ui, K_INVITE, right, y + 10, s.module.tr("chat.invite"), Widgets.Style.SECONDARY, () -> {
                    List<String> ids = new ArrayList<>();
                    for (ChatMember m : c.members()) if (!m.user().id().equals(st.selfId())) ids.add(m.user().id());
                    s.module.invites().inviteToMyWorld(ids);
                });
            }
        }
        int nx = tx + 24;
        g.text(g.clip(s.service.chatName(c), right - nx - 4), nx, y + 8, Theme.TEXT);
        g.text(g.clip(sub == null ? "" : sub, right - nx - 4), nx, y + 18, Theme.DIM);
        g.hline(x, y + HEAD - 1, w, Theme.LINE);
    }

    private String orStatus(String text, String status) {
        return text != null ? text : s.module.tr("status." + status);
    }

    // ============================================================================================ messages

    private void layout(Gfx g, Chat c, int w) {
        SocialState st = s.state();
        if (layoutRev == st.revision() && layoutW == w && c.id().equals(layoutChat)) return;
        if (layoutW != w || wrapCache.size() > 2000) wrapCache.clear();
        layoutRev = st.revision();
        layoutW = w;
        layoutChat = c.id();
        blocks.clear();
        SocialState.History hist = st.history(c.id());
        int textW = w - 44;
        Message prev = null;
        for (Message m : hist.messages) {
            boolean header;
            List<String> lines;
            int h;
            if ("system".equals(m.kind())) {
                header = false;
                lines = List.of(systemText(s, m));
                h = 14;
            } else {
                header = prev == null || "system".equals(prev.kind()) || prev.authorId() == null || !prev.authorId().equals(m.authorId())
                        || m.createdAt() - prev.createdAt() > GROUP_GAP_MS;
                if (m.deleted()) {
                    lines = List.of(s.module.tr("chat.deleted"));
                } else if ("invite".equals(m.kind()) && m.invite() != null) {
                    lines = List.of();
                } else {
                    String key = m.id() + "|" + m.editedAt() + "|" + m.content().length();
                    lines = wrapCache.get(key);
                    if (lines == null) {
                        String text = m.content() + (m.editedAt() > 0 ? " " + s.module.tr("chat.edited") : "");
                        lines = TextWrap.wrap(g, text, textW, 0);
                        wrapCache.put(key, lines);
                    }
                }
                h = (header ? 12 : 0) + lines.size() * 10 + ("invite".equals(m.kind()) && m.invite() != null && !m.deleted() ? 50 : 0) + 2;
                if (header) h += 5;
            }
            blocks.add(new Block(m, header, lines, h));
            prev = m;
        }
    }

    private void drawMessages(Ui ui, Chat c, int x, int top, int w, int areaH) {
        Gfx g = ui.g;
        SocialState st = s.state();
        SocialState.History hist = st.history(c.id());
        layout(g, c, w);
        // receipts under our last message
        Message lastMine = null;
        for (int i = blocks.size() - 1; i >= 0; i--) {
            Message m = blocks.get(i).m();
            if (m.authorId() != null && m.authorId().equals(st.selfId()) && !"system".equals(m.kind())) {
                lastMine = m;
                break;
            }
            if (!"system".equals(m.kind())) break; // somebody answered after it: no receipt needed
        }
        List<String> readers = lastMine == null || lastMine.isPending() ? List.of() : st.readers(c, lastMine);
        int receiptH = lastMine != null ? 11 : 0;

        int total = 8 + receiptH;
        for (Block b : blocks) total += b.h();
        int topInfo = 16;
        total += topInfo;
        // keep the view still when a new message arrives while scrolled up
        String newest = blocks.isEmpty() ? null : blocks.get(blocks.size() - 1).m().id();
        if (newest != null && newestId != null && !newest.equals(newestId) && scrollTarget > 0) scrollTarget += total - contentH;
        newestId = newest;
        contentH = total;
        int max = Math.max(0, total - areaH);
        double wheel = ui.takeScroll(x, top, w, areaH);
        if (wheel != 0) scrollTarget += (float) (wheel * 24);
        scrollTarget = Math.max(0, Math.min(scrollTarget, max));
        float off = smooth.to(scrollTarget);
        if ((max == 0 || off >= max - 12) && hist.hasMore && !hist.loading && hist.loaded) s.service.loadHistory(c.id(), true);

        ui.clip(x, top, x + w, top + areaH);
        int cy = top + areaH - total + Math.round(off);
        // top info: loading / beginning
        String info = hist.loading ? s.module.tr("chat.loading") : !hist.hasMore && hist.loaded ? s.module.tr("chat.beginning") : "";
        if (!info.isEmpty() && ui.visibleY(cy, topInfo)) g.textCenter(info, x + w / 2, cy + 4, Theme.DIM);
        cy += topInfo;
        for (Block b : blocks) {
            if (ui.visibleY(cy, b.h())) drawBlock(ui, c, b, x, cy, w);
            cy += b.h();
        }
        if (lastMine != null) {
            String r = lastMine.isPending() ? s.module.tr("chat.sending")
                    : readers.isEmpty() ? s.module.tr("chat.sent")
                    : c.dm() ? s.module.tr("chat.seen") : s.module.tr("chat.seenBy", readers.size());
            g.textRight(r, x + w - 10, cy + 1, readers.isEmpty() ? Theme.DIM : Theme.BRAND2);
        }
        ui.unclip();
        if (max > 0) {
            int thumb = Math.max(16, areaH * areaH / Math.max(1, total));
            int ty = top + Math.round((areaH - thumb) * (1f - Math.min(1f, off / max)));
            g.rect(x + w - 2, ty, 2, thumb, 0x30FFFFFF);
        }
    }

    private void drawBlock(Ui ui, Chat c, Block b, int x, int y, int w) {
        Gfx g = ui.g;
        SocialState st = s.state();
        Message m = b.m();
        if ("system".equals(m.kind())) {
            g.textCenter(g.clip(b.lines().get(0), w - 20), x + w / 2, y + 3, Theme.DIM);
            return;
        }
        boolean mine = m.authorId() != null && m.authorId().equals(st.selfId());
        boolean hov = ui.hover(x, y, w, b.h());
        int cy = y;
        int tx = x + 30;
        if (b.header()) {
            cy += 5;
            User u = st.user(m.authorId());
            SocialUi.head(g, m.authorId(), u == null ? null : u.skinUrl(), x + 10, cy, 14, null);
            String name = u == null ? "?" : u.name();
            g.text(name, tx, cy, mine ? Theme.BRAND2 : Theme.TEXT);
            g.text(SocialUi.time(m.createdAt()), tx + g.width(name) + 6, cy, Theme.DIM);
            cy += 12;
        }
        if (hov && editingId == null) ui.g.rect(x, y, 1, b.h(), Theme.LINE2);
        // own text messages: edit / delete on hover
        if (hov && mine && "text".equals(m.kind()) && !m.deleted() && !m.isPending()) {
            int right = x + w - 8;
            int by = b.header() ? y + 4 : y;
            String del = s.module.tr("action.delete");
            int dw = g.width(del) + 8;
            boolean dh = ui.hover(right - dw, by, dw, 11);
            if (ui.clicked(right - dw, by, dw, 11)) s.service.deleteMessage(c.id(), m.id());
            g.rect(right - dw, by, dw, 11, dh ? 0xFF2A1218 : Theme.SURFACE2);
            g.text(del, right - dw + 4, by + 2, dh ? Theme.DANGER : Theme.MUTED);
            String ed = s.module.tr("action.edit");
            int ew = g.width(ed) + 8;
            int ex = right - dw - 3 - ew;
            boolean eh = ui.hover(ex, by, ew, 11);
            if (ui.clicked(ex, by, ew, 11)) startEdit(m);
            g.rect(ex, by, ew, 11, eh ? Theme.SURFACE3 : Theme.SURFACE2);
            g.text(ed, ex + 4, by + 2, eh ? Theme.TEXT : Theme.MUTED);
        }
        int color = m.deleted() ? Theme.DIM : m.isPending() ? Theme.MUTED : editingId != null && editingId.equals(m.id()) ? Theme.BRAND2 : Theme.TEXT;
        for (String line : b.lines()) {
            g.text(line, tx, cy, color);
            cy += 10;
        }
        if ("invite".equals(m.kind()) && m.invite() != null && !m.deleted()) {
            Invite inv = st.invites.get(m.invite().inviteId());
            drawInviteCard(ui, m, inv, tx, cy + 2, Math.min(220, w - 40), mine);
        }
    }

    private void drawInviteCard(Ui ui, Message m, Invite inv, int x, int y, int w, boolean mine) {
        Gfx g = ui.g;
        int h = 44;
        var card = m.invite();
        g.rect(x, y, w, h, Theme.alpha(Theme.BRAND, 0.08f));
        g.outline(x, y, w, h, Theme.alpha(Theme.BRAND, 0.45f));
        g.rect(x, y, 2, h, Theme.BRAND);
        g.text(g.clip(s.module.tr("chat.inviteCard", card.title()), w - 14), x + 8, y + 6, Theme.TEXT);
        if (card.subtitle() != null) g.text(g.clip(card.subtitle(), w - 14), x + 8, y + 16, Theme.MUTED);
        boolean expired = card.expiresAt() < System.currentTimeMillis();
        boolean answered = inv != null && !inv.open();
        int by = y + h - 16;
        if (mine) {
            g.text(s.module.tr(expired ? "chat.inviteExpired" : "chat.inviteSent"), x + 8, by + 3, Theme.DIM);
        } else if (expired || answered) {
            String key = expired ? "chat.inviteExpired" : "invite.status." + (inv.myResponse() != null ? inv.myResponse() : inv.status());
            g.text(s.module.tr(key), x + 8, by + 3, Theme.DIM);
        } else {
            String join = s.module.tr("action.join");
            if (SocialUi.smallButton(ui, card.inviteId(), x + 8, by, join, Widgets.Style.PRIMARY)) s.service.acceptInvite(card.inviteId());
            int dx = x + 8 + SocialUi.smallButtonWidth(g, join) + 4;
            if (SocialUi.smallButton(ui, (card.inviteId() + "#d").intern(), dx, by, s.module.tr("action.decline"), Widgets.Style.GHOST)) {
                s.service.declineInvite(card.inviteId());
            }
        }
    }

    static String systemText(SocialScreen s, Message m) {
        var p = m.system();
        if (p == null) return m.content();
        SocialState st = s.state();
        String actor = p.actorId() == null ? "" : st.nameOf(p.actorId());
        String target = p.targetId() == null ? "" : st.nameOf(p.targetId());
        String key = "chat.system." + p.event();
        String text = s.module.tr(key, actor, target, p.name() == null ? "" : p.name());
        return text.equals("social." + key) ? (m.content().isEmpty() ? p.event() : m.content()) : text;
    }

    // ============================================================================================ input

    private void drawInput(Ui ui, Chat c, int x, int y, int w, int h) {
        Gfx g = ui.g;
        g.hline(x, y, w, Theme.LINE);
        int fy = y + 7;
        if (editingId != null) {
            g.rect(x, y + 1, w, 11, Theme.alpha(Theme.BRAND2, 0.08f));
            g.text(g.clip(s.module.tr("chat.editing"), w - 20), x + 10, y + 2, Theme.BRAND2);
            fy += 12;
        }
        String ph = c.dm() ? s.module.tr("chat.placeholder", s.service.chatName(c)) : s.module.tr("chat.placeholderGroup", s.service.chatName(c));
        int max = Math.max(1, s.state().chatMaxLength);
        box = SocialUi.field(ui, inputKey, x + 8, fy, w - 16, input, max, ph, v -> {
            input = v;
            if (!v.isBlank() && editingId == null) s.service.typing(c.id());
        });
        if (wantFocus) {
            wantFocus = false;
            s.focus(box);
        }
        if (input.length() > max * 0.9) g.textRight(input.length() + "/" + max, x + w - 12, fy - 9, Theme.WARN);
    }

    private void startEdit(Message m) {
        editingId = m.id();
        input = m.content();
        if (box != null) box.setValue(input);
        wantFocus = true;
    }

    /** Keys while the chat is shown; true if used. */
    boolean key(int keyCode) {
        if (chatId == null) return false;
        boolean focused = box != null && box.isFocused();
        if (keyCode == Keys.ENTER && focused) {
            submit();
            return true;
        }
        if (keyCode == Keys.ESCAPE && editingId != null) {
            editingId = null;
            input = "";
            if (box != null) box.setValue("");
            return true;
        }
        if (keyCode == Keys.UP && focused && input.isEmpty() && editingId == null) {
            SocialState.History h = s.state().history(chatId);
            for (int i = h.messages.size() - 1; i >= 0; i--) {
                Message m = h.messages.get(i);
                if (m.authorId() != null && m.authorId().equals(s.state().selfId()) && "text".equals(m.kind()) && !m.deleted() && !m.isPending()) {
                    startEdit(m);
                    return true;
                }
            }
        }
        return false;
    }

    private void submit() {
        String text = input.trim();
        if (editingId != null) {
            String id = editingId;
            editingId = null;
            if (!text.isEmpty()) s.service.editMessage(chatId, id, text);
        } else if (!text.isEmpty()) {
            s.service.sendMessage(chatId, text);
            scrollTarget = 0;
        }
        input = "";
        if (box != null) box.setValue("");
    }
}
