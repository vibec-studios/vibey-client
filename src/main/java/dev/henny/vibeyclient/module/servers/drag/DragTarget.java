package dev.henny.vibeyclient.module.servers.drag;

import net.minecraft.client.gui.components.ObjectSelectionList;

import java.util.List;

public interface DragTarget {
    boolean row(ObjectSelectionList.Entry<?> entry);

    boolean grabbable(ObjectSelectionList.Entry<?> entry, double x, double y);

    boolean[] groups(List<ObjectSelectionList.Entry<?>> rows);

    void drop(List<ObjectSelectionList.Entry<?>> rows, int from, int slot);
}
