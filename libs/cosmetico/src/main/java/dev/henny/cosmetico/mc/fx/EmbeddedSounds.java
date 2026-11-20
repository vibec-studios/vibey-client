package dev.henny.cosmetico.mc.fx;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.mc.CosmeticoMod;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Sound;
import org.lwjgl.openal.AL10;

import java.util.IdentityHashMap;
import java.util.Map;

public final class EmbeddedSounds {
    private EmbeddedSounds() {}

    private static final Map<Sound, OggDecoder.Pcm> DECODED = new IdentityHashMap<>();
    private static final Map<Sound, Integer> BUFFERS = new IdentityHashMap<>();
    private static boolean broken;
    private static int played;

    public static void prefetch(Bundle b) {
        for (Sound s : b.sounds) {
            synchronized (DECODED) {
                if (DECODED.containsKey(s)) continue;
                DECODED.put(s, null);
            }
            Cosmetico.host().workers().execute(() -> {
                OggDecoder.Pcm d = OggDecoder.decode(s.ogg());
                synchronized (DECODED) {
                    DECODED.put(s, d);
                }
            });
        }
    }

    public static void play(Sound s, double x, double y, double z, float volume, float pitch) {
        if (broken || volume <= 0f) return;
        try {
            Integer buffer = BUFFERS.get(s);
            if (buffer == null) {
                buffer = upload(s);
                if (buffer == null) return;
            }
            AlSources.play(buffer, x, y, z, volume, pitch);
            if (played++ == 0) CosmeticoMod.LOG.info("Embedded sound played");
        } catch (Throwable t) {
            broken = true;
            CosmeticoMod.LOG.warn("Embedded sounds disabled: {}", t.toString());
        }
    }

    private static Integer upload(Sound s) {
        OggDecoder.Pcm d;
        synchronized (DECODED) {
            d = DECODED.get(s);
        }
        if (d == null) d = OggDecoder.decode(s.ogg());
        if (d == null) return null;
        int buf = AlSources.buffer(d);
        BUFFERS.put(s, buf);
        return buf;
    }

    public static void cleanup() {
        if (!broken) AlSources.cleanup();
    }

    public static void evict(Bundle b) {
        for (Sound s : b.sounds) {
            synchronized (DECODED) {
                DECODED.remove(s);
            }
            Integer buf = BUFFERS.remove(s);
            if (buf == null || broken) continue;
            try {
                AlSources.stopAll();
                AL10.alDeleteBuffers(buf);
            } catch (Throwable ignored) {
            }
        }
    }
}
