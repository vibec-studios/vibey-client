package dev.henny.vibeyclient.module.media.video.ui;

import dev.henny.vibeyclient.media.ffmpeg.FfmpegPurpose;
import dev.henny.vibeyclient.media.ffmpeg.FfmpegSetupScreen;
import net.minecraft.client.gui.screens.Screen;

public final class SetupScreens {
    private SetupScreens() {}

    public static Screen of(Screen parent, SetupNeed need) {
        if (need == SetupNeed.FFMPEG) return new FfmpegSetupScreen(parent, FfmpegPurpose.PLAYBACK);
        return new SetupScreen(parent, need);
    }
}
