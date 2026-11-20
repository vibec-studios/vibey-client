package dev.henny.vui.hud;

import dev.henny.vui.dom.Document;
import dev.henny.vui.geom.Rect;
import dev.henny.vui.layout.Size;

public final class HudWidget {
    private final String id;
    private final Document document;
    private final HudPlacement defaults;
    private HudPlacement placement;
    private float contentWidth;
    private float contentHeight;
    private Rect bounds = new Rect(0, 0, 0, 0);

    public HudWidget(String id, Document document, HudPlacement defaults) {
        this.id = id;
        this.document = document;
        this.defaults = defaults;
        this.placement = defaults;
    }

    public String id() {
        return id;
    }

    public Document document() {
        return document;
    }

    public HudPlacement placement() {
        return placement;
    }

    public void placement(HudPlacement next) {
        placement = next;
    }

    public HudPlacement defaults() {
        return defaults;
    }

    public Rect bounds() {
        return bounds;
    }

    public float contentWidth() {
        return contentWidth;
    }

    public float contentHeight() {
        return contentHeight;
    }

    public boolean visible() {
        return placement.visible();
    }

    void layout(float viewportW, float viewportH) {
        if (document.isLayoutDirty() || contentWidth == 0) {
            Size s = document.measureContent(viewportW, viewportH);
            contentWidth = Math.max(1, s.width);
            contentHeight = Math.max(1, s.height);
            document.resize(contentWidth, contentHeight);
        }
        document.layoutIfNeeded();
        bounds = placement.rect(contentWidth, contentHeight, viewportW, viewportH);
    }
}
