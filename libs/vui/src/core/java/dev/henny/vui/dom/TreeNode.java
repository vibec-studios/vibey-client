package dev.henny.vui.dom;

import java.util.ArrayList;
import java.util.List;

public abstract class TreeNode {
    private final ArrayList<Element> children = new ArrayList<>();
    private final List<ChildSource> sources = new ArrayList<>();
    Document document;
    Element parent;
    private ChildSource owner;

    public Document document() {
        return document;
    }

    public Element parent() {
        return parent;
    }

    public List<Element> children() {
        return children;
    }

    public int childCount() {
        return children.size();
    }

    public Element child(int i) {
        return children.get(i);
    }

    public List<ChildSource> sources() {
        return sources;
    }

    public ChildSource owner() {
        return owner;
    }

    public void owner(ChildSource value) {
        owner = value;
    }

    public void attach(Document doc) {
        document = doc;
        for (int i = 0; i < children.size(); i++) children.get(i).attach(doc);
    }

    void parent(Element p) {
        parent = p;
    }

    public void rebuildChildren() {
        ChildListBuilder.rebuild((Element) this);
    }
}
