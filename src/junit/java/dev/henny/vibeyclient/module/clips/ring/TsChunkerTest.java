package dev.henny.vibeyclient.module.clips.ring;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The GOP chunker and the RAM ring on a synthetic MPEG-TS stream shaped like FFmpeg's (PAT, PMT, H.264 PES). */
class TsChunkerTest {
    static final int VIDEO_PID = 0x100, PMT_PID = 0x1000;

    /** {@code gops} × {@code perGop} frames at 30 fps, keyframe first in each GOP, 2 packets per frame. */
    static byte[] stream(int gops, int perGop, long startPts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int cc = 0;
        for (int i = 0; i < gops * perGop; i++) {
            boolean key = i % perGop == 0;
            if (key) {
                out.writeBytes(pat());
                out.writeBytes(pmt());
            }
            long pts = startPts + i * 3000L;
            out.writeBytes(pes(pts, key, cc++));
            out.writeBytes(filler(cc++));
        }
        return out.toByteArray();
    }

    static byte[] packet(int pid, boolean start, int cc) {
        byte[] p = new byte[188];
        java.util.Arrays.fill(p, (byte) 0xFF);
        p[0] = 0x47;
        p[1] = (byte) ((start ? 0x40 : 0) | (pid >> 8));
        p[2] = (byte) pid;
        p[3] = (byte) (0x10 | (cc & 0xF));
        return p;
    }

    static byte[] pat() {
        byte[] p = packet(0, true, 0);
        int[] s = {0, 0x00, 0xB0, 13, 0, 1, 0xC1, 0, 0, 0, 1, 0xE0 | (PMT_PID >> 8), PMT_PID & 0xFF, 1, 2, 3, 4};
        for (int i = 0; i < s.length; i++) p[4 + i] = (byte) s[i];
        return p;
    }

    static byte[] pmt() {
        byte[] p = packet(PMT_PID, true, 0);
        int[] s = {0, 0x02, 0xB0, 18, 0, 1, 0xC1, 0, 0, 0xE0 | (VIDEO_PID >> 8), VIDEO_PID & 0xFF, 0xF0, 0,
                0x1B, 0xE0 | (VIDEO_PID >> 8), VIDEO_PID & 0xFF, 0xF0, 0, 1, 2, 3, 4};
        for (int i = 0; i < s.length; i++) p[4 + i] = (byte) s[i];
        return p;
    }

    static byte[] pes(long pts, boolean key, int cc) {
        byte[] p = packet(VIDEO_PID, true, cc);
        int o = 4;
        if (key) {
            p[3] = (byte) (0x30 | (cc & 0xF));
            p[4] = 1;
            p[5] = 0x40; // random_access_indicator
            o = 6;
        }
        int[] h = {0, 0, 1, 0xE0, 0, 0, 0x80, 0x80, 5,
                (int) (0x21 | ((pts >> 29) & 0x0E)), (int) ((pts >> 22) & 0xFF), (int) (((pts >> 14) & 0xFE) | 1),
                (int) ((pts >> 7) & 0xFF), (int) (((pts << 1) & 0xFE) | 1)};
        for (int i = 0; i < h.length; i++) p[o + i] = (byte) h[i];
        return p;
    }

    static byte[] filler(int cc) {
        return packet(VIDEO_PID, false, cc);
    }

    static void feedRandomly(TsChunker c, byte[] data, long seed) {
        Random r = new Random(seed);
        int off = 0;
        while (off < data.length) {
            int n = Math.min(data.length - off, 1 + r.nextInt(5000));
            c.feed(data, off, n);
            off += n;
        }
    }

    @Test
    void cutsAtKeyframesAndMeasuresDurations() {
        ClipRing ring = new ClipRing(60_000, 1 << 30);
        TsChunker c = new TsChunker(ring, 1);
        feedRandomly(c, stream(3, 60, 900_000), 42);
        // two GOPs closed, the third still open
        assertEquals(3, ring.chunks());
        List<Chunk.View> all = ring.last(Long.MAX_VALUE);
        assertEquals(3, all.size());
        assertEquals(2000, all.get(0).durationMs());
        assertEquals(2000, all.get(1).durationMs());
        assertEquals(59 * 3000 / 90, all.get(2).durationMs()); // open: up to the newest frame start
        // the open chunk's view stops before its newest (maybe incomplete) frame
        assertEquals(59 * 2 * 188, all.get(2).bytes(), "59 whole frames (PAT/PMT before the keyframe belong to the previous chunk)");
        c.end();
        List<Chunk.View> closed = ring.last(Long.MAX_VALUE);
        assertEquals(2000, closed.get(2).durationMs());
    }

    @Test
    void lastTakesWholeChunksFromTheNewest() {
        ClipRing ring = new ClipRing(60_000, 1 << 30);
        TsChunker c = new TsChunker(ring, 1);
        byte[] s5 = stream(5, 60, 0);
        c.feed(s5, 0, s5.length);
        c.end();
        List<Chunk.View> v = ring.last(3000);
        assertEquals(2, v.size(), "3 s = the newest 2 chunks (4 s, starting at a keyframe)");
        assertTrue(ClipRing.durationMs(v) >= 3000);
    }

    @Test
    void ringKeepsLengthPlusHeadroomAndRespectsTheByteCap() {
        ClipRing ring = new ClipRing(4000, 1 << 30);
        TsChunker c = new TsChunker(ring, 1);
        c.feed(stream(6, 60, 0), 0, stream(6, 60, 0).length);
        // closed chunks kept while the rest without the oldest still covers 4 s
        long closedMs = ring.durationMs() - ring.last(1).get(0).durationMs();
        assertTrue(closedMs >= 4000 && closedMs < 6000, "kept " + closedMs);
        assertTrue(ring.droppedChunks() > 0);
        long bytesBefore = ring.bytes();
        ring.limits(4000, 60 * 2 * 188 + 4 * 188); // about one chunk
        assertTrue(ring.bytes() < bytesBefore);
        assertTrue(ring.chunks() <= 2);
        ring.clear();
        assertEquals(0, ring.bytes());
        assertEquals(0, ring.chunks());
    }

    @Test
    void partFilesStartWithPatAndPmt() throws Exception {
        ClipRing ring = new ClipRing(60_000, 1 << 30);
        TsChunker c = new TsChunker(ring, 7);
        byte[] s = stream(2, 30, 0);
        c.feed(s, 0, s.length);
        c.end();
        List<Chunk.View> v = ring.last(Long.MAX_VALUE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(v.get(0).headers());
        assertTrue(v.get(0).writeTo(out));
        byte[] b = out.toByteArray();
        assertEquals(0x47, b[0] & 0xFF);
        assertEquals(0, Ts.pid(b, 0), "PAT first");
        assertEquals(PMT_PID, Ts.pid(b, 188), "PMT second");
        assertEquals(7, v.get(0).session());
        assertEquals(0, b.length % 188);
    }

    @Test
    void droppedChunksAreNotWrittenAnyMore() throws Exception {
        ClipRing ring = new ClipRing(60_000, 1 << 30);
        TsChunker c = new TsChunker(ring, 1);
        byte[] s = stream(2, 30, 0);
        c.feed(s, 0, s.length);
        List<Chunk.View> v = ring.last(Long.MAX_VALUE);
        ring.clear();
        assertFalse(v.get(0).writeTo(new ByteArrayOutputStream()));
    }

    @Test
    void ptsWrapsAround() {
        ClipRing ring = new ClipRing(60_000, 1 << 30);
        TsChunker c = new TsChunker(ring, 1);
        byte[] s = stream(2, 60, Ts.PTS_MASK - 60_000); // wraps inside the first GOP
        c.feed(s, 0, s.length);
        c.end();
        for (Chunk.View v : ring.last(Long.MAX_VALUE)) assertEquals(2000, v.durationMs());
    }
}
