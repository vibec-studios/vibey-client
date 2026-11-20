package dev.henny.vibeyclient.module.screens.apply;

import dev.henny.vibeyclient.module.screens.core.Box;
import dev.henny.vibeyclient.module.screens.core.Geometry;
import dev.henny.vibeyclient.module.screens.core.IdMatcher;
import dev.henny.vibeyclient.module.screens.core.Placement;
import dev.henny.vibeyclient.module.screens.core.ScreenLayout;
import dev.henny.vibeyclient.module.screens.core.WidgetIds;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.ArrayList;
import java.util.List;

public final class LayoutApplier {
    private LayoutApplier() {}

    public static void apply(ScreenSession s) {
        ScreenLayout layout = s.layout();
        Screen screen = s.screen;
        List<AbstractWidget> ws = WidgetScan.scan(screen);
        List<WidgetIds.Fact> facts = new ArrayList<>(ws.size());
        for (AbstractWidget w : ws) facts.add(new WidgetIds.Fact(WidgetTypes.type(w), WidgetKeys.key(w.getMessage())));
        List<WidgetIds.Id> ids = WidgetIds.assign(facts);
        String[] matched = IdMatcher.match(ids, layout.widgets());
        s.items.clear();
        s.panel = null;
        Box panelBase = null;
        float[] delta = {0, 0};
        if (screen instanceof AbstractContainerScreen<?> c) {
            delta = PanelApplier.apply(s, c, layout);
            panelBase = s.panel.base;
        }
        for (int i = 0; i < ws.size(); i++) {
            AbstractWidget w = ws.get(i);
            String id = matched[i] != null ? matched[i] : ids.get(i).primary();
            Placement p = matched[i] != null ? layout.placement(id) : Placement.DEFAULT;
            boolean title = layout.hideTitle() && w instanceof AbstractStringWidget && w.getMessage() == screen.getTitle();
            s.items.add(one(s, w, id, ids.get(i).fallback(), p, title, panelBase, delta));
        }
    }

    private static Item one(ScreenSession s, AbstractWidget w, String id, String fallback, Placement p, boolean title,
                            Box panelBase, float[] delta) {
        WidgetState st = Rects.state(w, true);
        Box cur = Rects.of(w);
        if (st.applied == null || !cur.same(st.applied)) {
            st.base = cur;
            if (!st.hiddenByUs) st.visibleBefore = w.visible;
        }
        Box base = st.base;
        boolean child = panelBase != null && (delta[0] != 0 || delta[1] != 0) && panelBase.containsCenterOf(base);
        Box eff = child ? base.shifted(delta[0], delta[1]) : base;
        boolean scalable = WidgetTypes.scalable(w);
        boolean hidden = p.hidden() || title;
        boolean changes = !p.isDefault() || child || title;
        Box visual = changes ? Geometry.place(eff, p, s.width(), s.height(), scalable) : base;
        if (changes) {
            int lw = Math.round(Geometry.logicalWidth(eff, p)), lh = Math.round(eff.h());
            st.scale = scalable ? p.scale() : 1f;
            st.logicalW = lw;
            st.logicalH = lh;
            Rects.set(w, visual);
            hide(w, st, hidden);
            st.applied = Rects.of(w);
        } else {
            reset(w, st);
        }
        String label = w.getMessage() == null ? "" : w.getMessage().getString();
        return new Item(id, fallback, w, scalable, WidgetTypes.resizable(w), eff, visual, hidden, label);
    }

    static void hide(AbstractWidget w, WidgetState st, boolean hidden) {
        st.hidden = hidden;
        if (hidden) {
            if (!st.hiddenByUs) st.visibleBefore = w.visible;
            st.hiddenByUs = true;
            w.visible = false;
        } else if (st.hiddenByUs) {
            st.hiddenByUs = false;
            w.visible = st.visibleBefore;
        }
    }

    static void reset(AbstractWidget w, WidgetState st) {
        if (st.applied != null && Rects.of(w).same(st.applied)) Rects.set(w, st.base);
        st.applied = null;
        st.scale = 1f;
        hide(w, st, false);
    }

    public static void restore(ScreenSession s) {
        for (Item i : s.items) if (i.widget != null) {
            WidgetState st = Rects.state(i.widget, false);
            if (st != null) reset(i.widget, st);
        }
        if (s.screen instanceof AbstractContainerScreen<?> c) PanelApplier.restore(s, c);
    }

    public static boolean panelMoved(ScreenSession s) {
        return s.screen instanceof AbstractContainerScreen<?> c && PanelApplier.moved(s, c);
    }
}
