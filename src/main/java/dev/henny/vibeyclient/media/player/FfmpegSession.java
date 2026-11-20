package dev.henny.vibeyclient.media.player;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

final class FfmpegSession {
    static final int RING = 3;
    private static final long AUDIO_WAIT_NANOS = 2_500_000_000L;

    final FfmpegSpec spec;
    final SoundTarget sound;
    final long startMs;
    final long createdNanos = System.nanoTime();
    final ByteBuffer[] ring = new ByteBuffer[RING];
    final long[] pts = new long[RING];
    final ArrayBlockingQueue<Integer> free = new ArrayBlockingQueue<>(RING);
    final ArrayBlockingQueue<Integer> ready = new ArrayBlockingQueue<>(RING);
    volatile boolean closed;
    volatile boolean paused;
    volatile boolean videoEnded;
    volatile long framesRead;
    volatile int videoExit = Integer.MIN_VALUE;
    volatile int audioState;
    volatile boolean audioEnded;
    volatile long audioClockMs;
    volatile String lastError;
    volatile long durationMs;
    Process video;
    Process audio;
    private boolean wallStarted;
    private long wallBase;
    private long wallNanos;

    FfmpegSession(FfmpegSpec spec, SoundTarget sound, long startMs, boolean paused, long knownDurationMs) {
        this.spec = spec;
        this.sound = sound;
        this.startMs = startMs;
        this.paused = paused;
        this.audioClockMs = startMs;
        this.durationMs = knownDurationMs;
        for (int i = 0; i < RING; i++) {
            ring[i] = ByteBuffer.allocateDirect(spec.w() * spec.h() * 4);
            free.add(i);
        }
        try {
            video = FfmpegProcesses.start(FfmpegCommand.build(spec, startMs, true), true);
            audio = FfmpegProcesses.start(FfmpegCommand.build(spec, startMs, false), false);
        } catch (IOException e) {
            lastError = "FFmpeg did not start: " + e.getMessage();
            videoEnded = true;
            audioState = 2;
            audioEnded = true;
            videoExit = -1;
            return;
        }
        thread("video", new FfmpegVideoLoop(this));
        thread("audio", new FfmpegAudioLoop(this));
        thread("log", new FfmpegLogLoop(this));
    }

    private static void thread(String name, Runnable r) {
        Thread t = new Thread(r, "Vibey video " + name);
        t.setDaemon(true);
        t.start();
    }

    boolean started() {
        return framesRead > 0 || audioState == 1;
    }

    boolean failed() {
        return videoEnded && framesRead == 0 && audioState == 2 && videoExit != Integer.MIN_VALUE;
    }

    boolean ended() {
        return videoEnded && ready.isEmpty() && (audioState == 2 || audioEnded) && (framesRead > 0 || audioState == 1);
    }

    boolean audioOnly() {
        return videoEnded && framesRead == 0 && audioState == 1;
    }

    void setPaused(boolean p) {
        if (p == paused) return;
        if (p) wallBase = clock();
        wallNanos = System.nanoTime();
        paused = p;
    }

    long clock() {
        int a = audioState;
        boolean timedOut = a == 0 && System.nanoTime() - createdNanos > AUDIO_WAIT_NANOS;
        if (a == 1 && !timedOut) return audioClockMs;
        if (a == 2 || timedOut) {
            if (!wallStarted) {
                wallStarted = true;
                wallBase = startMs;
                wallNanos = System.nanoTime();
            }
            return paused ? wallBase : wallBase + (System.nanoTime() - wallNanos) / 1_000_000L;
        }
        return startMs;
    }

    void close() {
        closed = true;
        if (video != null) video.destroyForcibly();
        if (audio != null) audio.destroyForcibly();
    }
}
