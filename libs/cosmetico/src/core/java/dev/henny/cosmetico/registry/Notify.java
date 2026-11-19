package dev.henny.cosmetico.registry;

import dev.henny.cosmetico.host.Log;

import java.util.List;
import java.util.Map;

final class Notify {
    private Notify() {}

    static void changed(List<Runnable> listeners, Log log) {
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (RuntimeException e) {
                log.error("Item registry listener failed", e);
            }
        }
    }

    static void replaced(List<ItemListener> listeners, Item old, Item now, Log log) {
        for (ItemListener l : listeners) {
            try {
                l.replaced(old, now);
            } catch (RuntimeException e) {
                log.error("Item listener failed", e);
            }
        }
    }

    static void removed(List<ItemListener> listeners, Item old, Log log) {
        for (ItemListener l : listeners) {
            try {
                l.removed(old);
            } catch (RuntimeException e) {
                log.error("Item listener failed", e);
            }
        }
    }

    static void swapped(List<ItemListener> listeners, Map<String, Item> before, Map<String, Item> items, boolean replace, Log log) {
        for (Map.Entry<String, Item> e : items.entrySet()) replaced(listeners, before.get(e.getKey()), e.getValue(), log);
        if (!replace) return;
        for (Map.Entry<String, Item> e : before.entrySet()) if (!items.containsKey(e.getKey())) removed(listeners, e.getValue(), log);
    }
}
