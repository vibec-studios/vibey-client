package dev.henny.vibeyclient.module.media.lyrics.fx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A short synthetic test song for the sky effects (tests, DevTour; never shipped as a file): 120 bpm, a quiet build
 * with a pad and soft hats for {@link #DROP_SECONDS} seconds, then the drop – kick on every beat, a bass line and
 * brighter chords. Made from sine waves and noise, so it is original and tiny to generate.
 */
public final class TestTrack {
    public static final int RATE = 44100;
    public static final double BPM = 120;
    public static final double DROP_SECONDS = 8.0;
    public static final double LENGTH_SECONDS = 20.0;

    private TestTrack() {}

    /** Mono samples, -1 … 1. */
    public static float[] samples() {
        int n = (int) (RATE * LENGTH_SECONDS);
        float[] out = new float[n];
        double beat = 60.0 / BPM;
        long seed = 12345;
        double[] chord = {220.0, 277.18, 329.63};
        double[] bassNotes = {55.0, 55.0, 65.41, 49.0};
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            boolean drop = t >= DROP_SECONDS;
            double pos = t / beat;
            double inBeat = (pos - Math.floor(pos)) * beat;
            double s = 0;
            // pad, swelling up to the drop
            double padAmp = drop ? 0.10 : 0.04 + 0.05 * (t / DROP_SECONDS);
            for (double f : chord) s += padAmp * Math.sin(2 * Math.PI * f * (drop ? 2 : 1) * t) / chord.length;
            // hats on the off-beats
            double half = (pos * 2 - Math.floor(pos * 2)) * beat / 2;
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            double noise = ((seed >>> 33) / (double) (1L << 31)) * 2 - 1;
            if (half < 0.03) s += noise * (drop ? 0.08 : 0.03) * (1 - half / 0.03);
            if (drop) {
                // kick: a falling sine with a fast decay
                if (inBeat < 0.25) {
                    double f = 45 + 110 * Math.exp(-inBeat * 30);
                    s += 0.75 * Math.sin(2 * Math.PI * f * inBeat) * Math.exp(-inBeat * 12);
                }
                int bar = (int) (pos / 4) % bassNotes.length;
                s += 0.22 * Math.sin(2 * Math.PI * bassNotes[bar] * t) * (0.6 + 0.4 * Math.exp(-inBeat * 6));
            }
            out[i] = (float) Math.max(-1, Math.min(1, s));
        }
        return out;
    }

    /** Writes the song as a 16-bit mono WAV plus a matching .lrc next to it ({@code <name>.wav}, {@code <name>.lrc}). */
    public static void write(Path dir, String name) throws IOException {
        float[] s = samples();
        ByteBuffer b = ByteBuffer.allocate(44 + s.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes()).putInt(36 + s.length * 2).put("WAVE".getBytes());
        b.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1).putInt(RATE).putInt(RATE * 2)
                .putShort((short) 2).putShort((short) 16);
        b.put("data".getBytes()).putInt(s.length * 2);
        for (float v : s) b.putShort((short) Math.round(v * 32767));
        Files.createDirectories(dir);
        Files.write(dir.resolve(name + ".wav"), b.array());
        String lrc = "[ti:Sky Test]\n[ar:Vibey Media]\n"
                + "[00:00.50]Hold on, the sky is listening\n"
                + "[00:04.00]Count it down with me\n"
                + "[00:07.00]Here it comes\n"
                + "[00:08.00]Drop!\n"
                + "[00:12.00]Feel the colors move\n"
                + "[00:16.00]Stars on every beat\n"
                + "[00:19.50]\n";
        Files.writeString(dir.resolve(name + ".lrc"), lrc);
    }
}
