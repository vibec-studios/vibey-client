package dev.henny.vibeyclient.module.waypoints.core.map;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

public final class LruSet<K> {
    private final LinkedHashMap<K, Boolean> order = new LinkedHashMap<>(64, 0.75f, true);
    private final int capacity;

    public LruSet(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    public List<K> touch(K key) {
        order.put(key, Boolean.TRUE);
        List<K> evicted = new ArrayList<>(0);
        Iterator<K> it = order.keySet().iterator();
        while (order.size() - evicted.size() > capacity && it.hasNext()) {
            K oldest = it.next();
            if (oldest.equals(key)) continue;
            evicted.add(oldest);
        }
        for (K k : evicted) order.remove(k);
        return evicted;
    }

    public void remove(K key) {
        order.remove(key);
    }

    public boolean contains(K key) {
        return order.containsKey(key);
    }

    public int size() {
        return order.size();
    }

    public void clear() {
        order.clear();
    }
}
