package dev.henny.vibeyclient.module.screens.edit;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.screens.apply.Item;
import dev.henny.vibeyclient.module.screens.core.Box;
import dev.henny.vibeyclient.module.screens.core.Geometry;
import dev.henny.vibeyclient.module.screens.core.Guide;
import dev.henny.vibeyclient.module.screens.core.Hit;
import dev.henny.vibeyclient.module.screens.core.Placement;
import dev.henny.vibeyclient.module.screens.core.Snap;

import java.util.List;

public final class PointerInput {
    private static boolean recorded;

    private PointerInput() {}

    public static void press(EditSession e, double x, double y, int button) {
        e.mouseX = x;
        e.mouseY = y;
        double rx = e.realX(), ry = e.realY();
        if (e.menu != null) {
            ContextMenu.Entry hit = e.menu.hit(rx, ry);
            e.menu = null;
            if (hit != null) hit.action().run();
            return;
        }
        if (e.toolbar.press(e, rx, ry, button)) return;
        Picking.Picked p = Picking.at(e, x, y);
        if (button == 1) {
            e.selected = p == null ? null : p.item().id;
            e.menu = ContextMenu.open(e, p == null ? null : p.item(), rx, ry);
            return;
        }
        if (button != 0) return;
        if (p == null) {
            e.selected = null;
            return;
        }
        Item item = p.item();
        e.selected = item.id;
        recorded = false;
        Hit.Zone zone = item.hidden ? Hit.Zone.BODY : p.zone();
        Box start = EditActions.real(e, item);
        double sx = item.panel() ? rx : x, sy = item.panel() ? ry : y;
        e.gesture = new Gesture(item.id, zone, sx, sy, start, EditActions.placement(e, item), item.scalable);
    }

    public static void drag(EditSession e, double x, double y) {
        e.mouseX = x;
        e.mouseY = y;
        Gesture g = e.gesture;
        if (g == null) return;
        Item item = e.screen.item(g.id());
        if (item == null) return;
        if (!recorded) {
            if (Math.abs((item.panel() ? e.realX() : x) - g.startX()) < 1 && Math.abs((item.panel() ? e.realY() : y) - g.startY()) < 1) return;
            e.history.record(e.screen.layout(), null, System.currentTimeMillis());
            recorded = true;
        }
        e.guides.clear();
        boolean free = ClientCompat.controlDown();
        List<Box> others = Picking.others(e, item);
        float w = e.screen.width(), h = e.screen.height();
        Box start = g.startVisual();
        double px = item.panel() ? e.realX() : x, py = item.panel() ? e.realY() : y;
        float dx = (float) (px - g.startX()), dy = (float) (py - g.startY());
        Placement next;
        if (g.move()) {
            Box moved = start.shifted(Math.round(dx), Math.round(dy));
            if (!free) {
                Snap.Result r = Snap.move(moved, item.panel() ? List.of() : others, w, h);
                moved = moved.at(r.x(), r.y());
                float sx = item.panel() ? moved.x() - item.base.x() : 0, sy = item.panel() ? moved.y() - item.base.y() : 0;
                for (Guide gd : r.guides()) e.guides.add(new Guide(gd.vertical(), gd.at() - (gd.vertical() ? sx : sy), gd.from(), gd.to()));
            }
            next = Geometry.moved(g.startPlacement(), moved, w, h);
        } else {
            boolean left = g.zone() == Hit.Zone.LEFT;
            float edge = left ? start.x() + dx : start.right() + dx;
            if (!free) {
                Snap.Result r = Snap.edge(edge, true, start, others, w, h);
                edge = r.x();
                e.guides.addAll(r.guides());
            }
            float newW = left ? start.right() - edge : edge - start.x();
            next = Geometry.resized(g.startPlacement(), start, newW, left, w, h, g.scalable());
        }
        EditActions.store(e.screen, e.screen.layout().with(item.id, next));
    }

    public static void release(EditSession e, double x, double y) {
        e.mouseX = x;
        e.mouseY = y;
        e.gesture = null;
        e.guides.clear();
    }

    public static void wheel(EditSession e, double x, double y, double amount) {
        int dir = amount > 0 ? 1 : amount < 0 ? -1 : 0;
        if (dir == 0 || e.menu != null) return;
        if (e.toolbar.wheel(e, e.realX(), e.realY(), dir)) return;
        Picking.Picked p = Picking.at(e, x, y);
        Item item = p != null ? p.item() : e.selectedItem();
        if (item == null) return;
        e.selected = item.id;
        EditActions.wheel(e, item, dir);
    }
}
