package dev.henny.vibeyclient.module.clips.ring;

import java.util.ArrayDeque;

/** Reuses the 64 KiB blocks chunks are stored in, so a running buffer makes almost no garbage. */
final class BlockPool {
    static final int BLOCK = 64 * 1024;
    private static final int MAX_FREE = 64; // 4 MiB of spare blocks at most
    private final ArrayDeque<byte[]> free = new ArrayDeque<>();

    synchronized byte[] take() {
        byte[] b = free.pollFirst();
        return b != null ? b : new byte[BLOCK];
    }

    synchronized void give(byte[] b) {
        if (free.size() < MAX_FREE) free.addFirst(b);
    }

    synchronized void clear() {
        free.clear();
    }
}
