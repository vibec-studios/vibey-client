package dev.henny.vibeyclient.module.servers.core;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PingCache<V> {
    private record Item<V>(V value, long at) {}

    private final Map<String, Item<V>> items;
    private final int maxEntries;
    private long ttlNanos;

    public PingCache(int maxEntries, long ttlMillis) {
        this.maxEntries = maxEntries;
        this.ttlNanos = ttlMillis * 1_000_000L;
        this.items = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Item<V>> eldest) {
                return size() > PingCache.this.maxEntries;
            }
        };
    }

    public void ttl(long ttlMillis) {
        this.ttlNanos = ttlMillis * 1_000_000L;
    }

    public void put(String address, V value, long now) {
        String k = AddressKey.of(address);
        if (!k.isEmpty() && value != null) items.put(k, new Item<>(value, now));
    }

    public V get(String address, long now) {
        String k = AddressKey.of(address);
        Item<V> item = items.get(k);
        if (item == null) return null;
        if (now - item.at() > ttlNanos) {
            items.remove(k);
            return null;
        }
        return item.value();
    }

    public int size() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }
}
