package dev.henny.r3.persist;

import com.google.gson.JsonObject;
import dev.henny.r3.interact.SizeLimits;
import dev.henny.r3.object.Transform;
import dev.henny.r3.object.WorldObject;
import org.joml.Quaternionf;

import java.util.UUID;

public final class PlacementCodec {
    public static final double MAX_HORIZONTAL = 3.2e7;
    public static final double MAX_VERTICAL = 4096;
    public static final float MAX_SCALE = 64f;

    private PlacementCodec() {}

    public static PlacementRecord encode(WorldObject o, JsonObject data) {
        PlacementRecord r = new PlacementRecord();
        Transform t = o.transform();
        r.id = o.id();
        r.kind = o.kind();
        r.owner = o.owner() == null ? null : o.owner().toString();
        r.dimension = o.dimension();
        r.x = t.position().x;
        r.y = t.position().y;
        r.z = t.position().z;
        Quaternionf q = t.orientation();
        r.qx = q.x;
        r.qy = q.y;
        r.qz = q.z;
        r.qw = q.w;
        r.width = t.width();
        r.height = t.height();
        r.scale = t.scale();
        r.locked = o.lockedByUser();
        r.fullBright = o.fullBright();
        r.revision = o.revision();
        r.anchor = AnchorCodec.encode(o.anchor());
        r.data = data;
        return r;
    }

    public static boolean valid(PlacementRecord r) {
        if (r == null || r.id == null || r.id.isBlank() || r.kind == null) return false;
        if (!finite(r.x, MAX_HORIZONTAL) || !finite(r.y, MAX_VERTICAL) || !finite(r.z, MAX_HORIZONTAL)) return false;
        if (!Float.isFinite(r.qx) || !Float.isFinite(r.qy) || !Float.isFinite(r.qz) || !Float.isFinite(r.qw)) return false;
        float len = r.qx * r.qx + r.qy * r.qy + r.qz * r.qz + r.qw * r.qw;
        if (len < 1e-6f) return false;
        if (r.owner != null && !isUuid(r.owner)) return false;
        return Float.isFinite(r.width) && Float.isFinite(r.height) && Float.isFinite(r.scale);
    }

    public static WorldObject decode(PlacementRecord r, SizeLimits limits) {
        if (!valid(r)) throw new IllegalArgumentException("invalid placement record");
        WorldObject o = new WorldObject(r.id, r.kind);
        apply(r, o, limits);
        return o;
    }

    public static void apply(PlacementRecord r, WorldObject o, SizeLimits limits) {
        Transform t = o.transform();
        t.position(r.x, r.y, r.z);
        t.orientation().set(r.qx, r.qy, r.qz, r.qw).normalize();
        float scale = Math.max(0.01f, Math.min(MAX_SCALE, r.scale));
        t.scale(scale);
        t.size(limits.clampWidth(r.width * scale) / scale, limits.clampHeight(r.height * scale) / scale);
        o.dimension(r.dimension)
                .owner(r.owner == null ? null : UUID.fromString(r.owner))
                .locked(r.locked)
                .fullBright(r.fullBright)
                .revision(r.revision)
                .anchor(AnchorCodec.decode(r.anchor));
        o.state().markLayoutDirty();
    }

    private static boolean finite(double v, double max) {
        return Double.isFinite(v) && Math.abs(v) <= max;
    }

    private static boolean isUuid(String s) {
        try {
            UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
