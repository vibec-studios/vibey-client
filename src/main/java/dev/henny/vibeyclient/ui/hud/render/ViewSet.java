package dev.henny.vibeyclient.ui.hud.render;

import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vui.Vui;
import dev.henny.vui.dom.Document;
import dev.henny.vui.hud.HudLayer;
import dev.henny.vui.hud.HudPlacement;
import dev.henny.vui.hud.HudWidget;
import dev.henny.vui.layout.Anchor;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.template.CompiledTemplate;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class ViewSet {
    private static final String SOURCE = "<template><" + PaintBehavior.TAG + " :view=\"self\"/></template>";

    private final HudLayer layer;
    private final Map<HudElement, ElementView> byElement = new IdentityHashMap<>();
    private final List<ElementView> list = new ArrayList<>();
    private final List<HudElement> scratch = new ArrayList<>();
    private CompiledTemplate template;
    private int seen = -1;

    ViewSet(HudLayer layer) {
        this.layer = layer;
    }

    List<ElementView> list() {
        return list;
    }

    ElementView of(HudElement e) {
        return byElement.get(e);
    }

    void sync() {
        if (seen == HudLayout.registryVersion()) return;
        seen = HudLayout.registryVersion();
        Vui vui = VuiMc.vui();
        HudLayout.elements(scratch);
        for (int i = list.size() - 1; i >= 0; i--) {
            ElementView v = list.get(i);
            if (HudLayout.get(v.element().id()) == v.element()) continue;
            drop(vui, v);
            list.remove(i);
        }
        list.clear();
        for (HudElement e : scratch) list.add(byElement.computeIfAbsent(e, k -> create(vui, k)));
    }

    private void drop(Vui vui, ElementView v) {
        byElement.remove(v.element());
        if (layer.widget(v.element().id()) == v.widget()) layer.remove(v.element().id());
        vui.close(v.document());
    }

    private ElementView create(Vui vui, HudElement e) {
        if (template == null) {
            vui.behaviors().register(PaintBehavior.TAG, new PaintBehavior());
            template = vui.compile("vibeyclient:hud/element", "hud-element.vui", SOURCE);
        }
        ElementView v = new ElementView(e);
        Document doc = vui.create(template, v);
        if (layer.widget(e.id()) != null) layer.remove(e.id());
        HudWidget w = layer.add(e.id(), doc, new HudPlacement(Anchor.TOP_LEFT, 0, 0, 1f, false));
        v.attach(doc, w);
        return v;
    }
}
