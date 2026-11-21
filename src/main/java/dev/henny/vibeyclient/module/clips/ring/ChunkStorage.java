package dev.henny.vibeyclient.module.clips.ring;

import java.io.IOException;
import java.nio.file.Path;

public interface ChunkStorage {
    ChunkBody create(int session);

    void clear();

    boolean onDisk();

    IOException error();

    static ChunkStorage ram() {
        return new RamStorage();
    }

    static ChunkStorage disk(Path dir) {
        return new DiskStorage(dir);
    }
}
