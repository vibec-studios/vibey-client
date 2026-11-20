package dev.henny.vibeyclient.module.servers.worlds;

import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.servers.ServersModule;
import dev.henny.vibeyclient.module.servers.drag.ListDrag;
import dev.henny.vibeyclient.module.servers.mixin.SelectWorldAccess;
import dev.henny.vibeyclient.module.servers.mixin.WorldSelectionListAccess;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.mc.host.DocumentView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;

public final class WorldSession {
    public static final String SORT = "vibeyclient:servers/worlds";

    private final ServersModule m;
    private final SelectWorldScreen screen;
    private final SortController sort = new SortController(this::cycle);
    private final DocumentView sortView;
    private ListDrag drag;
    private WorldSelectionList dragList;

    WorldSession(ServersModule m, SelectWorldScreen screen) {
        this.m = m;
        this.screen = screen;
        this.sortView = new DocumentView(VuiMc.vui().open(SORT, sort));
    }

    public SortController sort() {
        return sort;
    }

    public DocumentView sortView() {
        return sortView;
    }

    WorldSelectionList list() {
        return ((SelectWorldAccess) screen).vibey$list();
    }

    public ListDrag drag() {
        WorldSelectionList list = list();
        if (list == null) return null;
        if (drag == null || dragList != list) {
            if (drag != null) drag.cancel();
            dragList = list;
            drag = new ListDrag(list, new WorldDrop(m, this::refill));
        }
        return drag;
    }

    void cycle() {
        m.worldSort.set(m.worldSort.get().next());
        refill();
    }

    public void refill() {
        WorldSelectionList list = list();
        if (list == null) return;
        WorldSelectionListAccess a = (WorldSelectionListAccess) list;
        if (a.vibey$levels() != null) a.vibey$fill(a.vibey$filter(), a.vibey$levels());
    }

    void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        ListDrag d = drag();
        if (d != null) {
            d.frame(mouseX, mouseY);
            d.drawFloating(g, mouseY, delta);
        }
        EditBox search = ((SelectWorldAccess) screen).vibey$search();
        if (search == null) return;
        sort.show(m.worldSort.get());
        sort.place(search.getX() + search.getWidth() + 4, search.getY() + (search.getHeight() - 16) / 2f);
        sortView.move(mouseX, mouseY);
        sortView.draw(g, screen.width, screen.height, false);
    }

    boolean click(double x, double y, int button) {
        if (sortView.hits(x, y)) {
            sortView.down(x, y, button);
            return false;
        }
        ListDrag d = drag();
        if (d != null && m.worldDragActive()) d.press(x, y, button);
        return true;
    }

    boolean release(double x, double y, int button) {
        ListDrag d = drag();
        if (d != null && d.release()) return false;
        return !(sortView.up(x, y, button) && sortView.hits(x, y));
    }

    boolean key(int key) {
        ListDrag d = drag();
        if (key == Keys.ESCAPE && d != null && d.dragging()) {
            d.cancel();
            return false;
        }
        return true;
    }

    void close() {
        if (drag != null) drag.cancel();
        VuiMc.vui().close(sortView.document());
    }
}
