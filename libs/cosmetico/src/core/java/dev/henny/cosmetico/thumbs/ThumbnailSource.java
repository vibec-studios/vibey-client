package dev.henny.cosmetico.thumbs;

import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemRegistry;
import dev.henny.cosmetico.registry.ItemSource;

public final class ThumbnailSource {
    private ThumbnailSource() {}

    public static String of(Item item, ItemRegistry items) {
        Item builtin = items.get(item.id(), ItemSource.BUILTIN);
        if (item.isDev() && item.thumbnail() != null) return item.thumbnail();
        if (builtin != null && builtin.thumbnail() != null) return builtin.thumbnail();
        return item.thumbnail();
    }
}
