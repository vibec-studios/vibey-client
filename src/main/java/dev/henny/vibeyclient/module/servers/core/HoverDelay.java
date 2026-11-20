package dev.henny.vibeyclient.module.servers.core;

import java.util.Objects;

public final class HoverDelay<K> {
    private final long delayNanos;
    private K key;
    private long since;

    public HoverDelay(long delayMillis) {
        this.delayNanos = delayMillis * 1_000_000L;
    }

    public K update(K hovered, long now) {
        if (!Objects.equals(hovered, key)) {
            key = hovered;
            since = now;
        }
        return key != null && now - since >= delayNanos ? key : null;
    }

    public void clear() {
        key = null;
    }
}
