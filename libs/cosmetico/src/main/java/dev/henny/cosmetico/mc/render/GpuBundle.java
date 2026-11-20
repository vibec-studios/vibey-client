package dev.henny.cosmetico.mc.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.henny.cosmetico.mc.CosmeticoMod;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Texture;
import dev.henny.cosmetico.skeleton.Rig;

public final class GpuBundle {
    public final Bundle bundle;
    public final Rig rig;
    private final Tex[] textures;
    private final AnimatedStrip[] strips;
    long lastUsed;
    private boolean released;

    GpuBundle(Rig rig) {
        this.bundle = rig.bundle;
        this.rig = rig;
        int n = bundle.textures.length;
        textures = new Tex[n];
        strips = new AnimatedStrip[n];
        for (int i = 0; i < n; i++) load(i, bundle.textures[i]);
    }

    private void load(int i, Texture t) {
        String prefix = bundle.id + "_" + t.id;
        try {
            NativeImage full = NativeImage.read(t.png);
            if (t.frameCount > 1) {
                strips[i] = AnimatedStrip.create(prefix, t, full);
                textures[i] = strips[i].texture();
            } else {
                textures[i] = Tex.of(prefix, full);
            }
        } catch (Exception e) {
            CosmeticoMod.LOG.warn("Texture {} of {} failed to load: {}", t.id, bundle.id, e.toString());
        }
    }

    public Tex texture(int index) {
        return index >= 0 && index < textures.length ? textures[index] : null;
    }

    void animate(long ticks, float partial) {
        for (AnimatedStrip s : strips) if (s != null) s.animate(ticks, partial);
    }

    public boolean released() {
        return released;
    }

    void release() {
        if (released) return;
        released = true;
        for (int i = 0; i < textures.length; i++) {
            if (strips[i] != null) strips[i].release();
            else if (textures[i] != null) textures[i].release();
        }
    }
}
