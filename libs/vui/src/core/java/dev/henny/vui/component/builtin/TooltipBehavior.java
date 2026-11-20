package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.dom.Element;
import dev.henny.vui.dom.OverlayPlacement;

public final class TooltipBehavior implements Behavior, OverlayPlacement {
    @Override
    public boolean overlay(Element e) {
        return true;
    }

    @Override
    public String defaultPlacement() {
        return "bottom";
    }

    @Override
    public void activate(Element e) {
    }
}
