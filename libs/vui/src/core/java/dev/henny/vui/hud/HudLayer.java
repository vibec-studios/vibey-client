package dev.henny.vui.hud;

import dev.henny.vui.dom.Document;
import dev.henny.vui.geom.Rect;
import dev.henny.vui.render.Canvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HudLayer {
    private static final int OUTLINE = 0x66FFFFFF;
    private static final int SELECTED = 0xFF8B6CFF;
    private static final int GUIDE = 0xFF3FE0C5;

    private final List<HudWidget> widgets = new ArrayList<>();
    private final HudEditor editor = new HudEditor(this);
    private float width;
    private float height;

    public HudWidget add(String id, Document document, HudPlacement defaults) {
        HudWidget w = new HudWidget(id, document, defaults);
        widgets.add(w);
        if (width > 0) w.layout(width, height);
        return w;
    }

    public void remove(String id) {
        widgets.removeIf(w -> w.id().equals(id));
    }

    public HudWidget widget(String id) {
        for (HudWidget w : widgets) if (w.id().equals(id)) return w;
        return null;
    }

    public List<HudWidget> widgets() {
        return Collections.unmodifiableList(widgets);
    }

    public HudEditor editor() {
        return editor;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public void resize(float w, float h) {
        width = w;
        height = h;
        layout();
    }

    public void layout() {
        for (int i = 0; i < widgets.size(); i++) widgets.get(i).layout(width, height);
    }

    public void frame(float dt) {
        for (int i = 0; i < widgets.size(); i++) {
            HudWidget w = widgets.get(i);
            w.document().frame(dt);
            w.layout(width, height);
        }
    }

    public HudWidget widgetAt(float x, float y) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            HudWidget w = widgets.get(i);
            if ((w.visible() || editor.active()) && w.bounds().contains(x, y)) return w;
        }
        return null;
    }

    public void render(Canvas canvas) {
        for (int i = 0; i < widgets.size(); i++) {
            HudWidget w = widgets.get(i);
            if (!w.visible() && !editor.active()) continue;
            Rect b = w.bounds();
            canvas.push();
            canvas.translate(b.x(), b.y());
            canvas.scale(w.placement().scale(), w.placement().scale());
            if (!w.visible()) canvas.pushAlpha(0.35f);
            w.document().render(canvas);
            if (!w.visible()) canvas.popAlpha();
            canvas.pop();
        }
        if (editor.active()) renderEditor(canvas);
    }

    private void renderEditor(Canvas canvas) {
        canvas.layer();
        for (HudWidget w : widgets) outline(canvas, w.bounds(), w == editor.selected() ? SELECTED : OUTLINE);
        for (SnapGuide g : editor.guides()) {
            if (g.vertical()) canvas.fill(g.position(), g.from(), 1, g.to() - g.from(), GUIDE);
            else canvas.fill(g.from(), g.position(), g.to() - g.from(), 1, GUIDE);
        }
    }

    private static void outline(Canvas c, Rect r, int color) {
        c.fill(r.x(), r.y(), r.width(), 1, color);
        c.fill(r.x(), r.bottom() - 1, r.width(), 1, color);
        c.fill(r.x(), r.y(), 1, r.height(), color);
        c.fill(r.right() - 1, r.y(), 1, r.height(), color);
    }

    public String toJson() {
        return HudLayoutCodec.write(this);
    }

    public void fromJson(String json) {
        HudLayoutCodec.read(this, json);
        layout();
    }
}
