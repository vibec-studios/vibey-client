package dev.henny.vibeyclient.module.waypoints.map;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.waypoints.core.map.CacheFile;
import dev.henny.vibeyclient.module.waypoints.core.map.TileCachePolicy;
import dev.henny.vibeyclient.module.waypoints.core.map.TileCodec;
import dev.henny.vibeyclient.module.waypoints.core.map.TileData;
import dev.henny.vibeyclient.module.waypoints.core.map.TileKey;
import dev.henny.vibeyclient.module.waypoints.data.StoreFiles;
import dev.henny.vibeyclient.module.waypoints.data.WorldContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

final class TileDisk {
    private final Path dir;

    TileDisk(Path dir) {
        this.dir = dir;
    }

    Path dir() {
        return dir;
    }

    Path path(TileKey k) {
        return dir.resolve(WorldContext.safe(k.dimension())).resolve(k.fileName());
    }

    TileData read(TileKey k) {
        Path file = path(k);
        if (!Files.isRegularFile(file)) return null;
        try {
            TileData data = TileCodec.decode(Files.readAllBytes(file));
            Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
            return data;
        } catch (IOException | RuntimeException e) {
            Vibey.LOG.debug("Waypoints: unreadable map tile {}: {}", file, e.toString());
            return null;
        }
    }

    void write(TileKey k, TileData t) {
        try {
            StoreFiles.writeAtomically(path(k), TileCodec.encode(t));
        } catch (IOException e) {
            Vibey.LOG.debug("Waypoints: could not save map tile {}: {}", k, e.toString());
        }
    }

    static long enforce(Path root, long maxBytes, Set<String> keep) {
        if (!Files.isDirectory(root)) return 0;
        List<CacheFile> files = new ArrayList<>();
        try (Stream<Path> s = Files.walk(root)) {
            s.filter(p -> p.toString().endsWith(".vwt")).forEach(p -> {
                try {
                    files.add(new CacheFile(p.toString(), Files.size(p), Files.getLastModifiedTime(p).toMillis()));
                } catch (IOException ignored) {
                    files.add(new CacheFile(p.toString(), 0, 0));
                }
            });
        } catch (IOException e) {
            return 0;
        }
        for (CacheFile f : TileCachePolicy.evict(files, maxBytes, keep)) {
            try {
                Files.deleteIfExists(Path.of(f.path()));
            } catch (IOException ignored) {
                Vibey.LOG.debug("Waypoints: could not delete {}", f.path());
            }
        }
        return TileCachePolicy.total(files);
    }
}
