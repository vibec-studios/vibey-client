package dev.henny.vibeyclient.module.social.ui;

import dev.henny.vibeyclient.module.social.InviteFlow;
import dev.henny.vibeyclient.module.social.SocialCompat;
import dev.henny.vibeyclient.module.social.SocialState;
import dev.henny.vibeyclient.module.social.model.Model.Friend;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.ScrollArea;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;
import dev.henny.vibeyclient.ui.toast.TextWrap;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/** Pick friends and invite them into the world we host (hosting starts first if needed). */
final class InviteView {
    private final SocialScreen s;
    private final ScrollArea scroll = new ScrollArea();
    private final Set<String> picked = new LinkedHashSet<>();
    private static final Object K_SEND = new Object(), K_BACK = new Object(), K_HOST = new Object();

    InviteView(SocialScreen s) {
        this.s = s;
    }

    void open(String preselect) {
        picked.clear();
        if (preselect != null) picked.add(preselect);
        scroll.reset();
    }

    void draw(Ui ui, int x, int y, int w, int h, boolean showBack) {
        Gfx g = ui.g;
        SocialState st = s.state();
        if (SocialUi.backHeader(ui, K_BACK, x, y, w, s.module.tr("invite.title"), showBack)) s.goBack();
        InviteFlow.Availability av = s.module.invites().availability();
        int barH = 28;
        int by = y + h - barH;
        g.hline(x, by, w, Theme.LINE);
        boolean can = !picked.isEmpty() && (av == InviteFlow.Availability.OK || av == InviteFlow.Availability.START_HOSTING);
        String label = av == InviteFlow.Availability.START_HOSTING ? s.module.tr("invite.hostAndSend", picked.size()) : s.module.tr("invite.send", picked.size());
        if (Widgets.button(ui, K_SEND, x + 8, by + 6, w - 16, 16, label, can ? Widgets.Style.PRIMARY : Widgets.Style.DISABLED)) {
            s.module.invites().inviteToMyWorld(new ArrayList<>(picked));
            s.goBack();
        }

        int cy = scroll.begin(ui, x, y + 26, w, h - 26 - barH) + 8;
        int cx = x + 12;
        int cw = w - 24;
        String world = SocialCompat.singleplayerWorld();
        String info = switch (av) {
            case OK -> s.module.tr("invite.infoHosting", world == null ? "?" : world);
            case START_HOSTING -> s.module.tr("invite.infoStart", world == null ? "?" : world);
            case NO_WORLD -> s.module.tr("invite.noWorldHint");
            case NO_MODULE -> s.module.tr("invite.noModuleHint");
        };
        int col = av == InviteFlow.Availability.OK ? Theme.OK : av == InviteFlow.Availability.START_HOSTING ? Theme.BRAND2 : Theme.WARN;
        var lines = TextWrap.wrap(g, info, cw - 10, 4);
        int boxH = lines.size() * 10 + 10;
        g.rect(cx, cy, cw, boxH, Theme.alpha(col, 0.08f));
        g.rect(cx, cy, 2, boxH, col);
        int ly = cy + 5;
        for (String l : lines) {
            g.text(l, cx + 8, ly, Theme.MUTED);
            ly += 10;
        }
        cy += boxH + 10;
        var hs = dev.henny.vibeyclient.api.Services.get(dev.henny.vibeyclient.api.service.HostingService.class).orElse(null);
        if (hs != null && (av == InviteFlow.Availability.OK || av == InviteFlow.Availability.START_HOSTING)) {
            if (Widgets.button(ui, K_HOST, cx, cy - 4, cw, 14, s.module.tr("invite.hostScreen"), Widgets.Style.GHOST)) hs.openHostScreen();
            cy += 16;
        }

        cy = SocialUi.caption(g, s.module.tr("invite.pick"), cx, cy, cw);
        if (st.friends.isEmpty()) {
            g.text(s.module.tr("friends.none"), cx, cy + 2, Theme.DIM);
            cy += 16;
        }
        for (Friend f : st.sortedFriends()) {
            String id = f.user().id();
            boolean on = picked.contains(id);
            int tx = x + w - 12 - Widgets.TOGGLE_W;
            if (Widgets.toggle(ui, (id + "#inv").intern(), tx, cy + 7, on, true) | SocialUi.row(ui, (id + "#invrow").intern(), x, cy, w, 26, false)) {
                if (on) picked.remove(id);
                else picked.add(id);
            }
            SocialUi.head(g, id, f.user().skinUrl(), x + 12, cy + 5, 16, f.presence().status());
            g.text(g.clip(f.user().name(), tx - x - 44), x + 34, cy + 4, on ? Theme.TEXT : Theme.MUTED);
            String sub = f.presence().online() ? s.service.activityText(f.presence()) : s.module.tr("status.offline");
            if (sub != null) g.text(g.clip(sub, tx - x - 44), x + 34, cy + 14, Theme.DIM);
            cy += 26;
        }
        cy += 6;
        scroll.end(ui, cy);
    }
}
