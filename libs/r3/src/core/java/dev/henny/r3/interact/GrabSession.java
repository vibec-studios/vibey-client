package dev.henny.r3.interact;

import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.anchor.BlockHit;
import dev.henny.r3.anchor.FaceSnap;
import dev.henny.r3.anchor.FreeAnchor;
import dev.henny.r3.anchor.WorldQuery;
import dev.henny.r3.math.Axes;
import dev.henny.r3.math.Face;
import dev.henny.r3.math.Ray;
import dev.henny.r3.object.Transform;
import org.joml.Vector3d;

public final class GrabSession {
    private final InteractionConfig config;
    private final BlockHit blockHit = new BlockHit();
    private final Vector3d point = new Vector3d();
    private final Vector3d right = new Vector3d();
    private final Vector3d up = new Vector3d();
    private Interactable target;
    private PoseSnapshot before;
    private boolean isNew;
    private boolean clickToPlace;
    private boolean surfaceFirst;
    private boolean snapped;
    private double holdDistance;
    private double offsetX;
    private double offsetY;
    private int quarterTurns;

    public GrabSession(InteractionConfig config) {
        this.config = config;
    }

    public void begin(Interactable target, double distance, double offsetX, double offsetY, boolean clickToPlace) {
        start(target, distance, offsetX, offsetY, false, clickToPlace, false);
    }

    public void beginNew(Interactable target) {
        start(target, config.placeDistance, 0, 0, true, true, true);
    }

    private void start(Interactable t, double distance, double ox, double oy, boolean fresh, boolean click, boolean surface) {
        target = t;
        before = PoseSnapshot.capture(t);
        isNew = fresh;
        clickToPlace = click;
        surfaceFirst = surface;
        snapped = false;
        holdDistance = clampDistance(distance);
        offsetX = ox;
        offsetY = oy;
        quarterTurns = 0;
    }

    public void follow(Pointer pointer, WorldQuery query) {
        if (target == null) return;
        Ray ray = pointer.ray();
        if (query != null && target.rules().snapToBlocks()) {
            double reach = surfaceFirst ? config.maxReach : holdDistance + config.snapReach;
            if (query.raycastBlocks(ray.origin(), ray.direction(), reach, blockHit)) {
                snapTo(pointer.viewerYaw());
                if (surfaceFirst) holdDistance = clampDistance(blockHit.distance());
                return;
            }
        }
        floatFree(ray);
    }

    private void snapTo(float viewerYaw) {
        Transform t = target.transform();
        Face face = blockHit.face();
        Face upFace = FaceSnap.upFor(face, viewerYaw, quarterTurns);
        FaceSnap.orientation(face, upFace, t.orientation());
        t.right(right);
        t.up(up);
        point.set(blockHit.point()).fma(-offsetX, right).fma(-offsetY, up);
        FaceSnap.pose(point, face, upFace, config.faceOffset, config.grid, t);
        target.anchor(BlockFaceAnchor.of(blockHit, upFace));
        snapped = true;
    }

    private void floatFree(Ray ray) {
        Transform t = target.transform();
        if (target.rules().turnToViewer()) {
            Axes.facing(-ray.direction().x(), -ray.direction().y(), -ray.direction().z(), t.orientation());
        }
        t.right(right);
        t.up(up);
        ray.pointAt(holdDistance, point);
        t.position().set(point).fma(-offsetX, right).fma(-offsetY, up);
        target.anchor(FreeAnchor.INSTANCE);
        snapped = false;
    }

    public void rotate() {
        quarterTurns = (quarterTurns + 1) % 4;
    }

    public void adjustDistance(double notches) {
        surfaceFirst = false;
        holdDistance = clampDistance(holdDistance + notches * config.distanceStep);
    }

    private double clampDistance(double d) {
        return Math.max(config.minHoldDistance, Math.min(config.maxHoldDistance, d));
    }

    public void end() {
        target = null;
        before = null;
    }

    public Interactable target() {
        return target;
    }

    public PoseSnapshot before() {
        return before;
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean clickToPlace() {
        return clickToPlace;
    }

    public boolean snapped() {
        return snapped;
    }

    public double holdDistance() {
        return holdDistance;
    }

    public int quarterTurns() {
        return quarterTurns;
    }
}
