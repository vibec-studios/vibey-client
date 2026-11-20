package dev.henny.vibeyclient.module.waypoints.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class WaypointList {
    public static final int MAX = 512;

    private final List<Waypoint> items = new ArrayList<>();
    private final Supplier<String> ids;
    private int revision;

    public WaypointList(Supplier<String> ids) {
        this.ids = ids;
    }

    public List<Waypoint> all() {
        return Collections.unmodifiableList(items);
    }

    public int size() {
        return items.size();
    }

    public int revision() {
        return revision;
    }

    public void changed() {
        revision++;
    }

    public String newId() {
        return ids.get();
    }

    public Supplier<String> ids() {
        return ids;
    }

    public Waypoint get(String id) {
        for (Waypoint w : items) if (w.id().equals(id)) return w;
        return null;
    }

    public boolean add(Waypoint w) {
        if (items.size() >= MAX || get(w.id()) != null) return false;
        items.add(w);
        changed();
        return true;
    }

    public Waypoint remove(String id) {
        Waypoint w = get(id);
        if (w != null && items.remove(w)) changed();
        return w;
    }

    public void replaceAll(List<Waypoint> list) {
        items.clear();
        for (Waypoint w : list) if (items.size() < MAX && get(w.id()) == null) items.add(w);
        changed();
    }

    public List<Waypoint> in(String dimension) {
        List<Waypoint> out = new ArrayList<>();
        for (Waypoint w : items) if (w.dimension().equals(dimension)) out.add(w);
        return out;
    }

    public Waypoint death() {
        for (Waypoint w : items) if (w.kind() == WaypointKind.DEATH) return w;
        return null;
    }

    public Waypoint markDeath(String name, Pos pos, String dimension, long now) {
        Waypoint old = death();
        if (old != null) items.remove(old);
        Waypoint w = new Waypoint(ids.get(), name, pos, dimension, Colors.DEATH).kind(WaypointKind.DEATH).created(now);
        items.add(0, w);
        changed();
        return w;
    }

    public int normalCount() {
        int n = 0;
        for (Waypoint w : items) if (w.kind() == WaypointKind.NORMAL) n++;
        return n;
    }
}
