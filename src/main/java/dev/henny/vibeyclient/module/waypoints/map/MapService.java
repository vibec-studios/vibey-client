package dev.henny.vibeyclient.module.waypoints.map;

import dev.henny.vibeyclient.module.waypoints.data.StoreFiles;
import dev.henny.vibeyclient.module.waypoints.data.WorldContext;
import net.minecraft.client.Minecraft;

public final class MapService {
    static final long SAVE_EVERY_MS = 30_000;

    private final TileWorker worker = new TileWorker();
    private final TileStore store = new TileStore(worker);
    private final TileTextures textures = new TileTextures(store);
    private final ChunkScanner scanner = new ChunkScanner();
    private String dimension;
    private long maxBytes;
    private long lastSave;
    private volatile long cacheBytes;

    public void open(String worldKey, long maxBytes) {
        this.maxBytes = maxBytes;
        store.open(new TileDisk(StoreFiles.maps(worldKey)));
        scanner.reset();
        dimension = null;
        lastSave = System.currentTimeMillis();
        worker.execute(this::enforceLimit);
    }

    public void close() {
        store.close();
        textures.clear();
        scanner.reset();
        dimension = null;
        worker.execute(this::enforceLimit);
    }

    public void tick(Minecraft mc) {
        String dim = WorldContext.dimension(mc.level);
        if (!dim.equals(dimension)) {
            textures.clear();
            dimension = dim;
        }
        scanner.tick(mc.level, mc.player, dim, mc.options.getEffectiveRenderDistance(), s -> worker.execute(() -> store.apply(s)));
        long now = System.currentTimeMillis();
        if (now - lastSave > SAVE_EVERY_MS) {
            lastSave = now;
            worker.execute(store::saveDirty);
            worker.execute(this::enforceLimit);
        }
    }

    public String dimension() {
        return dimension;
    }

    public TileTextures textures() {
        return textures;
    }

    public ScanStats scanStats() {
        return scanner.stats();
    }

    public int queuedTasks() {
        return worker.queued();
    }

    public int loadedTiles() {
        return store.loadedCount();
    }

    public long cacheBytes() {
        return cacheBytes;
    }

    public void flushForTest() {
        worker.execute(store::saveDirty);
    }

    private void enforceLimit() {
        if (maxBytes > 0) cacheBytes = TileDisk.enforce(StoreFiles.mapsRoot(), maxBytes, store.openFiles());
    }
}
