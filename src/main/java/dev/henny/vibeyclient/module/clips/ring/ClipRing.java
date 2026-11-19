package dev.henny.vibeyclient.module.clips.ring;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The replay buffer in RAM: encoded GOP chunks, oldest first. Keeps at least {@code keepMs} of closed chunks (the clip
 * length plus one chunk of headroom) and never more than {@code maxBytes}. Thread-safe: the chunker thread adds,
 * saves read snapshots from other threads.
 */
public final class ClipRing {
    private final BlockPool pool = new BlockPool();
    private final ArrayDeque<Chunk> closed = new ArrayDeque<>();
    private Chunk open;
    private volatile long keepMs;
    private volatile long maxBytes;
    private long closedBytes;
    private long closedMs;
    private long dropped;

    public ClipRing(long keepMs, long maxBytes) {
        this.keepMs = keepMs;
        this.maxBytes = maxBytes;
    }

    public synchronized void limits(long keepMs, long maxBytes) {
        this.keepMs = keepMs;
        this.maxBytes = maxBytes;
        prune();
    }

    synchronized Chunk open(int session, byte[] headers, long firstPts) {
        Chunk c = new Chunk(session, headers, firstPts, pool);
        open = c;
        return c;
    }

    synchronized void closed(Chunk c) {
        if (open == c) open = null;
        closed.addLast(c);
        closedBytes += c.bytes();
        closedMs += c.durationMs();
        prune();
    }

    private void prune() {
        while (closed.size() > 1) {
            Chunk oldest = closed.peekFirst();
            boolean tooLong = closedMs - oldest.durationMs() >= keepMs;
            boolean tooBig = closedBytes + openBytes() > maxBytes;
            if (!tooLong && !tooBig) break;
            drop(closed.pollFirst());
        }
    }

    private long openBytes() {
        return open == null ? 0 : open.bytes();
    }

    private void drop(Chunk c) {
        closedBytes -= c.bytes();
        closedMs -= c.durationMs();
        dropped++;
        c.release();
    }

    /** Throws everything away (buffer off, server rule). */
    public synchronized void clear() {
        while (!closed.isEmpty()) drop(closed.pollFirst());
        if (open != null) open.release();
        open = null;
        pool.clear();
    }

    /** Bytes held (closed + open chunk). */
    public synchronized long bytes() {
        return closedBytes + openBytes();
    }

    /** Recorded time held, including the open chunk up to its newest frame. */
    public synchronized long durationMs() {
        return closedMs + (open == null ? 0 : open.durationMs());
    }

    public synchronized int chunks() {
        return closed.size() + (open == null ? 0 : 1);
    }

    public synchronized long droppedChunks() {
        return dropped;
    }

    /**
     * The newest {@code lengthMs} (or a little more: whole chunks, starting at a keyframe), oldest first, including the
     * open chunk up to its newest complete frame. Empty when nothing is recorded.
     */
    public synchronized List<Chunk.View> last(long lengthMs) {
        List<Chunk.View> rev = new ArrayList<>();
        long sum = 0;
        if (open != null) {
            Chunk.View v = open.snapshot();
            if (v.bytes() > 0) {
                rev.add(v);
                sum += v.durationMs();
            }
        }
        Iterator<Chunk> it = closed.descendingIterator();
        while (it.hasNext() && sum < lengthMs) {
            Chunk c = it.next();
            Chunk.View v = c.snapshot();
            rev.add(v);
            sum += v.durationMs();
        }
        List<Chunk.View> out = new ArrayList<>(rev.size());
        for (int i = rev.size() - 1; i >= 0; i--) out.add(rev.get(i));
        return out;
    }

    /** Sum of the views' durations. */
    public static long durationMs(List<Chunk.View> views) {
        long s = 0;
        for (Chunk.View v : views) s += v.durationMs();
        return s;
    }
}
