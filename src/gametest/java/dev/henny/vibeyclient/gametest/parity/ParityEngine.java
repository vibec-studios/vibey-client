package dev.henny.vibeyclient.gametest.parity;

import net.minecraft.client.gui.screens.Screen;

import java.util.Map;
import java.util.UUID;

public interface ParityEngine {
    String name();

    int itemCount();

    boolean has(String itemId);

    void pin(double seconds);

    void tick(long ticks);

    void seed(long seed);

    void equipSelf(Map<String, String> slots);

    void clearSelf();

    void wear(UUID player, Map<String, String> slots);

    void track(UUID player);

    void play(UUID player, String emoteId);

    void stop(UUID player);

    void playSelf(String emoteId);

    Screen previews(float[] yaws);

    void previewEmote(Screen previews, String emoteId);

    void previewOverride(Screen previews, String slot, String itemId);

    Screen thumbnails(String[] itemIds);

    void openWardrobe(String slot);

    void openWardrobeEmotes();
}
