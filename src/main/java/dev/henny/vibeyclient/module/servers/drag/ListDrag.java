package dev.henny.vibeyclient.module.servers.drag;

import dev.henny.vibeyclient.module.servers.core.AutoScroll;
import dev.henny.vibeyclient.module.servers.core.DragTracker;
import dev.henny.vibeyclient.module.servers.core.DropSlot;
import dev.henny.vibeyclient.module.servers.core.GapOffsets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;

import java.util.ArrayList;
import java.util.List;

public final class ListDrag {
    private static final int HIDDEN_Y = -100_000;
    private static ListDrag active;

    private final ObjectSelectionList<?> list;
    private final DragTarget target;
    private final DragTracker tracker = new DragTracker();
    private List<ObjectSelectionList.Entry<?>> rows = List.of();
    private float[] offsets = new float[0];
    private int[] base = new int[0];
    private int slot = -1, lo, hi, grab;
    private long last;
    private boolean shifted;

    public ListDrag(ObjectSelectionList<?> list, DragTarget target) {
        this.list = list;
        this.target = target;
    }

    public static ListDrag active() {
        return active;
    }

    public boolean dragging() {
        return tracker.dragging();
    }

    public int slot() {
        return slot;
    }

    public int from() {
        return tracker.from();
    }

    public boolean press(double x, double y, int button) {
        if (button != 0 || !list.isMouseOver(x, y)) return false;
        rows = rows();
        for (int i = 0; i < rows.size(); i++) {
            ObjectSelectionList.Entry<?> e = rows.get(i);
            if (!e.isMouseOver(x, y)) continue;
            if (!target.grabbable(e, x, y)) return false;
            tracker.press(i, x, y);
            grab = (int) (y - e.getY());
            active = this;
            return true;
        }
        return false;
    }

    public void frame(double x, double y) {
        if (!tracker.pressed()) return;
        if (tracker.move(x, y)) begin();
        if (!tracker.dragging()) return;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;
        float step = AutoScroll.step(y, list.getY(), list.getBottom());
        if (step != 0) list.setScrollAmount(list.scrollAmount() + step * dt * 60f);
        int h = rows.get(tracker.from()).getHeight();
        int[] tops = new int[rows.size()], heights = new int[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            tops[i] = rows.get(i).getY();
            heights[i] = rows.get(i).getHeight();
        }
        slot = DropSlot.clamp(DropSlot.of(tops, heights, y - grab + h / 2.0), lo, hi);
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = GapOffsets.approach(offsets[i], GapOffsets.target(i, tracker.from(), slot, h), dt);
        }
    }

    private void begin() {
        rows = rows();
        offsets = new float[rows.size()];
        base = new int[rows.size()];
        int[] range = DropSlot.group(target.groups(rows), tracker.from());
        lo = range[0];
        hi = range[1];
        slot = tracker.from();
        last = System.nanoTime();
    }

    public boolean release() {
        boolean was = tracker.dragging();
        int from = tracker.release();
        active = null;
        if (!was || from < 0 || from >= rows.size()) return false;
        if (!DropSlot.noop(from, slot)) target.drop(rows, from, slot);
        return true;
    }

    public void cancel() {
        tracker.cancel();
        if (active == this) active = null;
    }

    public void drawFloating(GuiGraphics g, int mouseY, float delta) {
        if (!tracker.dragging() || tracker.from() >= rows.size()) return;
        ObjectSelectionList.Entry<?> e = rows.get(tracker.from());
        int keep = e.getY();
        int y = Math.max(list.getY(), Math.min(list.getBottom() - e.getHeight(), mouseY - grab));
        e.setY(y);
        DragPaint.floating(g, e.getX(), y, e.getWidth(), e.getHeight());
        e.renderContent(g, -1000, -1000, false, delta);
        e.setY(keep);
    }

    public static void shift(Object owner) {
        ListDrag d = active;
        if (d == null || d.list != owner || !d.tracker.dragging() || d.base.length != d.rows.size()) return;
        for (int i = 0; i < d.rows.size(); i++) {
            ObjectSelectionList.Entry<?> e = d.rows.get(i);
            d.base[i] = e.getY();
            e.setY(i == d.tracker.from() ? HIDDEN_Y : d.base[i] + Math.round(d.offsets[i]));
        }
        d.shifted = true;
    }

    public static void restore(Object owner, GuiGraphics g) {
        ListDrag d = active;
        if (d == null || d.list != owner || !d.shifted) return;
        d.shifted = false;
        for (int i = 0; i < d.rows.size(); i++) d.rows.get(i).setY(d.base[i]);
        if (d.rows.isEmpty()) return;
        ObjectSelectionList.Entry<?> e = d.rows.get(d.tracker.from());
        int y = d.base[0] + GapOffsets.gap(d.tracker.from(), d.slot) * e.getHeight();
        DragPaint.gap(g, e.getX(), y, e.getWidth(), e.getHeight());
    }

    private List<ObjectSelectionList.Entry<?>> rows() {
        List<ObjectSelectionList.Entry<?>> out = new ArrayList<>();
        for (Object o : list.children()) {
            if (o instanceof ObjectSelectionList.Entry<?> e && target.row(e)) out.add(e);
        }
        return out;
    }
}
