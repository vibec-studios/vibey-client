package dev.henny.vibeyclient.module.waypoints.map;

import dev.henny.vibeyclient.module.waypoints.core.map.ChunkSample;
import dev.henny.vibeyclient.module.waypoints.core.map.LruSet;
import dev.henny.vibeyclient.module.waypoints.core.map.TileComposer;
import dev.henny.vibeyclient.module.waypoints.core.map.TileData;
import dev.henny.vibeyclient.module.waypoints.core.map.TileKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class TileStore {
    static final int MAX_TILES = 256;

    private final TileWorker worker;
    private final Map<TileKey, TileData> tiles = new HashMap<>();
    private final Map<TileKey, Integer> saved = new HashMap<>();
    private final LruSet<TileKey> lru = new LruSet<>(MAX_TILES);
    private final Set<TileKey> loading = new HashSet<>();
    private final Set<TileKey> absent = new HashSet<>();
    private final TileShading shading = new TileShading();
    private TileDisk disk;
    private int generation;

    TileStore(TileWorker worker) {
        this.worker = worker;
    }

    synchronized void open(TileDisk d) {
        clearLocked();
        disk = d;
        generation++;
    }

    void close() {
        TileDisk d;
        List<Map.Entry<TileKey, TileData>> dirty;
        synchronized (this) {
            d = disk;
            dirty = dirtyLocked();
            clearLocked();
            disk = null;
            generation++;
        }
        if (d != null && !dirty.isEmpty()) worker.execute(() -> dirty.forEach(e -> d.write(e.getKey(), e.getValue())));
    }

    synchronized TileData peek(TileKey k) {
        TileData t = tiles.get(k);
        if (t != null) touchLocked(k);
        return t;
    }

    TileData request(TileKey k) {
        int gen;
        TileDisk d;
        synchronized (this) {
            TileData t = tiles.get(k);
            if (t != null) {
                touchLocked(k);
                return t;
            }
            if (disk == null || absent.contains(k) || !loading.add(k)) return null;
            gen = generation;
            d = disk;
        }
        worker.execute(() -> load(k, d, gen));
        return null;
    }

    void apply(ChunkSample s) {
        TileKey k = s.tile();
        TileData t = obtain(k);
        if (t == null) return;
        boolean changed = TileComposer.apply(t, s);
        if (changed || t.shadedVersion() != t.version()) shading.shade(t, quiet(k.north()));
    }

    private synchronized TileData quiet(TileKey k) {
        return tiles.get(k);
    }

    void saveDirty() {
        TileDisk d;
        List<Map.Entry<TileKey, TileData>> dirty;
        synchronized (this) {
            d = disk;
            dirty = dirtyLocked();
        }
        if (d != null) dirty.forEach(e -> d.write(e.getKey(), e.getValue()));
    }

    synchronized Set<String> openFiles() {
        Set<String> out = new HashSet<>();
        if (disk != null) for (TileKey k : tiles.keySet()) out.add(disk.path(k).toString());
        return out;
    }

    synchronized int loadedCount() {
        return tiles.size();
    }

    private void load(TileKey k, TileDisk d, int gen) {
        TileData data = d.read(k);
        synchronized (this) {
            loading.remove(k);
            if (gen != generation || tiles.containsKey(k)) return;
            if (data == null) {
                absent.add(k);
                return;
            }
            tiles.put(k, data);
            saved.put(k, data.version());
            touchLocked(k);
        }
        shading.shade(data, quiet(k.north()));
    }

    private TileData obtain(TileKey k) {
        int gen;
        TileDisk d;
        synchronized (this) {
            TileData t = tiles.get(k);
            if (t != null) {
                touchLocked(k);
                return t;
            }
            if (disk == null) return null;
            gen = generation;
            d = disk;
        }
        TileData data = absentOnDisk(k) ? null : d.read(k);
        synchronized (this) {
            if (gen != generation) return null;
            TileData t = tiles.get(k);
            if (t != null) return t;
            boolean fresh = data == null;
            if (fresh) data = new TileData();
            tiles.put(k, data);
            saved.put(k, fresh ? -1 : data.version());
            absent.remove(k);
            touchLocked(k);
            return data;
        }
    }

    private synchronized boolean absentOnDisk(TileKey k) {
        return absent.contains(k);
    }

    private void touchLocked(TileKey k) {
        for (TileKey old : lru.touch(k)) {
            TileData data = tiles.remove(old);
            Integer v = saved.remove(old);
            TileDisk d = disk;
            if (data != null && d != null && (v == null || v != data.version())) worker.execute(() -> d.write(old, data));
        }
    }

    private List<Map.Entry<TileKey, TileData>> dirtyLocked() {
        List<Map.Entry<TileKey, TileData>> out = new ArrayList<>();
        for (Map.Entry<TileKey, TileData> e : tiles.entrySet()) {
            Integer v = saved.get(e.getKey());
            int now = e.getValue().version();
            if (v == null || v != now) {
                out.add(Map.entry(e.getKey(), e.getValue()));
                saved.put(e.getKey(), now);
            }
        }
        return out;
    }

    private void clearLocked() {
        tiles.clear();
        saved.clear();
        lru.clear();
        loading.clear();
        absent.clear();
    }
}
