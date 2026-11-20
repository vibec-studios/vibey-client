package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudOption;
import dev.henny.vui.bind.ListState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class StyleLists {
    private StyleLists() {}

    static void anchors(ListState<Items.AnchorCell> out, Anchor current) {
        List<Items.AnchorCell> next = new ArrayList<>(9);
        for (Anchor a : Anchor.values()) next.add(new Items.AnchorCell(a.name().toLowerCase(Locale.ROOT), a == current));
        if (!next.equals(out.snapshot())) out.replaceAll(next);
    }

    static void options(ListState<Items.Option> out, HudElement e) {
        List<Items.Option> next = new ArrayList<>();
        if (e != null) {
            HudLayout.Placement p = HudLayout.placement(e);
            for (HudOption o : e.options()) {
                String v = p.opts.getOrDefault(o.key, o.def);
                boolean toggle = o.kind == HudOption.Kind.TOGGLE;
                String capture = o.captureLabel() == null ? "" : Lang.tr(o.captureLabel());
                next.add(new Items.Option(o.key, Lang.tr(o.labelKey), toggle, "1".equals(v), toggle ? "" : o.displayValue(v), capture, o));
            }
        }
        if (!next.equals(out.snapshot())) out.replaceAll(next);
    }
}
