package dev.henny.vibeyclient.ui.vui.corner;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.ui.MenuEntries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public final class PauseRow extends CornerController {
    public static final String DOC = "vibeyclient:pause/row";
    private static final int H = 20;
    private static final int GAP = 4;
    private static final int ROW_W = 204;

    public final List<CornerEntry> entries = new ArrayList<>();
    public CornerEntry main;
    public int width = ROW_W;
    public boolean compact;

    PauseRow(Screen screen, List<? extends GuiEventListener> widgets) {
        for (MenuEntries.Entry e : MenuEntries.visible(MenuEntries.Place.PAUSE)) {
            CornerEntry c = new CornerEntry(e);
            if (main == null && "vibey".equals(e.id())) main = c;
            else entries.add(c);
        }
        if (main == null && !entries.isEmpty()) main = entries.remove(0);
        Bounds b = Bounds.of(widgets, screen.height / 4);
        int icons = entries.size() * (H + GAP);
        int label = main == null ? 0 : Minecraft.getInstance().font.width(main.getLabel()) + 32;
        width = Math.max(ROW_W, Math.min(screen.width - 16, label + icons));
        compact = label + icons > screen.width - 16;
        int total = compact ? (entries.size() + 1) * (H + GAP) - GAP : width;
        int top;
        if (b.maxY() + 8 + H <= screen.height - 4) top = b.maxY() + 8;
        else if (b.minY() - 8 - H >= 4) top = b.minY() - 8 - H;
        else {
            compact = true;
            total = (entries.size() + 1) * (H + GAP) - GAP;
            top = 4;
        }
        int corner = Vibey.platform().isModLoaded("vibeylyrics") ? 90 : 4;
        boolean cornered = top == 4 && compact && b.maxY() + 8 + H > screen.height - 4;
        x.set(cornered ? corner : (screen.width - total) / 2);
        y.set(top);
    }

    public CornerEntry getMain() {
        return main;
    }

    @Override
    public List<CornerEntry> all() {
        List<CornerEntry> out = new ArrayList<>();
        if (main != null) out.add(main);
        out.addAll(entries);
        return out;
    }
}
