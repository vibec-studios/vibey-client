package dev.henny.vibeyclient.module.clips.ring;

import java.io.IOException;
import java.io.OutputStream;

/**
 * One GOP of MPEG-TS (starts with a keyframe), held in pooled 64 KiB blocks or a buffer file. Written by the chunker thread while
 * open; once {@link #close closed} it never changes. {@link #snapshot()} gives a consistent view of an open chunk up to
 * the start of its newest frame (so a save never ends in a half-written frame).
 */
public final class Chunk {
    final int session;
    final byte[] headers; // PAT + PMT of the session, put in front of every part file
    private final ChunkBody body;
    private long size;
    final long firstPts;
    private long lastPesStart;
    private long lastPesPts;
    private int frames;
    private volatile long durationPts = -1;
    private volatile boolean released;

    Chunk(int session, byte[] headers, long firstPts, ChunkBody body) {
        this.session = session;
        this.headers = headers;
        this.firstPts = firstPts;
        this.lastPesPts = firstPts;
        this.body = body;
    }

    public int session() {
        return session;
    }

    synchronized void append(byte[] src, int off, int len) {
        if (released) return; // the ring was cleared while the encoder still delivered
        body.append(src, off, len);
        size = body.size();
    }

    /** A video PES (one frame) starts at the current end with {@code pts}. */
    synchronized void markFrame(long pts) {
        lastPesStart = size;
        lastPesPts = pts;
        frames++;
    }

    synchronized void close(long endPts) {
        durationPts = Math.max(0, Ts.ptsDiff(endPts, firstPts));
        body.seal();
        size = body.size();
    }

    public boolean closed() {
        return durationPts >= 0;
    }

    public synchronized long bytes() {
        return size;
    }

    public synchronized int frames() {
        return frames;
    }

    /** Duration in ms: exact when closed, else up to the newest frame start. */
    public synchronized long durationMs() {
        long d = durationPts >= 0 ? durationPts : Ts.ptsDiff(lastPesPts, firstPts);
        return d / 90;
    }

    /** What a save may use of this chunk right now. */
    public synchronized View snapshot() {
        long end = durationPts >= 0 ? size : lastPesStart;
        long ms = durationPts >= 0 ? durationPts / 90 : Ts.ptsDiff(lastPesPts, firstPts) / 90;
        return new View(this, end, ms);
    }

    /** Returns the blocks to the pool (the ring dropped it). Views taken earlier must not be written afterwards. */
    synchronized void release() {
        if (released) return;
        released = true;
        body.release();
    }

    synchronized boolean writeTo(OutputStream out, long end) throws IOException {
        if (released) return false;
        return body.writeTo(out, Math.min(end, body.size()));
    }

    /** A fixed byte range of a chunk. */
    public record View(Chunk chunk, long bytes, long durationMs) {
        public int session() {
            return chunk.session;
        }

        public byte[] headers() {
            return chunk.headers;
        }

        /** Writes the range; false if the ring already dropped the chunk. */
        public boolean writeTo(OutputStream out) throws IOException {
            return chunk.writeTo(out, bytes);
        }
    }
}
