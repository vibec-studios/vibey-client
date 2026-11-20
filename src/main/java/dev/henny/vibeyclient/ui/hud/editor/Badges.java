package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vibeyclient.ui.ServerBadge;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

import java.util.ArrayList;
import java.util.List;

final class Badges {
    private Badges() {}

    private static final HudLayout.Box TMP = new HudLayout.Box();

    static void sync(EditorController c) {
        List<Items.Badge> next = new ArrayList<>();
        for (HudElement e : c.session.canvas()) {
            if (!HudLayout.serverDisabled(e)) continue;
            HudLayout.Box b = c.session.box(e, TMP);
            float y = b.h >= 14 ? b.y + b.h / 2f - 6 : b.y + b.h + 3;
            next.add(new Items.Badge(e.id(), Math.round(Math.max(2, b.x + 2)), Math.round(y), ServerBadge.hint()));
        }
        if (!next.equals(c.badges.snapshot())) c.badges.replaceAll(next);
    }
}
