package dev.henny.vibeyclient.ui.hud.edit;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

import java.util.ArrayList;
import java.util.List;

public final class EditActions {
    private final EditSession s;
    private final HudLayout.Box tmp = new HudLayout.Box();

    EditActions(EditSession s) {
        this.s = s;
    }

    public void undo() {
        if (s.history.undo()) afterRestore("ui.hud.undone");
    }

    public void redo() {
        if (s.history.redo()) afterRestore("ui.hud.redone");
    }

    private void afterRestore(String key) {
        s.refresh(s.width(), s.height());
        s.toast(Lang.tr(key));
    }

    public void nudge(int dx, int dy) {
        if (s.selection.isEmpty()) return;
        s.history.checkpoint("nudge");
        for (HudElement e : s.selection.items()) {
            HudLayout.Box b = s.box(e, tmp);
            HudLayout.moveTo(e, b.x + dx, b.y + dy, s.width(), s.height());
        }
    }

    public void scaleStep(HudElement target, int dir) {
        if (target != null && !s.selection.contains(target)) s.selection.only(target);
        if (s.selection.isEmpty()) return;
        s.history.checkpoint("wheel");
        for (HudElement e : s.selection.items()) {
            float next = ScaleSteps.next(HudLayout.placement(e).scale, dir);
            HudLayout.scaleTo(e, next, s.width(), s.height());
        }
    }

    public void scaleTo(float scale) {
        s.history.checkpoint("size");
        for (HudElement e : s.selection.items()) HudLayout.scaleTo(e, scale, s.width(), s.height());
    }

    public void hideSelection() {
        if (s.selection.isEmpty()) return;
        s.history.checkpoint(null);
        int n = s.selection.size();
        for (HudElement e : new ArrayList<>(s.selection.items())) {
            if (e.isCopy()) HudLayout.removeCopy(e);
            else HudLayout.setHidden(e, true);
        }
        s.selection.clear();
        s.refresh(s.width(), s.height());
        s.toast(n == 1 ? Lang.tr("ui.hud.hidden") : Lang.tr("ui.hud.hiddenMany", n));
    }

    public void resetSelection() {
        if (s.selection.isEmpty()) return;
        s.history.checkpoint(null);
        for (HudElement e : s.selection.items()) HudLayout.reset(e);
    }

    public void duplicateSelection() {
        boolean any = false;
        for (HudElement e : s.selection.items()) any |= e.duplicable();
        if (!any) return;
        s.history.checkpoint(null);
        List<HudElement> made = new ArrayList<>();
        for (HudElement e : new ArrayList<>(s.selection.items())) {
            HudElement c = e.duplicable() ? HudLayout.duplicate(e, s.width(), s.height()) : null;
            if (c != null) made.add(c);
        }
        s.refresh(s.width(), s.height());
        if (!made.isEmpty()) s.selection.set(made);
    }

    public void selectAll() {
        s.selection.set(s.canvas());
    }

    public void show(HudElement e, boolean on) {
        s.history.checkpoint(null);
        if (!on && e.isCopy()) HudLayout.removeCopy(e);
        else HudLayout.setHidden(e, !on);
        s.refresh(s.width(), s.height());
        if (on) s.selection.only(e);
        else s.selection.remove(e);
    }

    public HudElement addCopy(HudElement e) {
        s.history.checkpoint(null);
        HudElement c = HudLayout.duplicate(e, s.width(), s.height());
        s.refresh(s.width(), s.height());
        if (c != null) s.selection.only(c);
        return c;
    }

    public void apply(HudLayout.State state, String toastKey) {
        s.history.checkpoint(null);
        HudLayout.apply(state, true);
        s.history.breakMerge();
        s.selection.clear();
        s.refresh(s.width(), s.height());
        if (toastKey != null) s.toast(Lang.tr(toastKey));
    }

    public void resetAll() {
        s.history.checkpoint(null);
        HudLayout.resetAll();
        s.selection.clear();
        s.refresh(s.width(), s.height());
    }
}
