package dev.henny.vibeyclient.media.player;

import dev.henny.vibeyclient.Vibey;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class FfmpegAudioLoop implements Runnable {
    private static final int RATE = FfmpegCommand.RATE;
    private static final int CHUNK_BYTES = RATE / 20 * 2;
    private static final int AL_BUFFERS = 8;

    private final FfmpegSession s;
    private final byte[] tmp = new byte[CHUNK_BYTES];
    private final ByteBuffer pcm = ByteBuffer.allocateDirect(CHUNK_BYTES).order(ByteOrder.nativeOrder());
    private final int[] bufs = new int[AL_BUFFERS];
    private final int[] samples = new int[AL_BUFFERS];
    private InputStream in;
    private int src;
    private boolean eof;
    private long played;
    private float lastGain = -1f;

    FfmpegAudioLoop(FfmpegSession s) {
        this.s = s;
    }

    @Override
    public void run() {
        boolean alReady = false;
        try (InputStream stream = s.audio.getInputStream()) {
            in = stream;
            int n = readFully();
            if (n <= 1) {
                s.audioState = 2;
                return;
            }
            src = AL10.alGenSources();
            AL10.alGenBuffers(bufs);
            alReady = true;
            AL10.alSourcef(src, AL10.AL_ROLLOFF_FACTOR, 0f);
            AL10.alSourcef(src, AL10.AL_GAIN, 0f);
            queue(bufs[0], 0, n);
            for (int i = 1; i < AL_BUFFERS && refill(bufs[i], i); i++) {}
            s.audioState = 1;
            pump();
        } catch (IOException e) {
            if (s.audioState == 0) s.audioState = 2;
        } catch (RuntimeException e) {
            Vibey.LOG.warn("Video audio stopped: {}", e.toString());
            if (s.audioState == 0) s.audioState = 2;
            s.audioEnded = true;
        } finally {
            if (alReady) release();
        }
    }

    private void pump() throws IOException {
        while (!s.closed) {
            applySound();
            int state = AL10.alGetSourcei(src, AL10.AL_SOURCE_STATE);
            if (s.paused) {
                if (state == AL10.AL_PLAYING) AL10.alSourcePause(src);
                sleep(10);
                continue;
            }
            recycle();
            int queued = AL10.alGetSourcei(src, AL10.AL_BUFFERS_QUEUED);
            state = AL10.alGetSourcei(src, AL10.AL_SOURCE_STATE);
            if (state != AL10.AL_PLAYING && queued > 0) AL10.alSourcePlay(src);
            int offset = queued > 0 ? AL10.alGetSourcei(src, AL11.AL_SAMPLE_OFFSET) : 0;
            s.audioClockMs = s.startMs + (played + offset) * 1000L / RATE;
            if (eof && queued == 0) {
                s.audioEnded = true;
                break;
            }
            sleep(5);
        }
    }

    private void applySound() {
        SoundTarget t = s.sound;
        float g = t.gain;
        if (g != lastGain) {
            AL10.alSourcef(src, AL10.AL_GAIN, g);
            lastGain = g;
        }
        if (t.relative) {
            AL10.alSourcei(src, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            AL10.alSource3f(src, AL10.AL_POSITION, 0f, 0f, 0f);
        } else {
            AL10.alSourcei(src, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
            AL10.alSource3f(src, AL10.AL_POSITION, (float) t.x, (float) t.y, (float) t.z);
        }
    }

    private void recycle() throws IOException {
        int processed = AL10.alGetSourcei(src, AL10.AL_BUFFERS_PROCESSED);
        while (processed-- > 0) {
            int b = AL10.alSourceUnqueueBuffers(src);
            int slot = 0;
            for (int i = 0; i < AL_BUFFERS; i++) if (bufs[i] == b) slot = i;
            played += samples[slot];
            samples[slot] = 0;
            if (!eof) refill(b, slot);
        }
    }

    private boolean refill(int buf, int slot) throws IOException {
        int n = readFully();
        if (n <= 1) {
            eof = true;
            return false;
        }
        queue(buf, slot, n);
        return true;
    }

    private void queue(int buf, int slot, int n) {
        n &= ~1;
        pcm.clear();
        pcm.put(tmp, 0, n).flip();
        AL10.alBufferData(buf, AL10.AL_FORMAT_MONO16, pcm.asShortBuffer(), RATE);
        samples[slot] = n / 2;
        AL10.alSourceQueueBuffers(src, buf);
    }

    private void release() {
        try {
            AL10.alSourceStop(src);
            AL10.alSourcei(src, AL10.AL_BUFFER, 0);
            AL10.alDeleteSources(src);
            AL10.alDeleteBuffers(bufs);
        } catch (RuntimeException ignored) {}
    }

    private int readFully() throws IOException {
        int got = 0;
        while (got < CHUNK_BYTES && !s.closed) {
            int n = in.read(tmp, got, CHUNK_BYTES - got);
            if (n < 0) break;
            got += n;
        }
        return got;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
