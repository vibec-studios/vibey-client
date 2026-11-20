package dev.henny.vibeyclient.module.servers.worlds;

import dev.henny.vibeyclient.module.servers.ServersModule;
import dev.henny.vibeyclient.module.servers.core.WorldOrder;
import dev.henny.vibeyclient.module.servers.core.WorldSort;
import dev.henny.vibeyclient.module.servers.drag.DragTarget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;

import java.util.ArrayList;
import java.util.List;

final class WorldDrop implements DragTarget {
    static final int ICON = 32;

    private final ServersModule m;
    private final Runnable refill;

    WorldDrop(ServersModule m, Runnable refill) {
        this.m = m;
        this.refill = refill;
    }

    @Override
    public boolean row(ObjectSelectionList.Entry<?> entry) {
        return entry instanceof WorldSelectionList.WorldListEntry;
    }

    @Override
    public boolean grabbable(ObjectSelectionList.Entry<?> entry, double x, double y) {
        return x >= entry.getX() + 2 + ICON;
    }

    @Override
    public boolean[] groups(List<ObjectSelectionList.Entry<?>> rows) {
        return new boolean[rows.size()];
    }

    @Override
    public void drop(List<ObjectSelectionList.Entry<?>> rows, int from, int slot) {
        List<String> shown = new ArrayList<>(rows.size());
        for (ObjectSelectionList.Entry<?> e : rows) shown.add(((WorldSelectionList.WorldListEntry) e).getLevelSummary().getLevelId());
        List<String> full = WorldListView.full();
        if (!full.containsAll(shown)) return;
        WorldOrderStore.set(WorldOrder.drop(full, shown, from, slot));
        if (m.worldSort.get() != WorldSort.CUSTOM) m.worldSort.set(WorldSort.CUSTOM);
        refill.run();
    }
}
