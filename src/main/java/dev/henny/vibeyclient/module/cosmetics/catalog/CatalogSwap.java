package dev.henny.vibeyclient.module.cosmetics.catalog;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemSource;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Tasks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class CatalogSwap {
    private CatalogSwap() {}

    static void swap(Map<String, Item> items, String version, int gen, boolean fromDisk) {
        Tasks.onMain(() -> {
            if (!GlobalSettings.online() || gen != CatalogState.GENERATION.get()) return;
            boolean first = CatalogState.lastIds == null || fromDisk;
            Set<String> fresh = new HashSet<>(items.keySet());
            if (!first) fresh.removeAll(CatalogState.lastIds);
            if (!fromDisk) CatalogState.lastIds = Set.copyOf(items.keySet());
            Cosmetico.items().putAll(ItemSource.REMOTE, items, true);
            CatalogState.loaded = true;
            CatalogState.catalogVersion = version;
            fresh.removeIf(id -> {
                Item i = items.get(id);
                return i == null || i.needsUpdate(Cosmetico.versionGate());
            });
            if (!first && !fresh.isEmpty()) notifyNew(fresh);
        });
    }

    private static void notifyNew(Set<String> fresh) {
        try {
            CatalogState.onNewItems.accept(fresh);
        } catch (RuntimeException ex) {
            Vibey.LOG.debug("onNewItems failed: {}", ex.toString());
        }
    }
}
