package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Controls;
import dev.henny.vui.component.TemplatedBehavior;
import dev.henny.vui.dom.Element;
import dev.henny.vui.dom.build.SlotContent;
import dev.henny.vui.dom.build.TreeBuilder;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.layout.LayoutEngine;
import dev.henny.vui.layout.Size;

public final class ListBehavior implements TemplatedBehavior {
    public static final int OVERSCAN = 2;

    @Override
    public void template(Element e, SlotContent content, TreeBuilder builder) {
        VirtualRows rows = new VirtualRows(e, content, builder);
        e.behaviorState(rows);
        e.sources().add(rows);
    }

    @Override
    public void activate(Element e) {
    }

    @Override
    public void mount(Element e) {
        e.document().wantsFrames(e, true);
        VirtualRows rows = e.behaviorState();
        if (rows != null) rows.update();
    }

    @Override
    public void propChanged(Element e, String name, Object value) {
        VirtualRows rows = e.behaviorState();
        if (rows == null) return;
        if (name.equals("items")) rows.itemsChanged();
        if (name.equals("items") || name.equals("item-size")) e.markLayoutDirty();
    }

    @Override
    public void scrolled(Element e) {
        VirtualRows rows = e.behaviorState();
        if (rows != null) rows.update();
    }

    @Override
    public void frame(Element e, float dt) {
        VirtualRows rows = e.behaviorState();
        if (rows != null) rows.update();
    }

    @Override
    public boolean measure(Element e, LayoutEngine layout, float maxWidth, float maxHeight, Size out) {
        VirtualRows rows = e.behaviorState();
        float total = rows == null ? 0 : rows.count() * itemSize(e);
        out.set(Float.isFinite(maxWidth) ? maxWidth : 100, Math.min(total, Float.isFinite(maxHeight) ? maxHeight : total));
        return true;
    }

    @Override
    public boolean arrangeChildren(Element e, LayoutEngine layout) {
        VirtualRows rows = e.behaviorState();
        if (rows == null) return true;
        LayoutBox b = e.box();
        float size = itemSize(e);
        b.contentWidth = b.innerWidth();
        b.contentHeight = rows.count() * size;
        for (int i = 0; i < e.childCount(); i++) {
            Element c = e.child(i);
            int index = rows.indexOf(c);
            if (index < 0) continue;
            layout.arrange(c, b.innerX(), b.innerY() + index * size, b.innerWidth(), size);
        }
        e.scrollTo(e.scrollX(), e.scrollY());
        return true;
    }

    static float itemSize(Element e) {
        return Math.max(1f, Controls.number(e, "item-size", 16f));
    }
}
