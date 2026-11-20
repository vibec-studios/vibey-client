package dev.henny.vibeyclient.module.screens.edit;

import dev.henny.vibeyclient.module.screens.Screens;
import dev.henny.vibeyclient.module.screens.apply.Item;
import dev.henny.vibeyclient.module.screens.apply.ScreenSession;
import dev.henny.vibeyclient.module.screens.apply.Sessions;
import dev.henny.vibeyclient.module.screens.core.Box;
import dev.henny.vibeyclient.module.screens.core.Geometry;
import dev.henny.vibeyclient.module.screens.core.Placement;
import dev.henny.vibeyclient.module.screens.core.ScaleSteps;
import dev.henny.vibeyclient.module.screens.core.ScreenLayout;
import dev.henny.vibeyclient.module.screens.core.ScreenNames;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

public final class EditActions {
    private EditActions() {}

    public static void commit(EditSession e, String tag, ScreenLayout next) {
        ScreenLayout before = e.screen.layout();
        if (next.equals(before)) return;
        e.history.record(before, tag, System.currentTimeMillis());
        store(e.screen, next);
    }

    public static void store(ScreenSession s, ScreenLayout next) {
        float laidOut = s.scale();
        Screens.store().put(s.name, next.withLabel(label(s)));
        if (next.scale() != laidOut) relayout(s);
        else Sessions.reapply(s);
    }

    public static void relayout(ScreenSession s) {
        Window w = Minecraft.getInstance().getWindow();
        s.screen.resize(w.getGuiScaledWidth(), w.getGuiScaledHeight());
    }

    static String label(ScreenSession s) {
        String title = s.screen.getTitle() == null ? "" : s.screen.getTitle().getString();
        boolean obfuscated = s.name.contains("class_");
        return obfuscated && !title.isBlank() ? title : ScreenNames.readable(s.name);
    }

    static Placement placement(EditSession e, Item item) {
        return e.screen.layout().placement(item.id).withFallback(item.fallback);
    }

    static void put(EditSession e, String tag, Item item, Placement p) {
        commit(e, tag, e.screen.layout().with(item.id, p));
    }

    static void undo(EditSession e) {
        ScreenLayout prev = e.history.undo(e.screen.layout());
        if (prev != null) store(e.screen, prev);
    }

    static void redo(EditSession e) {
        ScreenLayout next = e.history.redo(e.screen.layout());
        if (next != null) store(e.screen, next);
    }

    static void toggleHide(EditSession e, Item item) {
        if (item == null || item.panel()) return;
        Placement p = placement(e, item);
        put(e, null, item, p.withHidden(!p.hidden()));
    }

    static void reset(EditSession e, Item item) {
        if (item == null) return;
        if (item.panel()) {
            commit(e, null, e.screen.layout().with(Item.PANEL, Placement.DEFAULT).withScale(1f));
            return;
        }
        put(e, null, item, Placement.DEFAULT);
    }

    static void resetScreen(EditSession e) {
        commit(e, null, ScreenLayout.EMPTY);
        e.selected = null;
    }

    static void wheel(EditSession e, Item item, int dir) {
        if (item == null) return;
        if (item.panel()) {
            screenScale(e, dir);
            return;
        }
        if (!item.scalable) return;
        Placement p = placement(e, item);
        float next = ScaleSteps.next(p.scale(), dir, Placement.MIN_SCALE, Placement.MAX_SCALE);
        if (next == p.scale()) return;
        put(e, "wheel:" + item.id, item, Geometry.scaled(p, item.visual, next, e.screen.width(), e.screen.height()));
    }

    static void nudge(EditSession e, Item item, int dx, int dy) {
        if (item == null) return;
        Box moved = item.visual.shifted(dx, dy);
        put(e, "nudge:" + item.id, item, Geometry.moved(placement(e, item), moved, e.screen.width(), e.screen.height()));
    }

    static void screenScale(EditSession e, int dir) {
        ScreenLayout l = e.screen.layout();
        float next = ScaleSteps.next(l.scale(), dir, ScreenLayout.MIN_SCALE, ScreenLayout.MAX_SCALE);
        if (next != l.scale()) commit(e, "scale", l.withScale(next));
    }

    static void dim(EditSession e, int dir) {
        ScreenLayout l = e.screen.layout();
        int from = l.dim() == ScreenLayout.VANILLA ? 70 : l.dim();
        int next = Math.max(0, Math.min(ScreenLayout.MAX_DIM, from + dir * 10));
        commit(e, "dim", l.withDim(next));
    }

    static void blur(EditSession e, int dir) {
        ScreenLayout l = e.screen.layout();
        int from = l.blur() == ScreenLayout.VANILLA ? Minecraft.getInstance().options.menuBackgroundBlurriness().get() : l.blur();
        int next = Math.max(0, Math.min(ScreenLayout.MAX_BLUR, from + dir));
        commit(e, "blur", l.withBlur(next));
    }

    static void title(EditSession e) {
        ScreenLayout l = e.screen.layout();
        commit(e, null, l.withHideTitle(!l.hideTitle()));
    }
}
