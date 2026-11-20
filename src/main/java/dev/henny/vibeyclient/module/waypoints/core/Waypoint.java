package dev.henny.vibeyclient.module.waypoints.core;

public final class Waypoint {
    public static final int MAX_NAME = 32;
    public static final float MIN_SIZE = 0.25f;
    public static final float MAX_SIZE = 6f;

    private final String id;
    private String name;
    private Pos pos;
    private String dimension;
    private int color;
    private float size = 1f;
    private boolean visible = true;
    private WaypointKind kind = WaypointKind.NORMAL;
    private long created;

    public Waypoint(String id, String name, Pos pos, String dimension, int color) {
        this.id = id;
        this.pos = pos;
        this.dimension = dimension;
        name(name);
        color(color);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Waypoint name(String name) {
        this.name = Names.clean(name);
        return this;
    }

    public Pos pos() {
        return pos;
    }

    public Waypoint pos(Pos pos) {
        this.pos = pos;
        return this;
    }

    public String dimension() {
        return dimension;
    }

    public Waypoint dimension(String dimension) {
        this.dimension = dimension;
        return this;
    }

    public int color() {
        return color;
    }

    public Waypoint color(int rgb) {
        this.color = rgb & 0xFFFFFF;
        return this;
    }

    public float size() {
        return size;
    }

    public Waypoint size(float size) {
        this.size = Float.isFinite(size) ? Math.max(MIN_SIZE, Math.min(MAX_SIZE, size)) : 1f;
        return this;
    }

    public boolean visible() {
        return visible;
    }

    public Waypoint visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public WaypointKind kind() {
        return kind;
    }

    public Waypoint kind(WaypointKind kind) {
        this.kind = kind == null ? WaypointKind.NORMAL : kind;
        return this;
    }

    public long created() {
        return created;
    }

    public Waypoint created(long created) {
        this.created = created;
        return this;
    }

    public Waypoint copy(String newId) {
        Waypoint w = new Waypoint(newId, name, pos, dimension, color);
        return w.size(size).visible(visible).kind(kind).created(created);
    }
}
