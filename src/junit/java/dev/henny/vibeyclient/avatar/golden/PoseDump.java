package dev.henny.vibeyclient.avatar.golden;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.avatar.AvatarItem;
import dev.henny.vibeyclient.avatar.AvatarItems;
import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.ItemSource;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.render.AvatarDraw;
import dev.henny.vibeyclient.avatar.render.BoneRenderer;
import dev.henny.vibeyclient.avatar.render.GpuBundle;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import dev.henny.vibeyclient.avatar.render.PlayerParts;

final class PoseDump {
    private PoseDump() {}

    static final float[] FRACTIONS = {0.37f, 0.81f};
    static final double NOW = 12.345, NEXT = 12.445;

    static String dump(Bundle b, Map<Slot, Bundle> others) throws Exception {
        Out o = new Out();
        GpuBundle gb = OldGpu.of(b);
        BoneRenderer.Mix mix = new BoneRenderer.Mix();
        o.line("case", "rest");
        render(o, gb, mix.clear());
        for (Bundle.Animation a : b.animations.values()) {
            for (float fr : FRACTIONS) {
                float t = a.length * fr;
                mix.clear().add(a, t, 1f);
                mix.visibility = a;
                mix.visibilityTime = t;
                o.line("case", "anim", a.name, t);
                render(o, gb, mix);
            }
        }
        if (!b.isEmote()) state(o, gb, b, others);
        return o.toString();
    }

    private static void state(Out o, GpuBundle gb, Bundle b, Map<Slot, Bundle> others) throws Exception {
        Map<String, AvatarItem> reg = new HashMap<>();
        AvatarState st = StateSetup.avatar(b);
        for (Bundle.OffsetRule r : b.offsetWhen) {
            Bundle other = others.get(r.slot());
            if (other == null) continue;
            o.line("other", r.slot(), other.id);
            reg.put(other.id, AvatarItem.of(other, ItemSource.BUILTIN, 0, null, null, 1));
            st.slots[r.slot().ordinal()] = other.id;
        }
        AvatarItems.putAll(ItemSource.BUILTIN, reg, true);
        AvatarItems.put(AvatarItem.of(b, ItemSource.DEV, 0, null, null, 2));
        try {
            Method m = AvatarDraw.class.getDeclaredMethod("cosmeticMix", AvatarState.class, Bundle.class, Slot.class, double.class);
            m.setAccessible(true);
            for (double now : new double[]{NOW, NEXT}) {
                float[] w = new float[8];
                AvatarDraw.weights(st, b, b.slot, w);
                o.line("case", "state", now, "weights", Out.f(w));
                render(o, gb, (BoneRenderer.Mix) m.invoke(null, st, b, b.slot, now));
            }
        } finally {
            AvatarItems.remove(b.id, ItemSource.DEV);
            AvatarItems.putAll(ItemSource.BUILTIN, Map.of(), true);
        }
    }

    private static void render(Out o, GpuBundle gb, BoneRenderer.Mix mix) throws Exception {
        o.line("mix", mix.count, "visibility", mix.visibility == null ? "null" : mix.visibility.name, mix.visibilityTime,
                "root", mix.hasRootOffset, Out.f(mix.rootOffset), Out.f(mix.rootRotation));
        for (int k = 0; k < mix.count; k++) o.line("play", mix.anims[k].name, mix.times[k], mix.weights[k]);
        for (BoneRenderer.Pass pass : BoneRenderer.Pass.values()) {
            PoseStack ps = new PoseStack();
            ps.last().pose().set(Frames.basePose());
            ps.last().normal().set(Frames.baseNormal());
            o.line("pass", pass);
            PlayerParts parts = pass == BoneRenderer.Pass.ATTACHED ? (part, p) -> Frames.apply(part, p.last().pose(), p.last().normal()) : null;
            BoneRenderer.render(gb, mix, ps, null, parts, pass, 0, -1, (l, mat, pivot) -> {
                Vector3f v = BoneRenderer.locatorLocal(l, pivot, new Vector3f());
                mat.transformPosition(v);
                o.line("locator", l.name(), v.x, v.y, v.z);
            });
            bones(o, gb.bundle.model.bones.length);
        }
    }

    private static void bones(Out o, int n) throws Exception {
        Matrix4f[] mats = (Matrix4f[]) OldGpu.get(BoneRenderer.class, "mats");
        Matrix3f[] norms = (Matrix3f[]) OldGpu.get(BoneRenderer.class, "norms");
        boolean[] visible = (boolean[]) OldGpu.get(BoneRenderer.class, "visible");
        boolean[] drawn = (boolean[]) OldGpu.get(BoneRenderer.class, "drawn");
        for (int i = 0; i < n; i++) if (drawn[i]) o.line("bone", i, visible[i], Out.m(mats[i]), Out.n(norms[i]));
    }
}
