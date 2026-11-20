package dev.henny.vibeyclient.module.waypoints.map;

import dev.henny.vibeyclient.module.waypoints.core.map.ChunkSample;
import dev.henny.vibeyclient.module.waypoints.core.map.ScanOrder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

final class ChunkScanner {
    static final int MAX_RADIUS = 16;
    static final int CHECKS_PER_TICK = 96;
    static final int SAMPLES_PER_TICK = 4;
    static final long BUDGET_NANOS = 1_500_000L;

    private final ColumnSampler sampler = new ColumnSampler();
    private final Map<Long, Long> scanned = new HashMap<>();
    private final ScanStats stats = new ScanStats();
    private ScanOrder order = new ScanOrder(0);
    private int cursor;
    private String dimension;

    ScanStats stats() {
        return stats;
    }

    void reset() {
        scanned.clear();
        cursor = 0;
        dimension = null;
    }

    void tick(ClientLevel level, Player player, String dim, int renderDistance, Consumer<ChunkSample> out) {
        if (!dim.equals(dimension)) {
            reset();
            dimension = dim;
        }
        int radius = Math.max(1, Math.min(MAX_RADIUS, renderDistance));
        if (order.radius() != radius) {
            order = new ScanOrder(radius);
            cursor = 0;
        }
        if (scanned.size() > 16384) scanned.clear();
        ChunkPos center = player.chunkPosition();
        long start = System.nanoTime();
        long now = level.getGameTime();
        int samples = 0;
        for (int i = 0; i < CHECKS_PER_TICK && samples < SAMPLES_PER_TICK; i++) {
            if (System.nanoTime() - start > BUDGET_NANOS) break;
            int dx = order.dx(cursor), dz = order.dz(cursor);
            cursor = (cursor + 1) % order.size();
            int cx = center.x + dx, cz = center.z + dz;
            long key = ChunkPos.asLong(cx, cz);
            Long at = scanned.get(key);
            if (!ScanOrder.due(at == null ? -1 : at, now, Math.max(Math.abs(dx), Math.abs(dz)))) continue;
            LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
            if (chunk == null || chunk.isEmpty()) continue;
            out.accept(sampler.sample(level, chunk, dim, player.getBlockY()));
            scanned.put(key, now);
            samples++;
        }
        stats.record(samples, System.nanoTime() - start);
    }
}
