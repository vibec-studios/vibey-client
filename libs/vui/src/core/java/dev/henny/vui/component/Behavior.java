package dev.henny.vui.component;

import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Event;
import dev.henny.vui.layout.LayoutEngine;
import dev.henny.vui.layout.Size;
import dev.henny.vui.paint.PaintContext;

public interface Behavior {
    default boolean focusable(Element e) {
        return false;
    }

    default boolean overlay(Element e) {
        return false;
    }

    default void mount(Element e) {
    }

    default void unmount(Element e) {
    }

    default void propChanged(Element e, String name, Object value) {
    }

    default boolean measure(Element e, LayoutEngine layout, float maxWidth, float maxHeight, Size out) {
        return false;
    }

    default boolean arrangeChildren(Element e, LayoutEngine layout) {
        return false;
    }

    default void paint(Element e, PaintContext ctx) {
    }

    default void paintOver(Element e, PaintContext ctx) {
    }

    default void handle(Element e, Event event) {
    }

    default void activate(Element e) {
        e.click();
    }

    default void scrolled(Element e) {
    }

    default void frame(Element e, float dt) {
    }

    default String modelProp() {
        return "value";
    }
}
