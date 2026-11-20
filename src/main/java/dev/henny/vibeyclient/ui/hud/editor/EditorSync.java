package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.edit.EditSession;
import dev.henny.vibeyclient.ui.hud.edit.PointerTool;
import dev.henny.vibeyclient.ui.hud.edit.ScaleSteps;
import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;

final class EditorSync {
    private static final float DOCK_W = 300;

    private final EditorController c;
    private final EditSession s;
    private final PointerTool tool;
    private final LibraryList library;
    private final HudLayout.Box group = new HudLayout.Box();
    private final HudLayout.Box tmp = new HudLayout.Box();

    EditorSync(EditorController c, PointerTool tool) {
        this.c = c;
        this.s = c.session;
        this.tool = tool;
        this.library = new LibraryList(c);
    }

    void frame(Document doc) {
        s.snapping(c.snap.peek());
        c.firstRun.set(!HudLayout.flag("intro"));
        c.busy.set(tool.busy());
        c.widgetCount.set(s.canvas().size());
        c.canUndo.set(s.history.canUndo());
        c.canRedo.set(s.history.canRedo());
        c.dockTop.set(dockTop());
        c.selected.set(bar(doc));
        library.sync();
        Badges.sync(c);
        if ("style".equals(c.panel.peek())) c.style.sync();
    }

    private boolean dockTop() {
        float bottom = s.height() - 40;
        float left = (s.width() - DOCK_W) / 2f, right = (s.width() + DOCK_W) / 2f;
        for (HudElement e : s.selection.items()) {
            HudLayout.Box b = s.box(e, tmp);
            if (b.y + b.h > bottom && b.x < right && b.x + b.w > left) return true;
        }
        return false;
    }

    private Items.Bar bar(Document doc) {
        if (s.selection.isEmpty()) return null;
        HudElement p = s.selection.primary();
        s.groupBox(group);
        Element el = doc.ref("wbar");
        float bw = el == null ? 120 : el.box().width;
        float x = Math.max(2, Math.min(s.width() - bw - 2, group.x));
        boolean above = group.y + group.h + 22 > s.height() - (c.dockTop.peek() ? 0 : 34);
        float y = above ? Math.max(2, group.y - 21) : group.y + group.h + 3;
        String name = s.selection.size() > 1 ? Lang.tr("ui.hud.count", s.selection.size()) : p.displayName();
        String pct = ScaleSteps.percent(HudLayout.placement(p).scale) + "%";
        return new Items.Bar(name, pct, Math.round(x), Math.round(y), above, s.selection.size() == 1 && p.isCopy());
    }
}
