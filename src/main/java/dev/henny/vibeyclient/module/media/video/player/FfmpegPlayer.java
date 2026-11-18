package dev.henny.vibeyclient.module.media.video.player;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.media.video.MediaSource;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local files and direct streams through two FFmpeg processes: one writes scaled, frame-rate-limited raw RGBA frames
 * to its stdout, the other mono 48 kHz PCM. Frames land in three preallocated direct buffers per session and are
 * uploaded straight into a GPU texture; the PCM feeds a streaming OpenAL source at the screen (positional, our own
 * gain). The audio clock drives the video; without audio a wall clock does. Seeking restarts both processes.
 */
public final class FfmpegPlayer implements MediaPlayer {
    private static final int RATE = 48000;
    private static final int CHUNK_SAMPLES = RATE / 20; // 50 ms
    private static final int AL_BUFFERS = 8;            // 400 ms queued
    private static final int RING = 3;
    private static final AtomicInteger IDS = new AtomicInteger();
    private static final Set<Process> LIVE = ConcurrentHashMap.newKeySet();
    private static final Pattern DURATION = Pattern.compile("Duration: (\\d+):(\\d+):(\\d+)\\.(\\d+)");

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> LIVE.forEach(Process::destroyForcibly), "Vibey video cleanup"));
    }

    private final String ffmpeg;
    private final MediaSource source;
    private final int w;
    private final int h;
    private final int fps;
    private final Identifier texId;
    private FrameTexture tex;
    private Session session;
    private boolean wantPlaying;
    private boolean uploadedAny;
    private volatile long durationMs = -1;
    private volatile String error;
    private volatile float gain = 1f;
    private volatile double px, py, pz;
    private volatile boolean relative = true;

    public FfmpegPlayer(String ffmpeg, MediaSource source, int width, int height, int fps, long startMs, boolean playing) {
        this.ffmpeg = ffmpeg;
        this.source = source;
        this.w = width & ~1;
        this.h = height & ~1;
        this.fps = fps;
        this.texId = Identifier.fromNamespaceAndPath("vibeyclient", "video/ffmpeg_" + IDS.incrementAndGet());
        this.wantPlaying = playing;
        this.session = new Session(startMs, !playing);
    }

    // ============================================================================================ control

    @Override
    public void play() {
        wantPlaying = true;
        if (session == null || session.ended()) {
            seek(session == null ? 0 : session.clock());
            return;
        }
        session.setPaused(false);
    }

    @Override
    public void pause() {
        wantPlaying = false;
        if (session != null) session.setPaused(true);
    }

    @Override
    public void seek(long ms) {
        if (session != null) session.close();
        error = null;
        session = new Session(Math.max(0, ms), !wantPlaying);
    }

    @Override
    public long positionMs() {
        return session == null ? 0 : session.clock();
    }

    @Override
    public long durationMs() {
        return durationMs;
    }

    @Override
    public Status status() {
        Session s = session;
        if (error != null) return Status.ERROR;
        if (s == null) return Status.ENDED;
        if (s.ended()) return Status.ENDED;
        if (!s.started()) return Status.LOADING;
        return s.paused ? Status.PAUSED : Status.PLAYING;
    }

    @Override
    public String error() {
        return error;
    }

    @Override
    public Identifier texture() {
        return uploadedAny ? texId : null;
    }

    @Override
    public int width() {
        return w;
    }

    @Override
    public int height() {
        return h;
    }

    @Override
    public boolean audioOnly() {
        Session s = session;
        return s != null && s.videoEnded && s.framesRead == 0 && s.audioState == 1;
    }

    @Override
    public void audio(float gain, Vec3 pos) {
        this.gain = Math.max(0f, Math.min(1f, gain));
        if (pos == null) {
            relative = true;
        } else {
            px = pos.x;
            py = pos.y;
            pz = pos.z;
            relative = false;
        }
    }

    @Override
    public void frame(boolean visible) {
        Session s = session;
        if (s == null) return;
        if (tex == null) {
            tex = new FrameTexture("Vibey video " + texId.getPath(), w, h);
            Minecraft.getInstance().getTextureManager().register(texId, tex);
        }
        if (s.failed() && error == null) error = s.lastError != null ? s.lastError : "FFmpeg could not open this";
        long now = s.clock();
        Integer pick = null;
        while (true) {
            Integer head = s.ready.peek();
            if (head == null || s.pts[head] > now + 5) break;
            s.ready.poll();
            if (pick != null) s.free.offer(pick);
            pick = head;
        }
        if (pick != null) {
            if (visible || !uploadedAny) {
                tex.upload(s.ring[pick]);
                uploadedAny = true;
            }
            s.free.offer(pick);
        }
    }

    @Override
    public void close() {
        if (session != null) session.close();
        session = null;
        if (tex != null) {
            Minecraft.getInstance().getTextureManager().release(texId);
            tex = null;
        }
    }

    // ============================================================================================ one decode run

    private List<String> command(long startMs, boolean video) {
        List<String> c = new ArrayList<>();
        c.add(ffmpeg);
        c.add("-hide_banner");
        c.add("-nostdin");
        c.add("-loglevel");
        c.add(video ? "info" : "error");
        String lower = source.uri().toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            c.addAll(List.of("-reconnect", "1", "-reconnect_streamed", "1", "-reconnect_delay_max", "5"));
        }
        if (startMs > 0) {
            c.add("-ss");
            c.add(String.format(Locale.ROOT, "%.3f", startMs / 1000.0));
        }
        c.add("-i");
        c.add(source.uri());
        if (video) {
            c.addAll(List.of("-map", "0:v:0?", "-an", "-sn", "-dn", "-vf",
                    "fps=" + fps + ",scale=" + w + ":" + h + ":force_original_aspect_ratio=decrease:flags=bilinear,pad="
                            + w + ":" + h + ":-1:-1:color=black,format=rgba",
                    "-f", "rawvideo", "-pix_fmt", "rgba", "pipe:1"));
        } else {
            c.addAll(List.of("-map", "0:a:0?", "-vn", "-sn", "-dn", "-ac", "1", "-ar", String.valueOf(RATE), "-f", "s16le", "pipe:1"));
        }
        return c;
    }

    private final class Session {
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
        /** 0 = not known yet, 1 = has audio, 2 = none. */
        volatile int audioState;
        volatile boolean audioEnded;
        volatile long audioClockMs;
        volatile String lastError;
        Process video;
        Process audio;
        // wall clock (no audio)
        boolean wallStarted;
        long wallBase;
        long wallNanos;

        Session(long startMs, boolean paused) {
            this.startMs = startMs;
            this.paused = paused;
            this.audioClockMs = startMs;
            for (int i = 0; i < RING; i++) {
                ring[i] = ByteBuffer.allocateDirect(w * h * 4);
                free.add(i);
            }
            try {
                video = start(command(startMs, true), true);
                audio = start(command(startMs, false), false);
            } catch (IOException e) {
                lastError = "FFmpeg did not start: " + e.getMessage();
                videoEnded = true;
                audioState = 2;
                audioEnded = true;
                videoExit = -1;
                return;
            }
            thread("video", this::videoLoop);
            thread("audio", this::audioLoop);
            thread("log", this::logLoop);
        }

        private Process start(List<String> cmd, boolean video) throws IOException {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            if (!video) pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();
            LIVE.add(p);
            p.onExit().thenRun(() -> LIVE.remove(p));
            try {
                p.getOutputStream().close();
            } catch (IOException ignored) {
                // nothing to close
            }
            return p;
        }

        private void thread(String name, Runnable r) {
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

        void setPaused(boolean p) {
            if (p == paused) return;
            if (p) {
                wallBase = clock();
                wallNanos = System.nanoTime();
            } else {
                wallNanos = System.nanoTime();
            }
            paused = p;
        }

        long clock() {
            int a = audioState;
            boolean timedOut = a == 0 && System.nanoTime() - createdNanos > 2_500_000_000L;
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

        private void videoLoop() {
            int frameBytes = w * h * 4;
            byte[] tmp = new byte[1 << 16];
            try (InputStream in = video.getInputStream()) {
                long index = 0;
                while (!closed) {
                    Integer idx = free.poll(100, TimeUnit.MILLISECONDS);
                    if (idx == null) continue;
                    ByteBuffer b = ring[idx];
                    b.clear();
                    int got = 0;
                    while (got < frameBytes) {
                        int n = in.read(tmp, 0, Math.min(tmp.length, frameBytes - got));
                        if (n < 0) break;
                        b.put(tmp, 0, n);
                        got += n;
                    }
                    if (got < frameBytes) {
                        free.offer(idx);
                        break;
                    }
                    pts[idx] = startMs + (long) (index * 1000.0 / fps);
                    index++;
                    framesRead = index;
                    ready.put(idx);
                }
            } catch (IOException | InterruptedException e) {
                // process gone
            } finally {
                videoEnded = true;
                try {
                    videoExit = video.waitFor(2, TimeUnit.SECONDS) ? video.exitValue() : -1;
                } catch (InterruptedException e) {
                    videoExit = -1;
                }
            }
        }

        private void logLoop() {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(video.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    Matcher m = DURATION.matcher(line);
                    if (m.find() && durationMs < 0) {
                        long ms = (Long.parseLong(m.group(1)) * 3600 + Long.parseLong(m.group(2)) * 60 + Long.parseLong(m.group(3))) * 1000
                                + Long.parseLong((m.group(4) + "00").substring(0, 3));
                        if (ms > 0) durationMs = ms;
                    }
                    String t = line.trim();
                    if (!t.isEmpty() && !t.startsWith("Stream") && !t.startsWith("Input") && !t.startsWith("Output")
                            && !t.startsWith("Duration") && !t.startsWith("Metadata") && !t.startsWith("frame=")
                            && (t.contains("rror") || t.contains("No such") || t.contains("Invalid") || t.contains("not found")
                            || t.contains("denied") || t.contains("refused"))) {
                        lastError = t.length() > 160 ? t.substring(0, 160) : t;
                    }
                }
            } catch (IOException ignored) {
                // closed
            }
        }

        private void audioLoop() {
            int chunkBytes = CHUNK_SAMPLES * 2;
            byte[] tmp = new byte[chunkBytes];
            ByteBuffer pcm = ByteBuffer.allocateDirect(chunkBytes).order(ByteOrder.nativeOrder());
            int src = 0;
            int[] bufs = new int[AL_BUFFERS];
            int[] samples = new int[AL_BUFFERS];
            boolean alReady = false;
            try (InputStream in = audio.getInputStream()) {
                int n = readFully(in, tmp, chunkBytes);
                if (n <= 1) {
                    audioState = 2;
                    return;
                }
                src = AL10.alGenSources();
                AL10.alGenBuffers(bufs);
                alReady = true;
                AL10.alSourcef(src, AL10.AL_ROLLOFF_FACTOR, 0f);
                AL10.alSourcef(src, AL10.AL_GAIN, 0f);
                // first chunk + prefill
                boolean eof = false;
                for (int i = 0; i < AL_BUFFERS; i++) {
                    if (i > 0) {
                        n = readFully(in, tmp, chunkBytes);
                        if (n <= 1) {
                            eof = true;
                            break;
                        }
                    }
                    n &= ~1;
                    pcm.clear();
                    pcm.put(tmp, 0, n).flip();
                    AL10.alBufferData(bufs[i], AL10.AL_FORMAT_MONO16, pcm.asShortBuffer(), RATE);
                    samples[i] = n / 2;
                    AL10.alSourceQueueBuffers(src, bufs[i]);
                }
                audioState = 1;
                long played = 0;
                float lastGain = -1f;
                while (!closed) {
                    float g = gain;
                    if (g != lastGain) {
                        AL10.alSourcef(src, AL10.AL_GAIN, g);
                        lastGain = g;
                    }
                    if (relative) {
                        AL10.alSourcei(src, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
                        AL10.alSource3f(src, AL10.AL_POSITION, 0f, 0f, 0f);
                    } else {
                        AL10.alSourcei(src, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
                        AL10.alSource3f(src, AL10.AL_POSITION, (float) px, (float) py, (float) pz);
                    }
                    int state = AL10.alGetSourcei(src, AL10.AL_SOURCE_STATE);
                    if (paused) {
                        if (state == AL10.AL_PLAYING) AL10.alSourcePause(src);
                        sleep(10);
                        continue;
                    }
                    int processed = AL10.alGetSourcei(src, AL10.AL_BUFFERS_PROCESSED);
                    while (processed-- > 0) {
                        int b = AL10.alSourceUnqueueBuffers(src);
                        int slot = 0;
                        for (int i = 0; i < AL_BUFFERS; i++) if (bufs[i] == b) slot = i;
                        played += samples[slot];
                        samples[slot] = 0;
                        if (eof) continue;
                        n = readFully(in, tmp, chunkBytes);
                        if (n <= 1) {
                            eof = true;
                            continue;
                        }
                        n &= ~1;
                        pcm.clear();
                        pcm.put(tmp, 0, n).flip();
                        AL10.alBufferData(b, AL10.AL_FORMAT_MONO16, pcm.asShortBuffer(), RATE);
                        samples[slot] = n / 2;
                        AL10.alSourceQueueBuffers(src, b);
                    }
                    int queued = AL10.alGetSourcei(src, AL10.AL_BUFFERS_QUEUED);
                    state = AL10.alGetSourcei(src, AL10.AL_SOURCE_STATE);
                    if (state != AL10.AL_PLAYING && queued > 0) {
                        AL10.alSourcePlay(src);
                        state = AL10.AL_PLAYING;
                    }
                    int offset = queued > 0 ? AL10.alGetSourcei(src, AL11.AL_SAMPLE_OFFSET) : 0;
                    audioClockMs = startMs + (played + offset) * 1000L / RATE;
                    if (eof && queued == 0) {
                        audioEnded = true;
                        break;
                    }
                    sleep(5);
                }
            } catch (IOException e) {
                if (audioState == 0) audioState = 2;
            } catch (RuntimeException e) {
                Vibey.LOG.warn("Video audio stopped: {}", e.toString());
                if (audioState == 0) audioState = 2;
                audioEnded = true;
            } finally {
                if (alReady) {
                    try {
                        AL10.alSourceStop(src);
                        AL10.alSourcei(src, AL10.AL_BUFFER, 0);
                        AL10.alDeleteSources(src);
                        AL10.alDeleteBuffers(bufs);
                    } catch (RuntimeException ignored) {
                        // sound engine already gone
                    }
                }
            }
        }

        private int readFully(InputStream in, byte[] buf, int len) throws IOException {
            int got = 0;
            while (got < len && !closed) {
                int n = in.read(buf, got, len - got);
                if (n < 0) break;
                got += n;
            }
            return got;
        }

        private void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
