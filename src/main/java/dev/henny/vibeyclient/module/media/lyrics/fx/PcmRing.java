package dev.henny.vibeyclient.module.media.lyrics.fx;

/**
 * Mono samples (-1 … 1) keyed by their absolute frame number: one thread writes (the audio thread or the loopback
 * reader), one reads (the analysis thread). The reader asks for the window that ends at a given frame (what is
 * audible now); if it is not (or no longer) in the buffer it gets false. No allocation after construction.
 */
public final class PcmRing {
    private final float[] buf;
    private final int mask;
    /** Absolute frame after the last written sample. */
    private volatile long end;
    /** First frame that was written since the last {@link #reset}. */
    private volatile long start;
    private volatile long writeNanos;
    private volatile int rate = 44100;

    public PcmRing(int sizePow2) {
        if (Integer.bitCount(sizePow2) != 1) throw new IllegalArgumentException("size must be a power of two");
        buf = new float[sizePow2];
        mask = sizePow2 - 1;
    }

    /** Writer: the next written sample is frame {@code frame} (start, seek). */
    public void reset(long frame, int sampleRate) {
        rate = sampleRate;
        start = frame;
        end = frame;
    }

    /** Writer: appends {@code frames} frames of 16-bit interleaved PCM, mixed down to mono. */
    public void write(short[] interleaved, int frames, int channels) {
        long e = end;
        float k = 1f / (32768f * channels);
        for (int f = 0; f < frames; f++) {
            int sum = 0;
            int o = f * channels;
            for (int c = 0; c < channels; c++) sum += interleaved[o + c];
            buf[(int) ((e + f) & mask)] = sum * k;
        }
        end = e + frames;
        writeNanos = System.nanoTime();
    }

    /** Writer: appends mono 16-bit little-endian samples. */
    public void writeLe16(byte[] bytes, int count) {
        long e = end;
        for (int i = 0; i < count; i++) {
            int lo = bytes[i * 2] & 0xFF;
            int hi = bytes[i * 2 + 1];
            buf[(int) ((e + i) & mask)] = (short) ((hi << 8) | lo) / 32768f;
        }
        end = e + count;
        writeNanos = System.nanoTime();
    }

    public long end() {
        return end;
    }

    public int rate() {
        return rate;
    }

    /** System.nanoTime() of the last write. */
    public long writeNanos() {
        return writeNanos;
    }

    /**
     * Copies the {@code n} samples that end at {@code endFrame} into {@code out}. Samples before the last reset are
     * zeros. False if {@code endFrame} has not been written yet or the window was already overwritten.
     */
    public boolean read(long endFrame, float[] out, int n) {
        long e = end;
        long s = start;
        // keep a quarter of the buffer free so the writer can't overwrite what is being copied
        if (endFrame > e || endFrame < s || e - (endFrame - n) > buf.length - buf.length / 4) return false;
        long from = endFrame - n;
        for (int i = 0; i < n; i++) {
            long f = from + i;
            out[i] = f < s ? 0f : buf[(int) (f & mask)];
        }
        return end - from <= buf.length;
    }
}
