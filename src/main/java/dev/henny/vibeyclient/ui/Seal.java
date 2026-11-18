package dev.henny.vibeyclient.ui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.api.GlobalSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;

/**
 * The animated seal mascot for menus: renders {@link SealModel} at the exact screen-pixel density into a texture
 * (crisp at every GUI scale) and animates it like the launcher logo – gentle breathing, blinking, and a happy
 * clap/nod while hovered. Re-renders only when the (time-quantised) pose changes. One instance per place it is
 * drawn; call {@link #close()} when the screen goes away.
 */
public final class Seal implements AutoCloseable {
    private static int counter;

    private final SealModel model;
    private final SealModel.Pose pose = new SealModel.Pose();
    private final SealModel.Pose drawn = new SealModel.Pose();
    private final Anim joy = new Anim(0f, 260);
    private final long start = System.nanoTime();
    private final Object id;
    private DynamicTexture texture;
    private int[] buffer = new int[0];
    private int texW, texH, guiW, guiH, ppu = -1;
    private long nextBlink = System.currentTimeMillis() + 1500;
    private long blinkUntil;
    private long burstUntil;
    private boolean everDrawn;

    public Seal(boolean withIce) {
        this.model = new SealModel(withIce);
        this.id = Compat.id("dynamic/seal_" + (counter++));
    }

    /** GUI size of the seal drawn with {@code unit} GUI pixels per model texel. */
    public int width(float unit) {
        return (int) Math.ceil(model.width() * unit);
    }

    public int height(float unit) {
        return (int) Math.ceil(model.height() * unit);
    }

    /** A short happy burst (clap + hop), e.g. when clicked. */
    public void cheer() {
        burstUntil = System.currentTimeMillis() + 900;
    }

    /**
     * Draws the seal with its top left at x, y; {@code unit} = GUI pixels per texel (any value, rounded to whole
     * screen pixels). {@code hovered} makes it happy. Returns the drawn GUI width.
     */
    public int draw(Gfx g, int x, int y, float unit, boolean hovered) {
        int scale = guiScale();
        int nppu = Math.max(1, Math.round(unit * scale));
        int gw = (int) Math.ceil(model.width() * nppu / scale);
        int gh = (int) Math.ceil(model.height() * nppu / scale);
        if (texture == null || nppu != ppu || gw != guiW || gh != guiH) allocate(nppu, gw, gh, scale);
        updatePose(hovered);
        if (!everDrawn || !pose.same(drawn)) {
            model.render(pose, buffer, texW, texH, ppu);
            upload();
            drawn.set(pose);
            everDrawn = true;
        }
        g.image(texId(), x, y, guiW, guiH, 0, 0, texW, texH, texW, texH);
        return guiW;
    }

    private void updatePose(boolean hovered) {
        long now = System.currentTimeMillis();
        boolean anim = GlobalSettings.animate();
        float j = joy.to(hovered || now < burstUntil ? 1f : 0f);
        // Stepped time: 12 updates/s at rest, 30 when excited – pixel-art pacing and few re-renders.
        float t = (System.nanoTime() - start) / 1e9f;
        float fps = j > 0.01f ? 30f : 12f;
        t = (float) Math.floor(t * fps) / fps;
        if (!anim) {
            t = 0;
            j = 0;
        }
        if (now > nextBlink) {
            blinkUntil = now + 140;
            nextBlink = now + 2200 + (long) (Math.abs(Math.sin(now * 0.001)) * 3000);
        }
        pose.blink = anim && now < blinkUntil;
        float flap = (float) Math.sin(t * 14) * j;
        pose.headZ = q(0.035f * (float) Math.sin(t * 1.4f) + 0.12f * j + (float) Math.sin(t * 7) * 0.08f * j);
        pose.headY = q((float) Math.sin(t * 3.5f) * 0.12f * j);
        pose.finFlap = q(flap);
        pose.tailWag = q((float) Math.sin(t * 10) * j + 0.15f * (float) Math.sin(t * 2.2f) * (1 - j));
        pose.hop = q(Math.abs((float) Math.sin(t * 7)) * 1.2f * j + 0.25f * (1 + (float) Math.sin(t * 1.4f)) * (1 - j));
        pose.yaw = SealModel.YAW - 0.25f * j;
    }

    private static float q(float v) {
        return Math.round(v * 200f) / 200f;
    }

    private static int guiScale() {
        return dev.henny.vibeyclient.client.ClientCompat.guiScale();
    }

    private void allocate(int nppu, int gw, int gh, int scale) {
        release();
        ppu = nppu;
        guiW = gw;
        guiH = gh;
        texW = gw * scale;
        texH = gh * scale;
        buffer = new int[texW * texH];
        //? if >=1.21.5 {
        texture = new DynamicTexture(() -> "vibeyclient seal", texW, texH, true);
        //?} else
        /*texture = new DynamicTexture(texW, texH, true);*/
        Minecraft.getInstance().getTextureManager().register(texId(), texture);
        everDrawn = false;
    }

    private void upload() {
        NativeImage img = texture.getPixels();
        if (img == null) return;
        for (int y = 0; y < texH; y++) {
            int row = y * texW;
            for (int x = 0; x < texW; x++) {
                int argb = buffer[row + x];
                //? if >=1.21.3 {
                img.setPixel(x, y, argb);
                //?} else
                /*img.setPixelRGBA(x, y, (argb & 0xFF00FF00) | (argb >> 16 & 0xFF) | (argb & 0xFF) << 16);*/
            }
        }
        texture.upload();
    }

    //? if >=1.21.11 {
    private net.minecraft.resources.Identifier texId() {
        return (net.minecraft.resources.Identifier) id;
    }
    //?} else {
    /*private net.minecraft.resources.ResourceLocation texId() {
        return (net.minecraft.resources.ResourceLocation) id;
    }
    *///?}

    private void release() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(texId());
            texture = null;
        }
    }

    @Override
    public void close() {
        release();
    }
}
