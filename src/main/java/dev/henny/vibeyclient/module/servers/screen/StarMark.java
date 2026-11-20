package dev.henny.vibeyclient.module.servers.screen;

import dev.henny.vibeyclient.module.servers.ServersModule;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;

public final class StarMark {
    public static final int SIZE = Icons.SIZE, PAD = 2, GOLD = 0xFFFFD23F;

    private StarMark() {}

    public static int x(ServerSelectionList.Entry e) {
        return e.getContentRight() - SIZE - PAD;
    }

    public static int y(ServerSelectionList.Entry e) {
        return e.getContentY() + e.getContentHeight() - SIZE - PAD;
    }

    public static void draw(GuiGraphics g, ServerSelectionList.Entry e, ServerData d, int mx, int my, boolean hovered) {
        ServersModule m = ServersModule.get();
        if (m == null || !m.favouritesActive()) return;
        boolean fav = m.favourite(d.ip);
        boolean over = inside(e, mx, my);
        if (!fav && !hovered) return;
        int color = fav ? GOLD : over ? Theme.TEXT : Theme.DIM;
        Icons.draw(new Gfx(g, Minecraft.getInstance().font), Icons.STAR, x(e), y(e), color);
    }

    public static boolean click(ServerSelectionList.Entry e, ServerData d, double mx, double my) {
        ServersModule m = ServersModule.get();
        if (m == null || !m.favouritesActive() || !inside(e, mx, my)) return false;
        m.toggleFavourite(d.ip);
        ServerSession s = ServerSessions.current();
        if (s != null) s.refreshList();
        return true;
    }

    static boolean inside(ServerSelectionList.Entry e, double mx, double my) {
        int x = x(e) - 1, y = y(e) - 1;
        return mx >= x && my >= y && mx < x + SIZE + 2 && my < y + SIZE + 2;
    }
}
