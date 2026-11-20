package dev.henny.vibeyclient.module.media.lyrics.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.module.media.lyrics.LyricsFeature;
import dev.henny.vibeyclient.module.media.lyrics.sky.SkyDome;

public final class SkyHook {
    private SkyHook() {}

    public static void render(PoseStack pose, WorldSink sink) {
        if (LyricsFeature.running()) SkyDome.render(pose, sink);
    }
}
