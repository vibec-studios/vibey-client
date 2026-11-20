package dev.henny.vui.icon;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class IconSet {
    private final String namespace;
    private final Map<String, PixelIcon> icons = new LinkedHashMap<>();

    public IconSet(String namespace) {
        this.namespace = namespace;
    }

    public String namespace() {
        return namespace;
    }

    public IconSet put(PixelIcon icon) {
        icons.put(icon.name(), icon);
        return this;
    }

    public IconSet put(String name, String... rows) {
        return put(PixelIcon.fromRows(name, java.util.List.of(rows)));
    }

    public IconSet alias(String name, String target) {
        PixelIcon icon = icons.get(target);
        if (icon != null) icons.put(name, icon);
        return this;
    }

    public PixelIcon get(String name) {
        return icons.get(name);
    }

    public Set<String> names() {
        return Collections.unmodifiableSet(icons.keySet());
    }

    public Collection<PixelIcon> icons() {
        return Collections.unmodifiableCollection(icons.values());
    }

    public int size() {
        return icons.size();
    }
}
