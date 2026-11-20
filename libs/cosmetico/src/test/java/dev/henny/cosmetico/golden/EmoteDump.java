package dev.henny.cosmetico.golden;

import dev.henny.cosmetico.anim.EmotePlay;
import dev.henny.cosmetico.config.CosmeticoConfig;
import dev.henny.cosmetico.draw.EmoteVisibility;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.PlayerBone;
import dev.henny.cosmetico.pose.EmotePartPose;
import dev.henny.cosmetico.pose.HeldItems;
import dev.henny.cosmetico.pose.PartPose;
import dev.henny.cosmetico.pose.RootPose;
import dev.henny.cosmetico.state.Avatar;
import org.joml.Matrix4f;

import java.util.UUID;

public final class EmoteDump {
    private EmoteDump() {}

    static final double START = 10.0;

    static void dump(Out o, Bundle b) {
        float len = b.emoteAnimation().length;
        EmotePlay e = new EmotePlay(b.id, b, START);
        for (double now : new double[]{START, START + 0.05, START + 0.1, START + 0.2, START + 0.5 * len, START + len, START + len + 0.1, START + 2.3 * len}) {
            e.update(now);
            line(o, "play", now, e);
        }
        EmotePlay s = new EmotePlay(b.id, b, START);
        double at = START + 0.6 * len;
        s.update(at);
        s.stop(at);
        for (double now : new double[]{at, at + 0.05, at + 0.1, at + 0.2}) {
            s.update(now);
            line(o, "stop", now, s);
        }
        EmotePlay p = new EmotePlay(b.id, b, START);
        p.update(START + 0.5 * len);
        pose(o, p);
    }

    private static void line(Out o, String what, double now, EmotePlay e) {
        o.line(what, now, e.localTime, e.weight, e.stopping(), e.finished(now), e.loops());
        o.line("pose", Out.f(e.pose), Out.i(e.channels));
    }

    private static void pose(Out o, EmotePlay e) {
        Avatar a = new Avatar(UUID.fromString("00000000-0000-0000-0000-000000000002"), true);
        a.emote = e;
        EmotePlay shown = EmoteVisibility.visible(a, new CosmeticoConfig());
        for (int k = 0; k < 6; k++) {
            int i = k + 1;
            PartPose part = new PartPose().set(i, 2 * i, -i, 0.1f * i, 0.05f * i, -0.02f * i, 1f + 0.1f * i, 1f + 0.1f * i, 1f + 0.1f * i);
            if (EmotePartPose.active(shown)) EmotePartPose.blend(shown, PlayerBone.VALUES[k], part);
            o.line("part", k, Out.f(new float[]{part.x, part.y, part.z, part.xRot, part.yRot, part.zRot, part.xScale, part.yScale, part.zScale}));
        }
        RootPose root = new RootPose();
        Matrix4f m = new Matrix4f();
        if (root.compute(shown, 0.9375f)) {
            if (root.translate) m.translate(root.tx, root.ty, root.tz);
            if (root.rotate) m.rotate(root.rotation);
            if (root.scale) m.scale(root.sx, root.sy, root.sz);
        }
        o.line("root", Out.m(m), "hideHeld", HeldItems.hide(shown));
    }
}
