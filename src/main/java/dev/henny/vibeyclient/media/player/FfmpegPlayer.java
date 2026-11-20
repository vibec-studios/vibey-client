package dev.henny.vibeyclient.media.player;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicInteger;

public final class FfmpegPlayer implements MediaPlayer {
    private static final AtomicInteger IDS = new AtomicInteger();

    private final FfmpegSpec spec;
    private final SoundTarget sound = new SoundTarget();
    private final Identifier texId;
    private FrameTexture tex;
    private FfmpegSession session;
    private boolean wantPlaying;
    private boolean uploadedAny;
    private long knownDurationMs = -1;
    private volatile String error;

    public FfmpegPlayer(String ffmpeg, String uri, int width, int height, int fps, long startMs, boolean playing) {
        this.spec = new FfmpegSpec(ffmpeg, uri, width & ~1, height & ~1, fps);
        this.texId = Identifier.fromNamespaceAndPath("vibeyclient", "video/ffmpeg_" + IDS.incrementAndGet());
        this.wantPlaying = playing;
        this.session = open(startMs, !playing);
    }

    private FfmpegSession open(long startMs, boolean paused) {
        return new FfmpegSession(spec, sound, startMs, paused, knownDurationMs);
    }

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
        durationMs();
        error = null;
        session = open(Math.max(0, ms), !wantPlaying);
    }

    @Override
    public long positionMs() {
        return session == null ? 0 : session.clock();
    }

    @Override
    public long durationMs() {
        FfmpegSession s = session;
        if (s != null && s.durationMs >= 0) knownDurationMs = s.durationMs;
        return knownDurationMs;
    }

    @Override
    public Status status() {
        FfmpegSession s = session;
        if (error != null) return Status.ERROR;
        if (s == null || s.ended()) return Status.ENDED;
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
        return spec.w();
    }

    @Override
    public int height() {
        return spec.h();
    }

    @Override
    public boolean audioOnly() {
        FfmpegSession s = session;
        return s != null && s.audioOnly();
    }

    @Override
    public void audio(float gain, Vec3 pos) {
        sound.set(gain, pos);
    }

    @Override
    public void frame(boolean visible) {
        FfmpegSession s = session;
        if (s == null) return;
        if (tex == null) {
            tex = new FrameTexture("Vibey video " + texId.getPath(), spec.w(), spec.h());
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
        if (pick == null) return;
        if (visible || !uploadedAny) {
            tex.upload(s.ring[pick]);
            uploadedAny = true;
        }
        s.free.offer(pick);
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
}
