package dev.henny.vui.dom;

import java.util.List;

public final class StaticChild implements ChildSource {
    private final Element element;

    public StaticChild(Element element) {
        this.element = element;
        element.owner(this);
    }

    public Element element() {
        return element;
    }

    @Override
    public void collect(List<Element> out) {
        out.add(element);
    }

    @Override
    public void dispose() {
        Lifecycle.dispose(element);
    }
}
