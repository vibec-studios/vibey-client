package dev.henny.vibeyclient.ui.vui.corner;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.ui.MenuEntries;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public final class TitleCorner extends CornerController {
    public static final String DOC = "vibeyclient:title/corner";
    static final int SIZE = 20;
    static final int GAP = 2;
    private static final int MARGIN = 6;

    public final List<CornerEntry> entries = new ArrayList<>();
    public String corner = "at";
    public String spot = "top-right";

    TitleCorner(Screen screen, List<? extends GuiEventListener> widgets) {
        for (MenuEntries.Entry e : MenuEntries.visible(MenuEntries.Place.TITLE)) entries.add(new CornerEntry(e));
        int n = entries.size();
        int total = n * SIZE + Math.max(0, n - 1) * GAP;
        int left = Vibey.platform().isModLoaded("vibeylyrics") ? 92 : MARGIN;
        int[][] spots = {{screen.width - MARGIN - total, MARGIN}, {left, MARGIN}, {MARGIN, screen.height - 12 - SIZE - 4}};
        String[] names = {"top-right", "top-left", "bottom-left"};
        int pick = 0;
        for (int i = 0; i < spots.length; i++) {
            if (Bounds.free(widgets, spots[i][0], spots[i][1], total, SIZE)) {
                pick = i;
                break;
            }
        }
        spot = names[pick];
        x.set(spots[pick][0]);
        y.set(spots[pick][1]);
    }

    @Override
    public List<CornerEntry> all() {
        return entries;
    }
}
