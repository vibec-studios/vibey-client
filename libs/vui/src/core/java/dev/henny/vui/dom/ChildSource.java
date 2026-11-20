package dev.henny.vui.dom;

import java.util.List;

public interface ChildSource {
    void collect(List<Element> out);

    void dispose();

    default void removeLeft(Element element) {
    }
}
