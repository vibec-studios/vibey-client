package dev.henny.vui.dom;

import dev.henny.vui.style.StateFlags;
import dev.henny.vui.template.CompiledTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MountRegistry {
    private final Map<String, Element> refs = new HashMap<>();
    private final Map<String, CompiledTemplate> components = new HashMap<>();
    private final List<Element> entering = new ArrayList<>();
    private final List<Element> framed = new ArrayList<>();
    private final LeavingElements leaving = new LeavingElements();

    Map<String, Element> refs() {
        return refs;
    }

    void ref(String name, Element e) {
        refs.put(name, e);
    }

    boolean useComponent(CompiledTemplate t) {
        return components.put(t.id(), t) == null;
    }

    Collection<CompiledTemplate> components() {
        return components.values();
    }

    void entering(Element e) {
        e.state(StateFlags.ENTER, true);
        entering.add(e);
    }

    boolean finishEntering() {
        if (entering.isEmpty()) return false;
        for (int i = 0; i < entering.size(); i++) {
            Element e = entering.get(i);
            e.state(StateFlags.ENTER, false);
            e.flags().entering = false;
        }
        entering.clear();
        return true;
    }

    void leaving(Element e, ChildSource owner, DocumentStyles styles) {
        leaving.add(e, owner, styles);
    }

    void updateLeaving() {
        leaving.update();
    }

    void framed(Element e, boolean on) {
        if (on && !framed.contains(e)) framed.add(e);
        if (!on) framed.remove(e);
    }

    void runFramed(float dt) {
        for (int i = 0; i < framed.size(); i++) framed.get(i).behavior().frame(framed.get(i), dt);
    }

    void forget(Element e) {
        framed.remove(e);
        entering.remove(e);
        if (e.ref() != null && refs.get(e.ref()) == e) refs.remove(e.ref());
    }

    void clear() {
        refs.clear();
        components.clear();
        entering.clear();
        leaving.clear();
        framed.clear();
    }
}
