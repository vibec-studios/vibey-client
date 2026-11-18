package dev.henny.vibeyclient.avatar.fx;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedded OGG sounds of bundles: decoded with stb_vorbis on a worker thread (ships with Minecraft), played through
 * OpenAL as positional sources in Minecraft's own context (its listener follows the camera). Mono sounds are
 * positional; stereo ones play non-positional. Client thread for play/tick.
 */
public final class EmbeddedSounds {
    private EmbeddedSounds() {}

    private static final int MAX_SOURCES = 16;
    private static final Map<Bundle.Sound, Decoded> DECODED = new IdentityHashMap<>();
    private static final Map<Bundle.Sound, Integer> BUFFERS = new IdentityHashMap<>();
    private static final List<int[]> PLAYING = new ArrayList<>();
    private static boolean broken;

    private record Decoded(short[] pcm, int channels, int rate) {}

    /** Starts decoding early (worker thread) so the first play has no delay. */
    public static void prefetch(Bundle b) {
        for (Bundle.Sound s : b.sounds) {
            synchronized (DECODED) {
                if (DECODED.containsKey(s)) continue;
                DECODED.put(s, null);
            }
            Tasks.run(() -> {
                Decoded d = decode(s.ogg());
                synchronized (DECODED) {
                    DECODED.put(s, d);
                }
            });
        }
    }

    /** Streams through stb_vorbis (like the lyrics module): only buffers we allocate ourselves. */
    private static Decoded decode(byte[] ogg) {
        ByteBuffer data = MemoryUtil.memAlloc(ogg.length);
        long handle = 0;
        ShortBuffer chunk = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            data.put(ogg).flip();
            IntBuffer err = stack.mallocInt(1);
            handle = STBVorbis.stb_vorbis_open_memory(data, err, null);
            if (handle == 0) return null;
            org.lwjgl.stb.STBVorbisInfo info = org.lwjgl.stb.STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(handle, info);
            int channels = info.channels(), rate = info.sample_rate();
            chunk = MemoryUtil.memAllocShort(4096 * channels);
            short[] out = new short[Math.max(4096, STBVorbis.stb_vorbis_stream_length_in_samples(handle) * channels)];
            int n = 0;
            while (true) {
                chunk.clear();
                int frames = STBVorbis.stb_vorbis_get_samples_short_interleaved(handle, channels, chunk);
                if (frames <= 0) break;
                int count = frames * channels;
                if (n + count > out.length) out = java.util.Arrays.copyOf(out, Math.max(out.length * 2, n + count));
                chunk.get(out, n, count);
                n += count;
                if (n > 30 * rate * channels) break; // 30 s cap
            }
            return new Decoded(java.util.Arrays.copyOf(out, n), channels, rate);
        } catch (Throwable t) {
            Vibey.LOG.warn("Could not decode embedded sound: {}", t.toString());
            return null;
        } finally {
            if (handle != 0) STBVorbis.stb_vorbis_close(handle);
            if (chunk != null) MemoryUtil.memFree(chunk);
            MemoryUtil.memFree(data);
        }
    }

    /** Plays {@code s} at a world position; {@code volume} already includes the game's sound volume. */
    public static void play(Bundle.Sound s, double x, double y, double z, float volume, float pitch) {
        if (broken || volume <= 0f) return;
        try {
            Integer buffer = BUFFERS.get(s);
            if (buffer == null) {
                Decoded d;
                synchronized (DECODED) {
                    d = DECODED.get(s);
                }
                if (d == null) {
                    // not decoded yet: decode now (small effects, <512 KiB) – rare, prefetch normally ran
                    d = decode(s.ogg());
                    if (d == null) return;
                }
                int buf = AL10.alGenBuffers();
                ShortBuffer sb = MemoryUtil.memAllocShort(d.pcm.length);
                sb.put(d.pcm).flip();
                AL10.alBufferData(buf, d.channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16, sb, d.rate);
                MemoryUtil.memFree(sb);
                BUFFERS.put(s, buf);
                buffer = buf;
            }
            cleanup();
            if (PLAYING.size() >= MAX_SOURCES) return;
            int src = AL10.alGenSources();
            AL10.alSourcei(src, AL10.AL_BUFFER, buffer);
            AL10.alSourcef(src, AL10.AL_GAIN, Math.min(1f, volume));
            AL10.alSourcef(src, AL10.AL_PITCH, Math.max(0.5f, Math.min(2f, pitch)));
            AL10.alSource3f(src, AL10.AL_POSITION, (float) x, (float) y, (float) z);
            AL10.alSourcei(src, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
            AL10.alSourcef(src, AL10.AL_ROLLOFF_FACTOR, 1f);
            AL10.alSourcef(src, AL10.AL_REFERENCE_DISTANCE, 2f);
            AL10.alSourcef(src, AL10.AL_MAX_DISTANCE, 24f);
            AL10.alSourcePlay(src);
            PLAYING.add(new int[]{src});
        } catch (Throwable t) {
            broken = true;
            Vibey.LOG.warn("Embedded sounds disabled: {}", t.toString());
        }
    }

    /** Frees finished sources (call every tick). */
    public static void cleanup() {
        if (broken) return;
        for (int i = PLAYING.size() - 1; i >= 0; i--) {
            int src = PLAYING.get(i)[0];
            if (AL10.alGetSourcei(src, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                AL10.alDeleteSources(src);
                PLAYING.remove(i);
            }
        }
    }

    /** Drops decoded data and buffers of a replaced bundle. */
    public static void evict(Bundle b) {
        for (Bundle.Sound s : b.sounds) {
            synchronized (DECODED) {
                DECODED.remove(s);
            }
            Integer buf = BUFFERS.remove(s);
            if (buf != null && !broken) {
                try {
                    stopAll();
                    AL10.alDeleteBuffers(buf);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void stopAll() {
        for (int[] s : PLAYING) {
            AL10.alSourceStop(s[0]);
            AL10.alDeleteSources(s[0]);
        }
        PLAYING.clear();
    }
}
