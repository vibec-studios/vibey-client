package dev.henny.vibeyclient.module.media.lyrics.fx;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The music analysis on the synthetic test track: beats, tempo, the drop, levels, and no allocation per hop. */
class AnalysisTest {
    private static final int HOP = 512;

    @Test
    void findsTheBeatTheTempoAndTheDrop() {
        float[] song = TestTrack.samples();
        Analysis a = new Analysis(TestTrack.RATE, 2048);
        float[] win = new float[2048];
        List<Double> onsets = new ArrayList<>();
        List<Double> drops = new ArrayList<>();
        double bassBefore = 0;
        int nBefore = 0;
        double bassAfter = 0;
        int nAfter = 0;
        for (int end = 2048; end <= song.length; end += HOP) {
            System.arraycopy(song, end - 2048, win, 0, 2048);
            double t = end / (double) TestTrack.RATE;
            a.process(win, t);
            if (a.onset) onsets.add(t);
            if (a.drop) drops.add(t);
            if (t > 3 && t < 7.5) {
                bassBefore += a.bass;
                nBefore++;
            }
            if (t > 10) {
                bassAfter += a.bass;
                nAfter++;
            }
        }
        bassBefore /= nBefore;
        bassAfter /= nAfter;
        long afterDrop = onsets.stream().filter(t -> t > 9).count();
        // 120 bpm for 11 s after the drop: ~22 kicks
        assertTrue(afterDrop >= 16 && afterDrop <= 30, "onsets after the drop: " + afterDrop + " " + onsets);
        assertEquals(120, a.bpm, 6, "bpm");
        assertEquals(1, drops.size(), "drops " + drops);
        assertTrue(drops.get(0) > 7.9 && drops.get(0) < 9.5, "drop at " + drops.get(0));
        assertTrue(bassAfter > bassBefore + 0.25, "bass before " + bassBefore + " after " + bassAfter);
    }

    @Test
    void silenceFadesOut() {
        Analysis a = new Analysis(TestTrack.RATE, 2048);
        float[] song = TestTrack.samples();
        float[] win = new float[2048];
        int start = (int) (12 * TestTrack.RATE);
        for (int i = 0; i < 100; i++) {
            System.arraycopy(song, start + i * HOP, win, 0, 2048);
            a.process(win, i * 0.0116);
        }
        assertTrue(a.energy > 0.3f, "energy " + a.energy);
        for (int i = 100; i < 300; i++) a.silence(i * 0.0116);
        assertTrue(a.energy < 0.02f && a.bass < 0.02f, "after silence " + a.energy + " " + a.bass);
    }

    @Test
    void ringGivesTheAudibleWindow() {
        PcmRing ring = new PcmRing(1 << 12);
        ring.reset(1000, 44100);
        short[] chunk = new short[256 * 2];
        for (int i = 0; i < 256; i++) {
            chunk[i * 2] = (short) (i * 100);
            chunk[i * 2 + 1] = (short) (i * 100);
        }
        ring.write(chunk, 256, 2);
        float[] out = new float[64];
        assertTrue(ring.read(1256, out, 64));
        assertEquals(192 * 100 / 32768f, out[0], 1e-4);
        assertTrue(!ring.read(1300, out, 64), "not written yet");
        assertTrue(ring.read(1010, out, 64), "partly before the reset reads zeros");
        assertEquals(0f, out[0]);
    }

    @Test
    void noAllocationPerHop() {
        var bean = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        Analysis a = new Analysis(TestTrack.RATE, 2048);
        float[] song = TestTrack.samples();
        float[] win = new float[2048];
        // warm up (JIT, first onsets)
        for (int end = 2048; end < song.length / 2; end += HOP) {
            System.arraycopy(song, end - 2048, win, 0, 2048);
            a.process(win, end / 44100.0);
        }
        long before = bean.getCurrentThreadAllocatedBytes();
        long t0 = System.nanoTime();
        int hops = 0;
        for (int end = song.length / 2; end <= song.length; end += HOP) {
            System.arraycopy(song, end - 2048, win, 0, 2048);
            a.process(win, end / 44100.0);
            hops++;
        }
        long took = System.nanoTime() - t0;
        long allocated = bean.getCurrentThreadAllocatedBytes() - before;
        System.out.printf("[media-fx] %d hops, %.1f us per hop, %d bytes allocated%n", hops, took / 1000.0 / hops, allocated);
        assertTrue(allocated < 1024, "allocated " + allocated + " bytes in " + hops + " hops");
        assertTrue(took / hops < 2_000_000, "hop took " + took / hops + " ns");
    }
}
