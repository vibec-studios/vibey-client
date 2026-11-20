package dev.henny.cosmetico.mc.fx;

import org.lwjgl.openal.AL10;
import org.lwjgl.system.MemoryUtil;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

final class AlSources {
    private AlSources() {}

    private static final int MAX_SOURCES = 16;
    private static final List<Integer> PLAYING = new ArrayList<>();

    static int buffer(OggDecoder.Pcm d) {
        int buf = AL10.alGenBuffers();
        ShortBuffer sb = MemoryUtil.memAllocShort(d.samples().length);
        sb.put(d.samples()).flip();
        AL10.alBufferData(buf, d.channels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16, sb, d.rate());
        MemoryUtil.memFree(sb);
        return buf;
    }

    static void play(int buffer, double x, double y, double z, float volume, float pitch) {
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
        PLAYING.add(src);
    }

    static void cleanup() {
        for (int i = PLAYING.size() - 1; i >= 0; i--) {
            int src = PLAYING.get(i);
            if (AL10.alGetSourcei(src, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                AL10.alDeleteSources(src);
                PLAYING.remove(i);
            }
        }
    }

    static void stopAll() {
        for (int src : PLAYING) {
            AL10.alSourceStop(src);
            AL10.alDeleteSources(src);
        }
        PLAYING.clear();
    }

    static int playing() {
        return PLAYING.size();
    }
}
