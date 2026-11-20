package dev.henny.vui.style;

public interface StyleTarget {
    String styleTag();

    String styleId();

    boolean hasClass(String name);

    int states();

    StyleTarget styleParent();

    default StyleTarget stylePreviousSibling() {
        return null;
    }
}
