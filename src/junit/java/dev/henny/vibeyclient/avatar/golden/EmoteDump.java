package dev.henny.vibeyclient.avatar.golden;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.anim.EmotePlay;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.render.AvatarHooks;
import net.minecraft.client.model.geom.ModelPart;

import java.util.List;
import java.util.Map;
import java.util.UUID;

final class EmoteDump {
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
        AvatarState st = new AvatarState(UUID.fromString("00000000-0000-0000-0000-000000000002"), true);
        st.emote = e;
        ModelPart[] parts = new ModelPart[6];
        for (int k = 0; k < 6; k++) {
            ModelPart m = new ModelPart(List.of(), Map.of());
            int i = k + 1;
            m.x = i;
            m.y = 2 * i;
            m.z = -i;
            m.xRot = 0.1f * i;
            m.yRot = 0.05f * i;
            m.zRot = -0.02f * i;
            m.xScale = m.yScale = m.zScale = 1f + 0.1f * i;
            parts[k] = m;
        }
        AvatarHooks.emotePose(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], st);
        for (int k = 0; k < 6; k++) {
            ModelPart m = parts[k];
            o.line("part", k, Out.f(new float[]{m.x, m.y, m.z, m.xRot, m.yRot, m.zRot, m.xScale, m.yScale, m.zScale}));
        }
        PoseStack ps = new PoseStack();
        AvatarHooks.rootTransform(st, ps, 0.9375f);
        o.line("root", Out.m(ps.last().pose()), "hideHeld", AvatarHooks.hideHeldItems(st));
    }
}
