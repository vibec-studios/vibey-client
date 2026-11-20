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

    static void apply(ScreenSession s, AbstractContainerScreen<?> c, ScreenLayout layout) {
        Box base = current(c);
        Placement p = layout.placement(Item.PANEL);
        Box visual = p.isDefault() ? base : Geometry.place(base, p, s.width(), s.height(), false);
        s.offsetX = visual.x() - base.x();
        s.offsetY = visual.y() - base.y();
        s.panel = new Item(Item.PANEL, null, null, false, false, base, base, false, Lang.tr("screens.ui.panel"));
    }

    static void restore(ScreenSession s) {
        s.offsetX = 0;
        s.offsetY = 0;
    }
}
