package dev.henny.vibeyclient.module.media.lyrics.fx;

import dev.henny.vibeyclient.module.media.lyrics.LyricsConfig;
import dev.henny.vibeyclient.module.media.lyrics.LyricsFeature;
import dev.henny.vibeyclient.module.media.lyrics.Playback;
import dev.henny.vibeyclient.module.media.lyrics.audio.AudioPlayer;
import dev.henny.vibeyclient.module.media.lyrics.lyrics.Lyrics;

import java.util.Locale;
import java.util.concurrent.locks.LockSupport;

/**
 * Listens to the music that plays and publishes its features for the sky ({@link SkyFx}). Runs on its own daemon
 * thread at ~86 hops per second while the sky effects are on and something plays, and stops itself after a while
 * of silence. Sources, best first: the song from the folder (decoded PCM, the window that is audible now), the PC's
 * sound through WASAPI loopback ({@link LoopbackCapture}, Windows, music from the PC), the lyrics timing (word and
 * line starts as soft beats). The client thread only hands over a snapshot in {@link #tick()}; the worker never
 * touches game state and allocates nothing per hop.
 */
public final class MusicAnalyzer {
    public enum Source { NONE, SONG, PC, LYRICS }

    private static final long HOP_NANOS = 11_610_000L;
    private static final long IDLE_STOP_NANOS = 15_000_000_000L;

    // client thread -> worker (snapshot in tick)
    private static volatile AudioPlayer player;
    private static volatile boolean playing;
    private static volatile boolean pcMode;
    private static volatile boolean wantPcAudio;
    private static volatile Lyrics lyrics;
    private static volatile long lyricMs;
    private static volatile long lyricNanos;

    // worker -> render thread
    public static volatile Source source = Source.NONE;
    public static volatile float bass;
    public static volatile float mid;
    public static volatile float high;
    public static volatile float energy;
    /** Counts up by one per beat/onset; the renderer notices changes. */
    public static volatile int onsets;
    public static volatile float onsetStrength;
    /** Counts up by one per drop. */
    public static volatile int drops;
    public static volatile float bpm;

    // statistics (worker): time and allocated bytes per hop
    public static volatile long hops;
    public static volatile double hopMicrosAvg;
    public static volatile double hopMicrosMax;
    public static volatile long allocatedBytes;

    private static Thread thread;
    private static volatile boolean running;
    private static volatile long lastActiveNanos;

    private MusicAnalyzer() {}

    public static boolean systemAudioSupported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /** Client tick: snapshot of what plays, starts/stops the worker. */
    public static void tick() {
        LyricsConfig cfg = LyricsConfig.get();
        boolean on = cfg.skyFx && cfg.fxStrength > 0 && LyricsFeature.running();
        player = Playback.audio();
        playing = Playback.isPlaying();
        pcMode = Playback.isPc();
        wantPcAudio = on && pcMode && cfg.fxPcAudio && systemAudioSupported();
        lyrics = Playback.lyrics();
        lyricMs = Playback.lyricTimeMs();
        lyricNanos = System.nanoTime();
        LoopbackCapture.want(wantPcAudio && playing);
        if (on && playing) {
            lastActiveNanos = System.nanoTime();
            start();
        }
    }

    public static synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(MusicAnalyzer::loop, "Vibey Media analysis");
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        thread.start();
    }

    /** Feature off / game closing. */
    public static synchronized void stopAll() {
        running = false;
        if (thread != null) thread.interrupt();
        thread = null;
        LoopbackCapture.want(false);
        LoopbackCapture.stopNow();
        source = Source.NONE;
        bass = mid = high = energy = 0f;
    }

    // ============================================================================================ worker

    private static void loop() {
        Analysis song = null;
        Analysis pc = null;
        Analysis words = new Analysis(12000, 512);
        float[] window = new float[2048];
        long lastLyricT = Long.MIN_VALUE;
        double timeBase = System.nanoTime() / 1e9;
        com.sun.management.ThreadMXBean mx = threadBean();
        long next = System.nanoTime();
        double avg = 0;
        double max = 0;
        long count = 0;
        while (running) {
            long now = System.nanoTime();
            if (!playing && now - lastActiveNanos > IDLE_STOP_NANOS) break;
            long allocBefore = mx != null ? mx.getCurrentThreadAllocatedBytes() : 0;
            double t = now / 1e9 - timeBase;
            Analysis used;
            Source src = Source.NONE;
            AudioPlayer p = player;
            if (p != null && playing) {
                int rate = p.sampleRate();
                int n = rate > 32000 ? 2048 : 1024;
                if (song == null || song.rate() != rate || song.size() != n) song = new Analysis(rate, n);
                used = song;
                src = Source.SONG;
                if (p.ring().read(p.positionFrames(), window, n)) song.process(window, t);
                else song.silence(t);
            } else if (pcMode && playing && LoopbackCapture.fresh()) {
                PcmRing ring = LoopbackCapture.ring();
                int rate = ring.rate();
                int n = rate > 32000 ? 2048 : rate > 16000 ? 1024 : 512;
                if (pc == null || pc.rate() != rate || pc.size() != n) pc = new Analysis(rate, n);
                used = pc;
                src = Source.PC;
                if (ring.read(ring.end(), window, n)) pc.process(window, t);
                else pc.silence(t);
            } else if (playing && lyrics != null) {
                used = words;
                src = Source.LYRICS;
                long lt = lyricMs + (System.nanoTime() - lyricNanos) / 1_000_000L;
                float strength = 0f;
                float inLine = 0f;
                Lyrics l = lyrics;
                int idx = l.lineAt(lt);
                if (idx >= 0) {
                    Lyrics.Line line = l.lines().get(idx);
                    if (!line.isBreak() && lt < line.end()) inLine = 1f;
                    if (lastLyricT != Long.MIN_VALUE && lt > lastLyricT && lt - lastLyricT < 500) {
                        if (line.start() > lastLyricT && line.start() <= lt && !line.isBreak()) strength = 0.9f;
                        for (int i = 0; i < line.words().size(); i++) {
                            long ws = line.words().get(i).start();
                            if (ws > lastLyricT && ws <= lt) strength = Math.max(strength, 0.55f);
                        }
                    }
                }
                lastLyricT = lt;
                float b = Math.max(0.15f + 0.25f * inLine, strength);
                words.external(t, strength, b, 0.3f + 0.35f * inLine);
            } else {
                used = song != null ? song : words;
                used.silence(t);
            }
            publish(used, src);
            long took = System.nanoTime() - now;
            if (mx != null) allocatedBytes += Math.max(0, mx.getCurrentThreadAllocatedBytes() - allocBefore);
            count++;
            double us = took / 1000.0;
            avg = count == 1 ? us : avg * 0.98 + us * 0.02;
            max = Math.max(max * 0.9995, us);
            hops = count;
            hopMicrosAvg = avg;
            hopMicrosMax = max;
            next += HOP_NANOS;
            long sleep = next - System.nanoTime();
            if (sleep > 0) LockSupport.parkNanos(sleep);
            else next = System.nanoTime();
            if (Thread.interrupted()) break;
        }
        synchronized (MusicAnalyzer.class) {
            if (thread == Thread.currentThread()) {
                running = false;
                thread = null;
            }
        }
        source = Source.NONE;
    }

    private static void publish(Analysis a, Source src) {
        source = src;
        bass = a.bass;
        mid = a.mid;
        high = a.high;
        energy = a.energy;
        bpm = a.bpm;
        if (a.onset) {
            onsetStrength = a.onsetStrength;
            onsets++;
        }
        if (a.drop) drops++;
    }

    private static com.sun.management.ThreadMXBean threadBean() {
        try {
            var bean = java.lang.management.ManagementFactory.getThreadMXBean();
            if (bean instanceof com.sun.management.ThreadMXBean sun && sun.isThreadAllocatedMemorySupported()) {
                sun.setThreadAllocatedMemoryEnabled(true);
                return sun;
            }
        } catch (Throwable ignored) {
            // not on this JVM
        }
        return null;
    }

    /** One line for logs and tests. */
    public static String stats() {
        return String.format(Locale.ROOT, "source=%s hops=%d avg=%.1fus max=%.1fus alloc=%dB bass=%.2f energy=%.2f onsets=%d drops=%d bpm=%.0f loopback=%s",
                source, hops, hopMicrosAvg, hopMicrosMax, allocatedBytes, bass, energy, onsets, drops, bpm, LoopbackCapture.state());
    }
}
