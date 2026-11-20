package dev.henny.vibeyclient.module.screens.apply;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.screens.core.Box;
import dev.henny.vibeyclient.module.screens.core.Geometry;
import dev.henny.vibeyclient.module.screens.core.Placement;
import dev.henny.vibeyclient.module.screens.core.ScreenLayout;
import dev.henny.vibeyclient.module.screens.mixin.ContainerAccess;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

final class PanelApplier {
    private PanelApplier() {}

    static Box current(AbstractContainerScreen<?> c) {
        ContainerAccess a = (ContainerAccess) c;
        return new Box(a.vibey$leftPos(), a.vibey$topPos(), a.vibey$imageWidth(), a.vibey$imageHeight());
    }

    static float[] apply(ScreenSession s, AbstractContainerScreen<?> c, ScreenLayout layout) {
        WidgetState st = s.panelState;
        Box cur = current(c);
        if (st.applied == null || !cur.same(st.applied)) st.base = cur;
        Placement p = layout.placement(Item.PANEL);
        Box visual = p.isDefault() ? st.base : Geometry.place(st.base, p, s.width(), s.height(), false);
        ContainerAccess a = (ContainerAccess) c;
        a.vibey$setLeftPos(Math.round(visual.x()));
        a.vibey$setTopPos(Math.round(visual.y()));
        st.applied = p.isDefault() ? null : current(c);
        s.panel = new Item(Item.PANEL, null, null, false, false, st.base, visual, false, Lang.tr("screens.ui.panel"));
        return new float[] {visual.x() - st.base.x(), visual.y() - st.base.y()};
    }

    static void restore(ScreenSession s, AbstractContainerScreen<?> c) {
        WidgetState st = s.panelState;
        if (st.applied != null && current(c).same(st.applied)) {
            ContainerAccess a = (ContainerAccess) c;
            a.vibey$setLeftPos(Math.round(st.base.x()));
            a.vibey$setTopPos(Math.round(st.base.y()));
        }
        st.applied = null;
    }

    static boolean moved(ScreenSession s, AbstractContainerScreen<?> c) {
        return s.panelState.applied != null && !current(c).same(s.panelState.applied);
    }
}
