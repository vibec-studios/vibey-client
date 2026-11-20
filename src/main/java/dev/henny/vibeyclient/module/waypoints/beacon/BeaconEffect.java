package dev.henny.vibeyclient.module.waypoints.beacon;

import dev.henny.r3.anchor.Facing;
import dev.henny.r3.fx.Effect;
import dev.henny.r3.fx.EffectContext;
import dev.henny.r3.object.Transform;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.panel.PointerContent;
import dev.henny.r3.render.WorldCanvas;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;

final class BeaconEffect implements Effect, PointerContent {
    static final String CONTROL = "navigate";

    private final Waypoint waypoint;
    private final boolean foreign;
    private final BeaconLabels labels = new BeaconLabels();
    private final BeaconAnchor anchor = new BeaconAnchor(this);
    private final BeaconState state;
    private final BeaconMoves moves = new BeaconMoves();
    private WorldObject object;
    private Pos shown;
    private float time;
    private int ticks;
    private int labelLevel;

    BeaconEffect(Waypoint waypoint, boolean foreign, Pos shown, BeaconState state) {
        this.waypoint = waypoint;
        this.foreign = foreign;
        this.shown = shown;
        this.state = state;
    }

    void attach(WorldObject object) {
        this.object = object;
    }

    void reachable(boolean near) {
        if (object != null && object.rules().interactive() != near) object.rules().interactive(near);
    }

    Waypoint waypoint() {
        return waypoint;
    }

    boolean foreign() {
        return foreign;
    }

    BeaconAnchor anchor() {
        return anchor;
    }

    Pos shownPos() {
        return shown;
    }

    void shownPos(Pos p) {
        shown = p;
    }

    BeaconMoves moves() {
        return moves;
    }

    BeaconLabels labels() {
        return labels;
    }

    boolean hovered() {
        return state.hovered(object);
    }

    boolean navTarget() {
        return waypoint.id().equals(state.navTargetId());
    }

    double distance() {
        return object == null ? 0 : object.state().distance();
    }

    float time() {
        return time;
    }

    int labelLevel() {
        return labelLevel;
    }

    void labelLevel(int level) {
        labelLevel = level;
    }

    BeaconState state() {
        return state;
    }

    @Override
    public void tick() {
        if (ticks++ % 5 == 0) labels.refresh(this);
    }

    @Override
    public void simulate(float dt, EffectContext context) {
        time += dt;
    }

    @Override
    public void draw(WorldCanvas canvas, EffectContext context) {
        BeaconDraw.draw(canvas, context, this, object.transform(), state);
    }

    @Override
    public String controlAt(float x, float y) {
        return CONTROL;
    }

    @Override
    public Facing facing() {
        return Facing.CAMERA;
    }

    @Override
    public double boundingRadius(Transform transform) {
        return transform.worldWidth() * 3;
    }
}
