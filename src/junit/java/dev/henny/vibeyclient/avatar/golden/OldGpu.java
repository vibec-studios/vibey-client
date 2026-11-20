package dev.henny.vibeyclient.avatar.golden;

import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.render.BundleMesh;
import dev.henny.vibeyclient.avatar.render.GpuBundle;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

final class OldGpu {
    private OldGpu() {}

    static GpuBundle of(Bundle b) throws Exception {
        Field uf = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        uf.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) uf.get(null);
        GpuBundle gb = (GpuBundle) unsafe.allocateInstance(GpuBundle.class);
        BundleMesh mesh = BundleMesh.compile(b);
        int n = b.textures.length;
        set(gb, "bundle", b);
        set(gb, "mesh", mesh);
        set(gb, "textures", new dev.henny.vibeyclient.avatar.render.Tex[n]);
        set(gb, "strips", new com.mojang.blaze3d.platform.NativeImage[n]);
        set(gb, "shownKey", new int[n]);
        Map<Bundle.Animation, Bundle.Track[]> tracks = new IdentityHashMap<>();
        for (Bundle.Animation a : b.animations.values()) {
            Bundle.Track[] tr = new Bundle.Track[mesh.parts.length];
            for (Bundle.Track t : a.tracks) if (t.bone() >= 0 && t.bone() < tr.length) tr[t.bone()] = t;
            tracks.put(a, tr);
        }
        set(gb, "boneTracks", tracks);
        return gb;
    }

    static Object get(Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    private static void set(Object o, String name, Object v) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(o, v);
    }
}
