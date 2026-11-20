package dev.henny.cosmetico.skeleton;

import dev.henny.cosmetico.mesh.BundleMesh;
import dev.henny.cosmetico.model.Animation;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Track;

import java.util.IdentityHashMap;
import java.util.Map;

public final class Rig {
    public final Bundle bundle;
    public final BundleMesh mesh;
    private final Map<Animation, Track[]> boneTracks = new IdentityHashMap<>();

    private Rig(Bundle bundle, BundleMesh mesh) {
        this.bundle = bundle;
        this.mesh = mesh;
        for (Animation a : bundle.animations.values()) boneTracks.put(a, tracksByBone(a, mesh.parts.length));
    }

    public static Rig of(Bundle bundle) {
        return new Rig(bundle, BundleMesh.compile(bundle));
    }

    public static Rig of(Bundle bundle, BundleMesh mesh) {
        return new Rig(bundle, mesh);
    }

    public Track[] tracks(Animation a) {
        return boneTracks.get(a);
    }

    private static Track[] tracksByBone(Animation a, int bones) {
        Track[] tr = new Track[bones];
        for (Track t : a.tracks) if (t.bone() >= 0 && t.bone() < tr.length) tr[t.bone()] = t;
        return tr;
    }
}
