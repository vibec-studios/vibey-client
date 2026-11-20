package dev.henny.cosmetico.registry;

import java.util.Comparator;

public final class ItemOrder {
    private ItemOrder() {}

    public static final Comparator<Item> ORDER = Comparator
            .comparingInt((Item i) -> i.isEmote() ? 100 : i.slot() == null ? 99 : i.slot().ordinal())
            .thenComparingInt(Item::sort)
            .thenComparing(Item::id);
}
