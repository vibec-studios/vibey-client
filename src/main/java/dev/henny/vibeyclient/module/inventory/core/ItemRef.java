package dev.henny.vibeyclient.module.inventory.core;

import java.util.Objects;

public record ItemRef(String id, String name) {
    public static final ItemRef EMPTY = new ItemRef("", null);

    public static final int MAX_NAME = 64;

    public ItemRef {
        id = id == null ? "" : id;
        if (name != null && name.length() > MAX_NAME) name = name.substring(0, MAX_NAME);
    }

    public boolean empty() {
        return id.isEmpty();
    }

    public boolean sameItem(ItemRef other) {
        return other != null && !empty() && id.equals(other.id);
    }

    public boolean exact(ItemRef other) {
        return sameItem(other) && Objects.equals(name, other.name);
    }
}
