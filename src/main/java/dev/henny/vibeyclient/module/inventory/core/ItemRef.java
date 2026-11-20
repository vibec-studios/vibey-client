package dev.henny.vibeyclient.module.inventory.core;

import java.util.Objects;

public record ItemRef(String id, String name) {
    public static final ItemRef EMPTY = new ItemRef("", null);

    public ItemRef {
        id = id == null ? "" : id;
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
