package dev.henny.vibeyclient.module.social.ui;

import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.module.social.SidebarRoute;
import dev.henny.vibeyclient.module.social.SocialState;
import dev.henny.vibeyclient.module.social.model.Model.FriendRequest;
import dev.henny.vibeyclient.module.social.model.Model.SearchResult;
import dev.henny.vibeyclient.module.social.model.Model.User;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;
import net.minecraft.client.gui.components.EditBox;

import java.util.ArrayList;
import java.util.List;

/** Requests tab: "add friend" with name search, incoming requests (accept/decline), outgoing (cancel). */
final class RequestsView {
    private final SocialScreen s;
    private final ScrollArea scroll = new ScrollArea();
    private static final Object K_SEARCH = new Object();
    private static final Object K_SEND = new Object();
    private String query = "";
    private String searched = "";
    private List<SearchResult> results = new ArrayList<>();
    private boolean searching;
    private int searchSeq;
    private boolean wantFocus;
    private EditBox box;

    RequestsView(SocialScreen s) {
        this.s = s;
    }

    void focusSearch() {
        wantFocus = true;
    }

    /** Enter in the search field: send a request to exactly that name. */
    boolean enter() {
        if (box == null || !box.isFocused() || query.isBlank()) return false;
        send();
        return true;
    }

    private void send() {
        String name = query.trim();
        if (name.isEmpty()) return;
        s.service.sendFriendRequest(name, null).thenRun(() -> Tasks.onMain(() -> {
            query = "";
            results = new ArrayList<>();
            searched = "";
        }));
    }

    private void search(String q) {
        query = q;
        String t = q.trim();
        int seq = ++searchSeq;
        if (t.length() < 2) {
            results = new ArrayList<>();
            searched = "";
            searching = false;
            return;
        }
        searching = true;
        Tasks.debounce("vibeyclient-social-search", 300, () -> s.service.search(t).whenCompleteAsync((list, err) -> {
            if (seq != searchSeq) return;
            searching = false;
            searched = t;
            results = list == null ? new ArrayList<>() : new ArrayList<>(list);
        }, dev.henny.vibeyclient.module.social.SocialService.MAIN));
    }

    void draw(Ui ui, int x, int y, int w, int h) {
        Gfx g = ui.g;
        SocialState st = s.state();
        // search box (fixed above the list)
        int fx = x + 10;
        int fw = w - 20;
        Widgets.caption(g, s.module.tr("requests.add"), fx, y + 8);
        String sendLabel = s.module.tr("action.send");
        int sw = SocialUi.smallButtonWidth(g, sendLabel) + 4;
        box = SocialUi.field(ui, K_SEARCH, fx, y + 21, fw - sw - 4, query, 25, s.module.tr("requests.placeholder"), this::search);
        if (wantFocus) {
            wantFocus = false;
            s.focus(box);
        }
        if (Widgets.button(ui, K_SEND, fx + fw - sw, y + 21, sw, 16, sendLabel, query.isBlank() ? Widgets.Style.DISABLED : Widgets.Style.PRIMARY)) send();
        int top = y + 44;
        g.hline(x, top - 1, w, Theme.LINE);

        int cy = scroll.begin(ui, x, top, w, h - (top - y)) + 6;
        if (!query.isBlank()) {
            if (searching && results.isEmpty()) {
                g.text(s.module.tr("requests.searching"), fx, cy, Theme.DIM);
                cy += 14;
            } else if (results.isEmpty() && !searched.isEmpty()) {
                g.text(g.clip(s.module.tr("requests.noResults", searched), fw), fx, cy, Theme.DIM);
                cy += 14;
            }
            for (SearchResult r : results) cy = resultRow(ui, r, x, cy, w);
            if (!results.isEmpty()) cy += 6;
        }

        List<FriendRequest> in = new ArrayList<>();
        List<FriendRequest> out = new ArrayList<>();
        for (FriendRequest r : st.requests.values()) (r.incoming() ? in : out).add(r);
        cy = SocialUi.caption(g, s.module.tr("requests.incoming", in.size()), fx, cy, w);
        if (in.isEmpty()) {
            g.text(s.module.tr("requests.noneIncoming"), fx, cy + 2, Theme.DIM);
            cy += 16;
        }
        for (FriendRequest r : in) cy = requestRow(ui, r, x, cy, w);
        cy += 8;
        cy = SocialUi.caption(g, s.module.tr("requests.outgoing", out.size()), fx, cy, w);
        if (out.isEmpty()) {
            g.text(s.module.tr("requests.noneOutgoing"), fx, cy + 2, Theme.DIM);
            cy += 16;
        }
        for (FriendRequest r : out) cy = requestRow(ui, r, x, cy, w);
        cy += 6;
        scroll.end(ui, cy);
    }

    private int requestRow(Ui ui, FriendRequest r, int x, int y, int w) {
        Gfx g = ui.g;
        int h = SocialUi.ROW;
        User u = r.other();
        int right = x + w - 8;
        if (r.incoming()) {
            right = SocialUi.smallButtonRight(ui, r.id(), right, y + 6, s.module.tr("action.decline"), Widgets.Style.GHOST,
                    () -> s.service.declineRequest(r.id()));
            right = SocialUi.smallButtonRight(ui, (r.id() + "#a").intern(), right, y + 6, s.module.tr("action.accept"), Widgets.Style.PRIMARY,
                    () -> s.service.acceptRequest(r.id()));
        } else {
            right = SocialUi.smallButtonRight(ui, r.id(), right, y + 6, s.module.tr("action.cancel"), Widgets.Style.GHOST,
                    () -> s.service.cancelRequest(r.id()));
        }
        SocialUi.row(ui, (r.id() + "#row").intern(), x, y, w, h, false);
        SocialUi.head(g, u == null ? null : u.id(), u == null ? null : u.skinUrl(), x + 10, y + 5, 16, null);
        int tx = x + 32;
        g.text(g.clip(u == null ? "?" : u.name(), right - tx - 4), tx, y + 4, Theme.TEXT);
        g.text(g.clip(SocialUi.ago(r.createdAt()), right - tx - 4), tx, y + 14, Theme.DIM);
        return y + h;
    }

    private int resultRow(Ui ui, SearchResult r, int x, int y, int w) {
        Gfx g = ui.g;
        int h = SocialUi.ROW;
        User u = r.user();
        int right = x + w - 8;
        String rel = r.relationship();
        switch (rel) {
            case "none" -> right = SocialUi.smallButtonRight(ui, u.id(), right, y + 6, s.module.tr("action.add"), Widgets.Style.PRIMARY,
                    () -> s.service.sendFriendRequest(null, u.id()).thenRun(() -> Tasks.onMain(() -> search(query))));
            case "incoming" -> right = SocialUi.smallButtonRight(ui, u.id(), right, y + 6, s.module.tr("action.accept"), Widgets.Style.PRIMARY, () -> {
                for (FriendRequest fr : s.state().requests.values()) {
                    if (fr.incoming() && fr.other() != null && fr.other().id().equals(u.id())) s.service.acceptRequest(fr.id());
                }
            });
            default -> {
                String label = s.module.tr("relationship." + rel);
                g.textRight(label, right, y + 9, Theme.DIM);
                right -= g.width(label) + 6;
            }
        }
        if (SocialUi.row(ui, (u.id() + "#res").intern(), x, y, w, h, false)) s.navigate(SidebarRoute.profile(u.id()));
        SocialUi.head(g, u.id(), u.skinUrl(), x + 10, y + 5, 16, null);
        int tx = x + 32;
        g.text(g.clip(u.name(), right - tx - 4), tx, y + 4, Theme.TEXT);
        if (u.statusText() != null) g.text(g.clip(u.statusText(), right - tx - 4), tx, y + 14, Theme.DIM);
        return y + h;
    }
}
