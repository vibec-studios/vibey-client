package dev.henny.vibeyclient.client;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.VibeyScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.List;
import java.util.function.Consumer;

/** Puts a "Vibey" button into the top left corner of the pause menu. */
public final class PauseMenu {
    private PauseMenu() {}

    public static void decorate(Screen screen, List<? extends GuiEventListener> widgets, Consumer<AbstractWidget> add) {
        if (!(screen instanceof PauseScreen)) return;
        if (!hasButton(widgets, "menu.returnToGame")) return; // pause menu hidden (F3 + Esc)
        // The standalone Vibey Lyrics mod puts its own button into this corner (6, 6, 80 wide).
        int x = dev.henny.vibeyclient.Vibey.platform().isModLoaded("vibeylyrics") ? 90 : 6;
        add.accept(Button.builder(Lang.text("core.pause.button"), b -> ClientCompat.openScreen(new VibeyScreen(screen)))
                .bounds(x, 6, 64, 20).build());
    }

    private static boolean hasButton(List<? extends GuiEventListener> widgets, String key) {
        for (GuiEventListener l : widgets) {
            if (l instanceof Button b && b.getMessage().getContents() instanceof TranslatableContents tc && tc.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }
}
