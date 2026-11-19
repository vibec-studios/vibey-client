package dev.henny.vibeyclient.module.clips.encode;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.clips.ring.ClipRing;
import dev.henny.vibeyclient.module.clips.ring.TsChunker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * One FFmpeg encoder process: raw NV12 frames go into its stdin at a constant frame rate, MPEG-TS comes out of its
 * stdout and is cut into GOP chunks for the {@link ClipRing}. The render thread only hands over finished frames
 * ({@link #borrow}/{@link #submit}); pacing, pipe writes and reading run on daemon threads. While {@link #paused} no
 * frames are written at all, so a clip just has a jump cut at a pause.
 */
public final class EncodeSession implements AutoCloseable {
    private static final Set<Process> LIVE = ConcurrentHashMap.newKeySet();
    private static final int POOL = 4;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> LIVE.forEach(Process::destroyForcibly), "Vibey clips cleanup"));
    }

    public final int id;
    public final Encoder encoder;
    public final int width, height, fps;
    private final int frameBytes;
    private final Process process;
    private final OutputStream stdin;
    private final TsChunker chunker;
    private final ArrayBlockingQueue<byte[]> free = new ArrayBlockingQueue<>(POOL);
    private final AtomicReference<byte[]> latest = new AtomicReference<>();
    private volatile boolean paused;
    private volatile boolean closing;
    private volatile boolean outputEnded;
    private volatile String lastError;
    private final long startedNanos = System.nanoTime();
    private final AtomicLong framesWritten = new AtomicLong();
    private final AtomicLong framesRepeated = new AtomicLong();
    private final AtomicLong framesSuperseded = new AtomicLong();
    private final AtomicLong bytesOut = new AtomicLong();
    private volatile long writeNanosMax;

    public EncodeSession(int id, String ffmpeg, Encoder encoder, int width, int height, int fps, int kbps, ClipRing ring) throws IOException {
        this.id = id;
        this.encoder = encoder;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.frameBytes = width * height * 3 / 2;
        List<String> c = new ArrayList<>(List.of(ffmpeg, "-hide_banner", "-loglevel", "error",
                "-f", "rawvideo", "-pix_fmt", "nv12", "-s", width + "x" + height, "-r", String.valueOf(fps), "-i", "pipe:0"));
        c.addAll(encoder.args(fps, kbps));
        c.addAll(List.of("-an", "-f", "mpegts", "-muxdelay", "0", "-muxpreload", "0", "-flush_packets", "1", "pipe:1"));
        process = new ProcessBuilder(c).start();
        LIVE.add(process);
        process.onExit().thenRun(() -> LIVE.remove(process));
        Priority.belowNormal(process);
        stdin = process.getOutputStream();
        chunker = new TsChunker(ring, id);
        for (int i = 0; i < POOL; i++) free.add(new byte[frameBytes]);
        thread("writer", this::writeLoop, Thread.NORM_PRIORITY);
        thread("reader", this::readLoop, Thread.NORM_PRIORITY);
        thread("log", this::logLoop, Thread.MIN_PRIORITY);
    }

    private void thread(String name, Runnable r, int prio) {
        Thread t = new Thread(r, "Vibey clips " + name + " #" + id);
        t.setDaemon(true);
        t.setPriority(prio);
        t.start();
    }

    // ============================================================================================ render thread side

    public int frameBytes() {
        return frameBytes;
    }

    /** A free frame buffer to copy the next frame into, or null when the encoder is behind (skip this frame). */
    public byte[] borrow() {
        return free.poll();
    }

    /** Hands over a filled buffer from {@link #borrow}. An older frame the writer has not taken yet is dropped. */
    public void submit(byte[] frame) {
        byte[] old = latest.getAndSet(frame);
        if (old != null) {
            free.offer(old);
            framesSuperseded.incrementAndGet();
        }
    }

    /** Gives a borrowed buffer back unused. */
    public void giveBack(byte[] frame) {
        free.offer(frame);
    }

    public void paused(boolean p) {
        paused = p;
    }

    public boolean paused() {
        return paused;
    }

    // ============================================================================================ threads

    private void writeLoop() {
        long period = 1_000_000_000L / fps;
        long next = System.nanoTime();
        byte[] current = null;
        try {
            while (!closing) {
                long now = System.nanoTime();
                if (now < next) {
                    LockSupport.parkNanos(Math.min(next - now, 2_000_000L));
                    continue;
                }
                next += period;
                if (now - next > period * 4) next = now + period; // fell far behind (system busy): don't catch up
                if (paused) {
                    // nothing is fed while paused; the next frame after resuming starts right away
                    next = now + period;
                    if (current != null) {
                        free.offer(current); // never repeat a frame from before the pause
                        current = null;
                    }
                    byte[] stale = latest.getAndSet(null);
                    if (stale != null) free.offer(stale);
                    continue;
                }
                byte[] n = latest.getAndSet(null);
                if (n != null) {
                    if (current != null) free.offer(current);
                    current = n;
                } else if (current != null) {
                    framesRepeated.incrementAndGet(); // the game was slower than the clip: repeat (constant frame rate)
                } else {
                    continue; // nothing captured yet
                }
                long t0 = System.nanoTime();
                stdin.write(current, 0, frameBytes);
                long dt = System.nanoTime() - t0;
                if (dt > writeNanosMax) writeNanosMax = dt;
                framesWritten.incrementAndGet();
            }
        } catch (IOException e) {
            if (!closing) lastError = "encoder stopped: " + e.getMessage();
        } finally {
            try {
                stdin.close(); // FFmpeg flushes the encoder and ends the stream
            } catch (IOException ignored) {
                // gone
            }
        }
    }

    private void readLoop() {
        byte[] buf = new byte[64 * 1024];
        try (InputStream in = process.getInputStream()) {
            int n;
            while ((n = in.read(buf)) > 0) {
                bytesOut.addAndGet(n);
                chunker.feed(buf, 0, n);
            }
        } catch (IOException e) {
            // process gone
        } finally {
            chunker.end();
            outputEnded = true;
        }
    }

    private void logLoop() {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String t = line.strip();
                if (t.isEmpty()) continue;
                lastError = t.length() > 200 ? t.substring(0, 200) : t;
                Vibey.LOG.debug("clips ffmpeg: {}", lastError);
            }
        } catch (IOException ignored) {
            // closed
        }
    }

    // ============================================================================================ state

    public boolean alive() {
        return process.isAlive() && !outputEnded;
    }

    /** FFmpeg ended by itself (crash, driver error) – not because we closed it. */
    public boolean failed() {
        return !closing && !alive();
    }

    public boolean producedOutput() {
        return bytesOut.get() > 0;
    }

    public String lastError() {
        return lastError;
    }

    public long framesWritten() {
        return framesWritten.get();
    }

    public long framesRepeated() {
        return framesRepeated.get();
    }

    public long framesSuperseded() {
        return framesSuperseded.get();
    }

    public long writeMsMax() {
        return writeNanosMax / 1_000_000L;
    }

    public long ageMs() {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    public long pid() {
        return process.pid();
    }

    /** FFmpeg's CPU time so far (user + kernel), or -1. */
    public long cpuMs() {
        return process.info().totalCpuDuration().map(Duration::toMillis).orElse(-1L);
    }

    public long rssBytes() {
        return Priority.rss(process);
    }

    /** Closes stdin (FFmpeg flushes its last frames into the ring), waits up to {@code waitMs}, then kills it. */
    public void close(long waitMs) {
        closing = true;
        try {
            stdin.close();
        } catch (IOException ignored) {
            // gone
        }
        try {
            if (!process.waitFor(waitMs, TimeUnit.MILLISECONDS)) process.destroyForcibly();
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        close(1500);
    }
}
