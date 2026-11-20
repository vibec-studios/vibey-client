package dev.henny.vui.dom;

import java.util.ArrayList;
import java.util.List;

final class LeavingElements {
    private final List<Element> elements = new ArrayList<>();
    private final List<ChildSource> owners = new ArrayList<>();
    private DocumentStyles styles;

    void add(Element e, ChildSource owner, DocumentStyles styles) {
        this.styles = styles;
        elements.add(e);
        owners.add(owner);
    }

    void update() {
        for (int i = elements.size() - 1; i >= 0; i--) {
            Element e = elements.get(i);
            boolean animating = styles != null && styles.isAnimating(e);
            if (e.flags().style || animating) continue;
            ChildSource owner = owners.get(i);
            elements.remove(i);
            owners.remove(i);
            owner.removeLeft(e);
            Lifecycle.dispose(e);
        }
    }

    void clear() {
        elements.clear();
        owners.clear();
    }

    int size() {
        return elements.size();
    }
}
