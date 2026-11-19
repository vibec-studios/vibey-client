package dev.henny.cosmetico.golden;

import dev.henny.cosmetico.Engine;
import dev.henny.cosmetico.draw.CosmeticMix;
import dev.henny.cosmetico.model.Animation;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.OffsetRule;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemSource;
import dev.henny.cosmetico.skeleton.BonePose;
import dev.henny.cosmetico.skeleton.LocatorMath;
import dev.henny.cosmetico.skeleton.Mix;
import dev.henny.cosmetico.skeleton.Pass;
import dev.henny.cosmetico.skeleton.Rig;
import dev.henny.cosmetico.state.Avatar;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public final class PoseDump {
    private PoseDump() {}

    public static final float[] FRACTIONS = {0.37f, 0.81f};
    public static final double NOW = 12.345, NEXT = 12.445;

    public static String dump(Bundle b, Map<Slot, Bundle> others) {
        Out o = new Out();
        Rig rig = Rig.of(b);
        BonePose pose = new BonePose();
        Mix mix = new Mix();
        o.line("case", "rest");
        render(o, rig, pose, mix.clear());
        for (Animation a : b.animations.values()) {
            for (float fr : FRACTIONS) {
                float t = a.length * fr;
                o.line("case", "anim", a.name, t);
                render(o, rig, pose, mix.single(a, t));
            }
        }
        if (!b.isEmote()) state(o, rig, pose, b, others);
        return o.toString();
    }

    private static void state(Out o, Rig rig, BonePose pose, Bundle b, Map<Slot, Bundle> others) {
        Engine engine = TestHost.engine();
        Map<String, Item> reg = new HashMap<>();
        Avatar a = StateSetup.avatar(b);
        for (OffsetRule r : b.offsetWhen) {
            Bundle other = others.get(r.slot());
            if (other == null) continue;
            o.line("other", r.slot(), other.id);
            reg.put(other.id, Item.of(other, ItemSource.BUILTIN, 0, null, null, 1));
            a.loadout.base[r.slot().ordinal()] = other.id;
        }
        engine.items.putAll(ItemSource.BUILTIN, reg, true);
        engine.items.put(Item.of(b, ItemSource.DEV, 0, null, null, 2));
        CosmeticMix mixer = engine.cosmeticMix();
        for (double now : new double[]{NOW, NEXT}) {
            float[] w = new float[8];
            mixer.weights().weights(a, b, b.slot, w);
            o.line("case", "state", now, "weights", Out.f(w));
            render(o, rig, pose, mixer.of(a, b, b.slot, now));
        }
    }

    private static void render(Out o, Rig rig, BonePose pose, Mix mix) {
        o.line("mix", mix.count, "visibility", mix.visibility == null ? "null" : mix.visibility.name, mix.visibilityTime,
                "root", mix.hasRootOffset, Out.f(mix.rootOffset), Out.f(mix.rootRotation));
        for (int k = 0; k < mix.count; k++) o.line("play", mix.anims[k].name, mix.times[k], mix.weights[k]);
        for (Pass pass : Pass.values()) {
            o.line("pass", pass);
            pose.compute(rig, mix, Frames.basePose(), Frames.baseNormal(), pass == Pass.ATTACHED ? Frames.PARTS : null, pass, (l, mat, pivot) -> {
                Vector3f v = LocatorMath.local(l, pivot, new Vector3f());
                mat.transformPosition(v);
                o.line("locator", l.name(), v.x, v.y, v.z);
            });
            for (int i = 0; i < pose.count(); i++) {
                if (pose.drawn(i)) o.line("bone", i, pose.visible(i), Out.m(pose.matrix(i)), Out.n(pose.normal(i)));
            }
        }
    }
}
