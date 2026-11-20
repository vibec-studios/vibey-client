package dev.henny.cosmetico.registry;

import dev.henny.cosmetico.host.Log;
import dev.henny.cosmetico.model.Slot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public final class ItemRegistry {
    private final Map<ItemSource, Map<String, Item>> sources = new TreeMap<>(SourceOrder.ORDER);
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final List<ItemListener> itemListeners = new CopyOnWriteArrayList<>();
    private final Supplier<Log> log;
    private volatile RegistrySnapshot snapshot = RegistrySnapshot.EMPTY;
    private volatile long version;
    private long revisions;

    public ItemRegistry(Supplier<Log> log) {
        this.log = log;
    }

    public List<Item> all() { return snapshot.sorted(); }

    public Item get(String id) { return id == null ? null : snapshot.byId().get(id); }

    public synchronized Item get(String id, ItemSource source) {
        Map<String, Item> m = sources.get(source);
        return m == null ? null : m.get(id);
    }

    public List<Item> bySlot(Slot slot) {
        List<Item> out = new ArrayList<>();
        for (Item i : snapshot.sorted()) if (!i.isEmote() && i.slot() == slot) out.add(i);
        return out;
    }

    public List<Item> emotes() {
        List<Item> out = new ArrayList<>();
        for (Item i : snapshot.sorted()) if (i.isEmote()) out.add(i);
        return out;
    }

    public long version() { return version; }

    public synchronized long nextRevision() { return ++revisions; }

    public AutoCloseable listen(Runnable onChange) {
        listeners.add(onChange);
        return () -> listeners.remove(onChange);
    }

    public AutoCloseable listenItems(ItemListener l) {
        itemListeners.add(l);
        return () -> itemListeners.remove(l);
    }

    public void put(Item item) {
        Item old;
        synchronized (this) {
            old = source(item.source()).put(item.id(), item);
            rebuild();
        }
        Notify.changed(listeners, log.get());
        Notify.replaced(itemListeners, old, item, log.get());
    }

    public void putAll(ItemSource source, Map<String, Item> items, boolean replace) {
        Map<String, Item> before;
        synchronized (this) {
            Map<String, Item> m = source(source);
            before = new HashMap<>(m);
            if (replace) m.clear();
            m.putAll(items);
            rebuild();
        }
        Notify.changed(listeners, log.get());
        Notify.swapped(itemListeners, before, items, replace, log.get());
    }

    public Item remove(String id, ItemSource source) {
        Item old;
        synchronized (this) {
            old = source(source).remove(id);
            if (old != null) rebuild();
        }
        if (old == null) return null;
        Notify.changed(listeners, log.get());
        Notify.removed(itemListeners, old, log.get());
        return old;
    }

    private Map<String, Item> source(ItemSource s) {
        return sources.computeIfAbsent(s, k -> new HashMap<>());
    }

    private void rebuild() {
        Map<String, Item> byId = new HashMap<>();
        for (Map<String, Item> m : sources.values()) byId.putAll(m);
        List<Item> sorted = new ArrayList<>(byId.values());
        sorted.sort(ItemOrder.ORDER);
        snapshot = new RegistrySnapshot(Collections.unmodifiableMap(byId), Collections.unmodifiableList(sorted));
        version++;
    }
}
