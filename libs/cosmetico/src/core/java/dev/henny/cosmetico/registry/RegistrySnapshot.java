package dev.henny.cosmetico.registry;

import java.util.List;
import java.util.Map;

record RegistrySnapshot(Map<String, Item> byId, List<Item> sorted) {
    static final RegistrySnapshot EMPTY = new RegistrySnapshot(Map.of(), List.of());
}
