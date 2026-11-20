package dev.henny.cosmetico.mc.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.henny.cosmetico.model.Texture;
import dev.henny.cosmetico.texture.StripFrame;
import dev.henny.cosmetico.texture.StripPixels;

final class AnimatedStrip {
    private final Texture spec;
    private final NativeImage strip;
    private final Tex target;
    private int shownKey = -1;

    AnimatedStrip(Texture spec, NativeImage strip, Tex target) {
        this.spec = spec;
        this.strip = strip;
        this.target = target;
        show(0, 0, 0f);
    }

    static AnimatedStrip create(String prefix, Texture spec, NativeImage full) {
        int fw = full.getWidth(), fh = full.getHeight() / spec.frameCount;
        Tex frame = Tex.of(prefix, new NativeImage(fw, fh, true));
        return new AnimatedStrip(spec, full, frame);
    }

    Tex texture() {
        return target;
    }

    void animate(long ticks, float partial) {
        StripFrame f = StripFrame.at(spec, ticks, partial);
        if (f.key() != shownKey) show(f.a(), f.b(), f.blend());
    }

    private void show(int a, int b, float mix) {
        NativeImage dst = target.pixels();
        if (dst == null) return;
        int w = dst.getWidth(), h = dst.getHeight();
        shownKey = a * 4096 + b * 8 + Math.round(mix * 8f);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pa = TexPixels.get(strip, x, a * h + y);
                if (mix > 0f && spec.interpolate) pa = StripPixels.lerp(pa, TexPixels.get(strip, x, b * h + y), mix);
                TexPixels.set(dst, x, y, pa);
            }
        }
        target.upload();
    }

    void release() {
        target.release();
        strip.close();
    }
}
