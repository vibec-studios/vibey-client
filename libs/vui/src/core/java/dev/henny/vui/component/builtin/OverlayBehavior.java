package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.dom.Dismissable;
import dev.henny.vui.dom.Element;
import dev.henny.vui.dom.OverlayPlacement;
import dev.henny.vui.style.StateFlags;

public class OverlayBehavior implements Behavior, OverlayPlacement, Dismissable {
    private final String placement;
    private final boolean lightDismiss;

    public OverlayBehavior(String placement, boolean lightDismiss) {
        this.placement = placement;
        this.lightDismiss = lightDismiss;
    }

    @Override
    public boolean overlay(Element e) {
        return true;
    }

    @Override
    public String defaultPlacement() {
        return placement;
    }

    public boolean lightDismiss() {
        return lightDismiss;
    }

    @Override
    public void activate(Element e) {
    }

    public static void open(Element e, boolean open) {
        if (e.flags().overlayOpen == open) return;
        e.flags().overlayOpen = open;
        e.state(StateFlags.OPEN, open);
        if (e.parent() != null) e.parent().state(StateFlags.OPEN, open);
        if (e.document() != null) e.document().invalidateLayout();
    }

    @Override
    public void dismiss(Element e) {
        open(e, false);
        e.emit("update:open", Boolean.FALSE);
        e.emit("close", null);
    }
}
