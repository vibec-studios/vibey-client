package dev.henny.cosmetico.thumbs;

import dev.henny.cosmetico.host.Log;
import dev.henny.cosmetico.host.ThumbnailFetcher;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public final class ThumbnailStore<T> {
    private static final Object LOADING = new Object(), FAILED = new Object();
    private final Map<String, Object> state = new HashMap<>();
    private final Map<String, byte[]> ready = new ConcurrentHashMap<>();
    private final ItemRegistry items;
    private final DevThumbnails dev;
    private final ThumbnailTextures<T> textures;
    private final ResourceReader resources;
    private final Supplier<ThumbnailFetcher> fetcher;
    private final Executor loader;
    private final Supplier<Log> log;

    public ThumbnailStore(ItemRegistry items, DevThumbnails dev, ThumbnailTextures<T> textures, ResourceReader resources,
                          Supplier<ThumbnailFetcher> fetcher, Executor loader, Supplier<Log> log) {
        this.items = items;
        this.dev = dev;
        this.textures = textures;
        this.resources = resources;
        this.fetcher = fetcher;
        this.loader = loader;
        this.log = log;
        dev.onForget(this::forget);
    }

    @SuppressWarnings("unchecked")
    public T get(Item item) {
        String src = ThumbnailSource.of(item, items);
        if (src == null) return null;
        Object s = state.get(src);
        if (s == null && src.startsWith(DevThumbnails.PREFIX)) s = startDev(src);
        if (s == null) {
            state.put(src, LOADING);
            loader.execute(() -> load(src));
            return null;
        }
        if (s == LOADING) s = finish(src);
        if (s == LOADING || s == FAILED || s == null) return null;
        T t = (T) s;
        return textures.released(t) ? null : t;
    }

    private Object startDev(String src) {
        byte[] png = dev.png(src);
        if (png == null) return FAILED;
        state.put(src, LOADING);
        ready.put(src, png);
        return LOADING;
    }

    private Object finish(String src) {
        byte[] png = ready.remove(src);
        if (png == null) return LOADING;
        Object s;
        try {
            s = png.length == 0 ? FAILED : textures.upload(png);
        } catch (Exception e) {
            s = FAILED;
        }
        state.put(src, s);
        return s;
    }

    private void load(String src) {
        if (!src.startsWith("/")) {
            fetcher.get().fetch(src).whenComplete((png, e) -> {
                if (e != null) log.get().debug("Thumbnail " + src + " failed: " + e);
                ready.put(src, png == null ? new byte[0] : png);
            });
            return;
        }
        byte[] png = null;
        try {
            png = resources.read(src);
        } catch (Exception e) {
            log.get().debug("Thumbnail " + src + " failed: " + e);
        }
        ready.put(src, png == null ? new byte[0] : png);
    }

    @SuppressWarnings("unchecked")
    private void forget(String key) {
        ready.remove(key);
        Object s = state.remove(key);
        if (s != null && s != LOADING && s != FAILED) textures.release((T) s);
    }
}
