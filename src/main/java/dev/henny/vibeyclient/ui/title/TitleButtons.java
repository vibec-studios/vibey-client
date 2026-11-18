package dev.henny.vibeyclient.ui.title;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.MenuEntries;
import dev.henny.vibeyclient.ui.VibeyButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * The vanilla title screen stays as it is; Vibey only adds a row of small square icon buttons (entries of
 * {@link MenuEntries.Place#TITLE}: wardrobe, friends with an unread badge, media, the Vibey menu) in a free corner:
 * top right, else top left, else bottom left above the version text. Every button has a tooltip and works with Tab.
 */
public final class TitleButtons {
    private TitleButtons() {}

    public static final int SIZE = 20;
    private static final int GAP = 3;
    private static final int MARGIN = 6;

    public static void decorate(Screen screen, List<? extends GuiEventListener> widgets, Consumer<AbstractWidget> add) {
        List<MenuEntries.Entry> entries = MenuEntries.visible(MenuEntries.Place.TITLE);
        if (entries.isEmpty()) return;
        BackendStatus.refresh(); // keeps the online state fresh for badges and the menus (at most every 5 min)
        int n = entries.size();
        int total = n * SIZE + (n - 1) * GAP;
        // The standalone Vibey Lyrics mod puts its own button into the top left corner (6, 6, 80 wide).
        int left = Vibey.platform().isModLoaded("vibeylyrics") ? 92 : MARGIN;
        int[][] spots = {
                {screen.width - MARGIN - total, MARGIN},
                {left, MARGIN},
                {MARGIN, screen.height - 12 - SIZE - 4},
        };
        int[] spot = spots[0];
        for (int[] s : spots) {
            if (free(widgets, s[0], s[1], total, SIZE)) {
                spot = s;
                break;
            }
        }
        int x = spot[0];
        for (MenuEntries.Entry e : entries) {
            String label = Lang.tr(e.labelKey());
            String icon = Icons.has(e.icon()) ? e.icon() : Icons.SEAL;
            VibeyButton b = new VibeyButton(x, spot[1], SIZE, SIZE, Component.literal(label), icon, () -> e.action().accept(screen))
                    .iconOnly(true).subtle(true).accent("vibey".equals(e.id())).badge(() -> MenuEntries.badge(e));
            add.accept(b);
            x += SIZE + GAP;
        }
    }

    /** No visible widget overlaps the area (with a 2 px margin). */
    private static boolean free(List<? extends GuiEventListener> widgets, int x, int y, int w, int h) {
        if (x < 0 || y < 0) return false;
        for (GuiEventListener l : widgets) {
            if (!(l instanceof LayoutElement e) || (l instanceof AbstractWidget aw && !aw.visible)) continue;
            if (e.getWidth() <= 0 || e.getHeight() <= 0) continue;
            if (e.getX() < x + w + 2 && e.getX() + e.getWidth() > x - 2 && e.getY() < y + h + 2 && e.getY() + e.getHeight() > y - 2) return false;
        }
        return true;
    }
}
