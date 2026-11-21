package dev.henny.vibeyclient.module.clips.ring;

import java.io.IOException;

final class RamStorage implements ChunkStorage {
    private final BlockPool pool = new BlockPool();

    @Override
    public ChunkBody create(int session) {
        return new RamBody(pool);
    }

    @Override
    public void clear() {
        pool.clear();
    }

    @Override
    public boolean onDisk() {
        return false;
    }

    @Override
    public IOException error() {
        return null;
    }
}
