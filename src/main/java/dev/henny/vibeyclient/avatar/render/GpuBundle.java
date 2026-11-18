package dev.henny.vibeyclient.avatar.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.bundle.Bundle.Animation;
import dev.henny.vibeyclient.avatar.bundle.Bundle.Track;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Render-thread side of a bundle: compiled mesh, uploaded textures and per-animation lookup tables. Animated
 * textures keep the whole strip on the CPU and copy the current frame into a frame-sized texture whenever it changes
 * (so capes/shields rendered by vanilla models animate too, and {@code interpolate} blends frames).
 */
public final class GpuBundle {
    public final Bundle bundle;
    public final BundleMesh mesh;
    /** Per bundle texture (frame sized). Null entries failed to load. */
    public final Tex[] textures;
    private final NativeImage[] strips;
    private final int[] shownKey;
    /** Per animation: track per model bone index (null = not animated). */
    private final Map<Animation, Track[]> boneTracks = new IdentityHashMap<>();
    long lastUsed;
    private boolean released;

    GpuBundle(Bundle bundle, BundleMesh mesh) {
        this.bundle = bundle;
        this.mesh = mesh;
        int n = bundle.textures.length;
        textures = new Tex[n];
        strips = new NativeImage[n];
        shownKey = new int[n];
        for (int i = 0; i < n; i++) {
            Bundle.Texture t = bundle.textures[i];
            try {
                NativeImage full = NativeImage.read(t.png);
                if (t.frameCount > 1) {
                    int fw = full.getWidth(), fh = full.getHeight() / t.frameCount;
                    NativeImage frame = new NativeImage(fw, fh, true);
                    strips[i] = full;
                    textures[i] = Tex.of(bundle.id + "_" + t.id, frame);
                    shownKey[i] = -1;
                    showFrame(i, 0, 0, 0f);
                } else {
                    textures[i] = Tex.of(bundle.id + "_" + t.id, full);
                }
            } catch (Exception e) {
                Vibey.LOG.warn("Texture {} of {} failed to load: {}", t.id, bundle.id, e.toString());
            }
        }
        for (Animation a : bundle.animations.values()) {
            Track[] tr = new Track[mesh.parts.length];
            for (Track t : a.tracks) if (t.bone() >= 0 && t.bone() < tr.length) tr[t.bone()] = t;
            boneTracks.put(a, tr);
        }
    }

    public Track[] tracks(Animation a) {
        return boneTracks.get(a);
    }

    public Tex texture(int index) {
        return index >= 0 && index < textures.length ? textures[index] : null;
    }

    /** Advances animated textures to {@code ticks} (+ partial for interpolation). Render thread, cheap if unchanged. */
    public void animate(long ticks, float partial) {
        for (int i = 0; i < strips.length; i++) {
            if (strips[i] == null || textures[i] == null) continue;
            Bundle.Texture t = bundle.textures[i];
            long cycle = t.cycleTicks();
            long pos = ticks % cycle;
            int step = (int) (pos / t.frameTime);
            int a = t.frames[step % t.frames.length];
            int b = t.frames[(step + 1) % t.frames.length];
            float mix = t.interpolate ? ((pos % t.frameTime) + partial) / t.frameTime : 0f;
            // quantise the blend so we upload at most ~8 times per frame step
            int q = t.interpolate ? Math.min(7, (int) (mix * 8f)) : 0;
            int key = a * 4096 + b * 8 + q;
            if (key != shownKey[i]) showFrame(i, a, b, q / 8f);
        }
    }

    private void showFrame(int i, int a, int b, float mix) {
        NativeImage strip = strips[i];
        Tex tex = textures[i];
        NativeImage dst = tex.pixels();
        if (dst == null) return;
        int w = dst.getWidth(), h = dst.getHeight();
        Bundle.Texture t = bundle.textures[i];
        shownKey[i] = a * 4096 + b * 8 + Math.round(mix * 8f);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pa = TexPixels.get(strip, x, a * h + y);
                if (mix > 0f && t.interpolate) {
                    int pb = TexPixels.get(strip, x, b * h + y);
                    pa = TexPixels.lerp(pa, pb, mix);
                }
                TexPixels.set(dst, x, y, pa);
            }
        }
        tex.upload();
    }

    public boolean released() {
        return released;
    }

    void release() {
        if (released) return;
        released = true;
        for (Tex t : textures) if (t != null) t.release();
        for (NativeImage s : strips) if (s != null) s.close();
    }
}
