package dev.henny.vibeyclient.module.hud.timer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

final class TimerSound {
    private TimerSound() {}

    static void ding() {
        try {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.2f, 0.8f));
        } catch (RuntimeException ignored) {
        }
    }
}
