package dev.henny.vibeyclient.module.cosmetics.catalog;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.Engine;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemSource;
import dev.henny.cosmetico.thumbs.ResourceReader;
import dev.henny.cosmetico.mc.thumbs.McThumbnails;

import java.util.Map;

public final class BuiltinItems {
    private BuiltinItems() {}

    public static final String ROOT = "/assets/vibeyclient/builtin/";

    public static void load() {
        McThumbnails.anchor(BuiltinItems.class);
        Engine e = Cosmetico.engine();
        Cosmetico.host().workers().execute(() -> {
            Map<String, Item> items = e.indexLoader().read(ResourceReader.classpath(BuiltinItems.class), ROOT, ItemSource.BUILTIN);
            e.main.post(() -> e.items.putAll(ItemSource.BUILTIN, items, true));
        });
    }
}
