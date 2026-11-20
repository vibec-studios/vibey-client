package dev.henny.cosmetico.thumbs;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class DevThumbnails {
    public static final String PREFIX = "dev:";
    private final Map<String, String> keys = new HashMap<>();
    private final Map<String, byte[]> pngs = new HashMap<>();
    private int counter;
    private Consumer<String> onForget = k -> {};

    public synchronized String put(String id, byte[] png) {
        remove(id);
        String key = PREFIX + id + "#" + (++counter);
        keys.put(id, key);
        pngs.put(key, png);
        return key;
    }

    public synchronized String key(String id) {
        return keys.get(id);
    }

    public synchronized byte[] png(String key) {
        return pngs.get(key);
    }

    public synchronized void remove(String id) {
        String old = keys.remove(id);
        if (old == null) return;
        pngs.remove(old);
        onForget.accept(old);
    }

    public synchronized void onForget(Consumer<String> listener) {
        onForget = listener;
    }
}
