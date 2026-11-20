package dev.henny.vibeyclient.ui.hud.render;

import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.WidgetPainter;
import dev.henny.vui.dom.Document;
import dev.henny.vui.hud.HudPlacement;
import dev.henny.vui.hud.HudWidget;
import dev.henny.vui.layout.Anchor;

public final class ElementView {
    private final HudElement element;
    private Document doc;
    private HudWidget widget;
    private float w = 1;
    private float h = 1;
    private boolean shown;

    ElementView(HudElement element) {
        this.element = element;
    }

    public ElementView self() {
        return this;
    }

    void attach(Document document, HudWidget vuiWidget) {
        doc = document;
        widget = vuiWidget;
    }

    Document document() {
        return doc;
    }

    public HudElement element() {
        return element;
    }

    public HudWidget widget() {
        return widget;
    }

    public boolean shown() {
        return shown;
    }

    float width() {
        return w;
    }

    float height() {
        return h;
    }

    boolean sync(Gfx g, boolean editor, boolean hideAll, int sw, int sh, long now, HudLayout.Box box) {
        HudLayout.Placement p = HudLayout.placement(element);
        boolean show = editor ? element.available() && !p.hidden
                : element.isWidget() && !hideAll && !p.hidden && element.available() && !HudLayout.serverDisabled(element);
        if (show) show = WidgetPainter.update(element, g, now, editor);
        int bw = HudLayout.boxWidth(element);
        int bh = HudLayout.boxHeight(element);
        if (bw != w || bh != h) {
            w = bw;
            h = bh;
            doc.invalidateLayout();
        }
        HudLayout.box(element, sw, sh, box);
        HudPlacement cur = widget.placement();
        if (cur.offsetX() != box.x || cur.offsetY() != box.y || cur.scale() != box.scale || cur.visible() != show) {
            widget.placement(new HudPlacement(Anchor.TOP_LEFT, box.x, box.y, box.scale, show));
        }
        shown = show;
        return show;
    }

    void paint() {
        WidgetPainter.paint(element, HudFrame.gfx(), HudFrame.now(), HudFrame.editor());
    }
}
