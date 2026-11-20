package dev.henny.cosmetico.registry;

import java.util.Comparator;

final class SourceOrder {
    private SourceOrder() {}

    static final Comparator<ItemSource> ORDER = Comparator.comparingInt(ItemSource::priority).thenComparing(ItemSource::id);
}
