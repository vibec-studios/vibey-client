package dev.henny.r3.support;

import dev.henny.r3.anchor.BlockHit;
import dev.henny.r3.anchor.EntityPose;
import dev.henny.r3.anchor.WorldQuery;
import dev.henny.r3.math.Face;
import dev.henny.r3.render.Light;
import org.joml.Vector3dc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FakeWorld implements WorldQuery {
    private final Set<Long> solid = new HashSet<>();
    private final Map<UUID, double[]> entities = new HashMap<>();
    public String dimension = "minecraft:overworld";
    public int light = Light.pack(7, 15);
    public int raycasts;

    public FakeWorld block(int x, int y, int z) {
        solid.add(key(x, y, z));
        return this;
    }

    public FakeWorld wallZ(int z, int x0, int x1, int y0, int y1) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) block(x, y, z);
        return this;
    }

    public FakeWorld floor(int y, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) block(x, y, z);
        return this;
    }

    public FakeWorld entity(UUID id, double x, double y, double z, float height, float yaw, boolean sneaking) {
        entities.put(id, new double[] {x, y, z, height, yaw, sneaking ? 1 : 0});
        return this;
    }

    public void removeEntity(UUID id) {
        entities.remove(id);
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x1FFFFF) << 42 | ((long) y & 0x1FFFFF) << 21 | ((long) z & 0x1FFFFF);
    }

    @Override
    public String dimension() {
        return dimension;
    }

    @Override
    public boolean raycastBlocks(Vector3dc o, Vector3dc d, double max, BlockHit out) {
        raycasts++;
        int x = (int) Math.floor(o.x()), y = (int) Math.floor(o.y()), z = (int) Math.floor(o.z());
        int sx = (int) Math.signum(d.x()), sy = (int) Math.signum(d.y()), sz = (int) Math.signum(d.z());
        double tx = boundary(o.x(), d.x(), x), ty = boundary(o.y(), d.y(), y), tz = boundary(o.z(), d.z(), z);
        double dx = d.x() == 0 ? Double.MAX_VALUE : 1 / Math.abs(d.x());
        double dy = d.y() == 0 ? Double.MAX_VALUE : 1 / Math.abs(d.y());
        double dz = d.z() == 0 ? Double.MAX_VALUE : 1 / Math.abs(d.z());
        while (true) {
            double t;
            Face face;
            if (tx < ty && tx < tz) {
                x += sx; t = tx; tx += dx; face = sx > 0 ? Face.WEST : Face.EAST;
            } else if (ty < tz) {
                y += sy; t = ty; ty += dy; face = sy > 0 ? Face.DOWN : Face.UP;
            } else {
                z += sz; t = tz; tz += dz; face = sz > 0 ? Face.NORTH : Face.SOUTH;
            }
            if (t > max) return false;
            if (solid.contains(key(x, y, z))) {
                out.set(o.x() + d.x() * t, o.y() + d.y() * t, o.z() + d.z() * t, face, x, y, z, t);
                return true;
            }
        }
    }

    private static double boundary(double o, double d, int cell) {
        if (d > 0) return (cell + 1 - o) / d;
        if (d < 0) return (o - cell) / -d;
        return Double.MAX_VALUE;
    }

    @Override
    public boolean entity(UUID id, EntityPose out) {
        double[] e = entities.get(id);
        if (e == null) return false;
        out.set(e[0], e[1], e[2], (float) e[3], (float) e[4], e[5] > 0);
        return true;
    }

    @Override
    public int lightAt(double x, double y, double z) {
        return light;
    }
}
