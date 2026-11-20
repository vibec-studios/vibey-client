package dev.henny.companio.sync;

import dev.henny.companio.def.PropDef;
import dev.henny.companio.world.Companion;

import java.util.ArrayList;
import java.util.List;

public final class Snapshots {
    private Snapshots() {}

    public static Snapshot of(Companion c, String hash, String dimension) {
        List<String> layers = new ArrayList<>();
        c.animator.layers(n -> {
            if (layers.size() < 4) layers.add(n);
        });
        List<String> props = new ArrayList<>();
        for (PropDef p : c.def.props().values()) if (c.propShown(p.id())) props.add(p.id());
        String base = c.animator.baseName();
        return new Snapshot(c.def.packId(), hash, c.def.id(), c.variant(), c.name(), c.size(), c.body.x, c.body.y, c.body.z,
                c.body.yaw, dimension, base == null ? "" : base, layers, props, c.bubbleText(), c.held());
    }

    public static boolean moved(Snapshot a, Snapshot b) {
        if (a == null || b == null) return true;
        double dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
        return dx * dx + dy * dy + dz * dz > 0.0025 || Math.abs(a.yaw() - b.yaw()) > 4 || !a.base().equals(b.base())
                || !a.layers().equals(b.layers()) || !a.props().equals(b.props()) || !a.bubble().equals(b.bubble())
                || !a.name().equals(b.name()) || a.size() != b.size() || !a.variant().equals(b.variant()) || a.held() != b.held();
    }
}
