package dev.henny.vibeyclient.module.clips.shots;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.module.clips.shots.core.Shot;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

final class ShotImages {
    enum Kind { THUMB, PREVIEW }

    private static final int MAX_THUMBS = 400, MAX_PREVIEWS = 3;
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Vibey screenshots");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private final Map<String, String> thumbs = new LinkedHashMap<>(64, 0.75f, true);
    private final Map<String, String> previews = new LinkedHashMap<>(8, 0.75f, true);
    private final Set<String> pending = new HashSet<>();
    private int generation;

    void request(Shot s, Kind kind, Consumer<String> ready) {
        Map<String, String> cache = kind == Kind.THUMB ? thumbs : previews;
        String key = s.key();
        String id = cache.get(key);
        if (id != null) {
            ready.accept(id);
            return;
        }
        String job = kind + key;
        if (!pending.add(job)) return;
        int gen = generation;
        Function<Shot, ShotDecoder.Pixels> decode = kind == Kind.THUMB ? ShotDecoder::thumb : ShotDecoder::preview;
        LOADER.execute(() -> {
            ShotDecoder.Pixels p = gen == generation ? decode.apply(s) : null;
            Tasks.onMain(() -> finish(job, gen, key, cache, kind, p, ready));
        });
    }

    void close() {
        generation++;
        pending.clear();
        releaseAll(thumbs);
        releaseAll(previews);
    }

    int loaded() {
        return thumbs.size() + previews.size();
    }

    private void finish(String job, int gen, String key, Map<String, String> cache, Kind kind, ShotDecoder.Pixels p,
                        Consumer<String> ready) {
        pending.remove(job);
        if (gen != generation || p == null) return;
        try {
            String id = ShotTextures.upload(p);
            cache.put(key, id);
            evict(cache, kind == Kind.THUMB ? MAX_THUMBS : MAX_PREVIEWS);
            ready.accept(id);
        } catch (RuntimeException e) {
            Vibey.LOG.debug("Screenshots: texture upload failed: {}", e.toString());
        }
    }

    private static void evict(Map<String, String> cache, int max) {
        Iterator<Map.Entry<String, String>> it = cache.entrySet().iterator();
        while (cache.size() > max && it.hasNext()) {
            ShotTextures.release(it.next().getValue());
            it.remove();
        }
    }

    private static void releaseAll(Map<String, String> cache) {
        for (String id : cache.values()) ShotTextures.release(id);
        cache.clear();
    }
}
