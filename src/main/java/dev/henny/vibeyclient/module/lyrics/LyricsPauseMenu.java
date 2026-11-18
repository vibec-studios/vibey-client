package dev.henny.vibeyclient.module.lyrics;


import dev.henny.vibeyclient.module.lyrics.screen.LyricsScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.List;
import java.util.function.Consumer;

/** Optional small "♪ Lyrics" button in the top left corner of the pause menu, next to the Vibey button. */
final class LyricsPauseMenu {
    private LyricsPauseMenu() {}

    static void decorate(Screen screen, List<? extends GuiEventListener> widgets, Consumer<AbstractWidget> add) {
        if (!(screen instanceof PauseScreen)) return;
        // Pause menu hidden (F3 + Esc): no buttons at all.
        boolean visible = false;
        for (GuiEventListener l : widgets) {
            if (l instanceof Button b && b.getMessage().getContents() instanceof TranslatableContents tc
                    && tc.getKey().equals("menu.returnToGame")) visible = true;
        }
        if (!visible) return;
        add.accept(Button.builder(T.text("pause"), b -> LyricsCompat.openScreen(new LyricsScreen(screen)))
                .bounds(74, 6, 80, 20).build());
    }
}
