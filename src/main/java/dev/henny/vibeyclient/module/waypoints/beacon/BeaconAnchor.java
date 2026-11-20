package dev.henny.vibeyclient.module.waypoints.beacon;

import dev.henny.r3.anchor.Anchor;
import dev.henny.r3.anchor.AnchorContext;
import dev.henny.r3.anchor.AnchorKind;
import dev.henny.r3.object.Transform;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import org.joml.Vector3dc;

final class BeaconAnchor implements Anchor {
    static final double NEAR = 40;
    static final double LIFT = 1.5;
    static final double GROW_FROM = 10;
    static final double NEAR_HIDE = 2;

    private final BeaconEffect beacon;
    private double factor = 1;
    private boolean clamped;

    BeaconAnchor(BeaconEffect beacon) {
        this.beacon = beacon;
    }

    static boolean standingAt(net.minecraft.world.entity.player.Player player, Pos p) {
        double dx = p.x() + 0.5 - player.getX(), dz = p.z() + 0.5 - player.getZ(), dy = p.y() - player.getY();
        return dx * dx + dz * dz < NEAR_HIDE * NEAR_HIDE && Math.abs(dy) < 3;
    }

    double factor() {
        return factor;
    }

    boolean clamped() {
        return clamped;
    }

    @Override
    public AnchorKind kind() {
        return AnchorKind.FREE;
    }

    @Override
    public boolean resolve(AnchorContext context, Transform target) {
        Pos p = beacon.shownPos();
        if (p == null || context.camera() == null) return false;
        Vector3dc cam = context.camera().position();
        double x = p.x() + 0.5, y = p.y() + LIFT, z = p.z() + 0.5;
        double dx = x - cam.x(), dy = y - cam.y(), dz = z - cam.z();
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        clamped = d > NEAR;
        beacon.reachable(!clamped);
        double k = clamped ? NEAR / d : 1;
        target.position(cam.x() + dx * k, cam.y() + dy * k, cam.z() + dz * k);
        factor = Math.max(1, Math.min(d, NEAR) / GROW_FROM);
        target.scale((float) (beacon.waypoint().size() * factor));
        return true;
    }

    @Override
    public Anchor copy() {
        return this;
    }
}
