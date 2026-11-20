package dev.henny.r3.mc.demo;

import dev.henny.r3.fx.Emitter;
import dev.henny.r3.fx.EmitterShape;
import dev.henny.r3.fx.ParticleStyle;
import dev.henny.r3.fx.ParticleSystem;
import dev.henny.r3.interact.InteractionRules;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.object.Transform;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.render.RenderMode;
import dev.henny.r3.text.Text3D;
import dev.henny.r3.text.TextStyle;
import org.joml.Vector3d;

final class DemoObjects {
    static final int RING_COLOR = 0xFF7FD4FF;
    static final int TITLE_COLOR = 0xFFFFFFFF;
    static final int TITLE_GLOW = 0xFF5A4BFF;

    private DemoObjects() {}

    static WorldObject title(Transform panel) {
        WorldObject text = R3Mc.text("r3");
        Text3D content = (Text3D) text.content();
        content.style(TextStyle.DEFAULT.withColor(TITLE_COLOR).withGlow(TITLE_GLOW, 0.8f)).pixelSize(0.03f);
        Vector3d at = above(panel, 0.45);
        text.transform().position(at.x, at.y, at.z);
        return text;
    }

    static WorldObject ring(Transform panel) {
        Emitter emitter = new Emitter(EmitterShape.RING).radius(0.45f).rate(40).speed(0.02f, 0.08f)
                .lifetime(0.8f, 1.4f).size(0.025f, 0.045f);
        ParticleSystem ring = new ParticleSystem(128, ParticleStyle.glowDots(RING_COLOR).withPhysics(0f, 0.5f)
                .withMode(RenderMode.ADDITIVE), 7).emitter(emitter);
        WorldObject fx = WorldObject.create("r3:demo-ring").content(ring).rules(new InteractionRules().interactive(false));
        fx.dimension(R3Mc.scene().query().dimension());
        Vector3d at = above(panel, -0.6 - panel.worldHeight());
        fx.transform().position(at.x, at.y, at.z);
        fx.transform().orientation().set(panel.orientation());
        return fx;
    }

    private static Vector3d above(Transform panel, double extra) {
        Vector3d up = panel.orientation().transform(new Vector3d(0, 1, 0));
        Vector3d normal = panel.orientation().transform(new Vector3d(0, 0, 1));
        double lift = panel.worldHeight() / 2 + extra;
        return new Vector3d(panel.position()).add(up.mul(lift)).add(normal.mul(0.15));
    }
}
