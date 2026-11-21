package dev.henny.vibeyclient.module.pvp;

import net.minecraft.client.Minecraft;
//? if >=1.21.11 && <26.1 {
import com.mojang.blaze3d.platform.NativeImage;
import dev.henny.vibeyclient.module.pvp.mixin.OverlayTextureAccessor;
import net.minecraft.client.renderer.texture.DynamicTexture;
//?}

/**
 * The tint of hurt entities. Vanilla keeps it in the upper half of the 16 × 16 "entity color overlay" texture
 * (ARGB {@code 0xB3FF0000}: red, and the entity shader keeps 70 % of the original colour). We repaint exactly those
 * rows with the chosen colour and strength, and paint vanilla's value back when the feature is off.
 */
public final class HitColor {
    private HitColor() {}

    static final int VANILLA = 0xB3FF0000;
    private static int painted = VANILLA;

    /** Repaints the overlay if the wanted colour changed (client thread; safe before the game finished starting). */
    public static void refresh() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        if (!mc.isSameThread()) {
            mc.execute(HitColor::refresh);
            return;
        }
        if (mc.gameRenderer == null) return;
        PvpModule m = PvpModule.get();
        int want = VANILLA;
        if (m != null && m.hitColorActive()) {
            int alpha = Math.round((100 - m.hitStrength.get()) * 2.55f);
            want = (alpha << 24) | (m.hitTint.argb() & 0xFFFFFF);
        }
        if (want == painted) return;
        //? if >=1.21.11 && <26.1 {
        DynamicTexture tex = ((OverlayTextureAccessor) mc.gameRenderer.overlayTexture()).vibey$texture();
        NativeImage img = tex.getPixels();
        if (img == null) return;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 16; x++) img.setPixel(x, y, want);
        }
        tex.upload();
        painted = want;
        //?}
    }

    /** The colour currently in the texture (tests). */
    public static int painted() {
        return painted;
    }
}
