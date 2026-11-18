package dev.henny.vibeyclient.client;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.MenuEntries;
import dev.henny.vibeyclient.ui.VibeyButton;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.title.TitleHook;
import dev.henny.vibeyclient.ui.title.VibeyTitleScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Decorates vanilla screens: one Vibey row in the pause menu (entries from {@link MenuEntries.Place#PAUSE}, placed
 * under the vanilla buttons – or above them, or icon-only in the corner when the GUI is too small) and a small Vibey
 * button on the vanilla title screen while the Vibey main menu is off.
 */
public final class PauseMenu {
    private PauseMenu() {}

    private static final int H = 20;
    private static final int GAP = 4;

    public static void decorate(Screen screen, List<? extends GuiEventListener> widgets, Consumer<AbstractWidget> add) {
        if (screen instanceof TitleScreen) {
            decorateTitle(screen, add);
            return;
        }
        if (!(screen instanceof PauseScreen)) return;
        if (!hasButton(widgets, "menu.returnToGame")) return; // pause menu hidden (F3 + Esc)
        List<MenuEntries.Entry> entries = MenuEntries.visible(MenuEntries.Place.PAUSE);
        if (entries.isEmpty()) return;

        // Bounds of what vanilla (and other mods) put on the screen.
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (GuiEventListener l : widgets) {
            if (!(l instanceof LayoutElement e) || (l instanceof AbstractWidget w && !w.visible)) continue;
            if (e.getWidth() <= 0 || e.getHeight() <= 0) continue;
            minY = Math.min(minY, e.getY());
            maxY = Math.max(maxY, e.getY() + e.getHeight());
        }
        if (minY == Integer.MAX_VALUE) {
            minY = screen.height / 4;
            maxY = minY;
        }

        List<String> labels = new ArrayList<>();
        int total = 0;
        for (MenuEntries.Entry e : entries) {
            String label = Lang.tr(e.labelKey());
            labels.add(label);
            total += VibeyButton.widthFor(label);
        }
        total += GAP * (entries.size() - 1);
        boolean iconOnly = total > screen.width - 16;
        if (iconOnly) total = entries.size() * H + GAP * (entries.size() - 1);

        int y;
        if (maxY + 8 + H <= screen.height - 4) y = maxY + 8;           // below the vanilla buttons
        else if (minY - 8 - H >= 4) y = minY - 8 - H;                   // above them
        else {                                                           // no room: icons in the top left corner
            iconOnly = true;
            total = entries.size() * H + GAP * (entries.size() - 1);
            y = 4;
        }
        // The standalone Vibey Lyrics mod puts its own button into the top left corner (6, 6, 80 wide).
        int corner = dev.henny.vibeyclient.Vibey.platform().isModLoaded("vibeylyrics") ? 90 : 4;
        int x = y == 4 && iconOnly && maxY + 8 + H > screen.height - 4 ? corner : (screen.width - total) / 2;

        for (int i = 0; i < entries.size(); i++) {
            MenuEntries.Entry e = entries.get(i);
            String label = labels.get(i);
            int w = iconOnly ? H : VibeyButton.widthFor(label);
            String icon = Icons.has(e.icon()) ? e.icon() : null;
            VibeyButton b = new VibeyButton(x, y, w, H, Component.literal(label), icon, () -> e.action().accept(screen))
                    .iconOnly(iconOnly).accent("vibey".equals(e.id())).badge(MenuEntries.badge(e));
            add.accept(b);
            x += w + GAP;
        }
    }

    /** Vanilla title screen (Vibey main menu off, or "classic menu" chosen): a small way back into Vibey. */
    private static void decorateTitle(Screen screen, Consumer<AbstractWidget> add) {
        boolean vibeyMenu = GlobalSettings.vibeyMainMenu.on() && TitleHook.enabled();
        String label = Lang.tr(vibeyMenu ? "core.title.backToVibey" : "core.pause.button");
        add.accept(new VibeyButton(6, 6, VibeyButton.widthFor(label), H, Component.literal(label), Icons.SEAL, () -> {
            if (vibeyMenu) ClientCompat.openScreen(new VibeyTitleScreen(false));
            else ClientCompat.openScreen(new VibeyScreen(screen));
        }).accent(true));
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
