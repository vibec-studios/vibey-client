package dev.henny.r3.support;

import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.anchor.FaceSnap;
import dev.henny.r3.hint.SetHintStore;
import dev.henny.r3.input.InputCode;
import dev.henny.r3.interact.Interaction;
import dev.henny.r3.interact.InteractionEvent;
import dev.henny.r3.interact.InteractionEventKind;
import dev.henny.r3.interact.InteractionRules;
import dev.henny.r3.math.Face;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.scene.R3Scene;
import dev.henny.r3.view.CameraState;
import dev.henny.r3.view.ViewFrustum;

import java.util.ArrayList;
import java.util.List;

public final class Rig {
    public static final float DT = 1f / 60f;
    public final FakeWorld world = new FakeWorld().wallZ(5, -8, 8, 58, 72);
    public final TestKeys keys = new TestKeys();
    public final SetHintStore hints = new SetHintStore();
    public final R3Scene scene = R3Scene.create(world, keys, hints);
    public final Interaction interaction = scene.interaction();
    public final CameraState camera = new CameraState().set(0.5, 65, 0.5, 0, 0);
    public final List<InteractionEvent> events = new ArrayList<>();
    public final TestPanel panel = new TestPanel();

    public Rig() {
        interaction.listen(events::add);
    }

    public WorldObject wallScreen() {
        WorldObject o = WorldObject.create("test:screen").content(panel).rules(InteractionRules.screen());
        o.dimension(world.dimension);
        o.transform().size(2f, 1.125f).position(0.5, 65, 5 - FaceSnap.DEFAULT_OFFSET);
        FaceSnap.orientation(Face.NORTH, Face.UP, o.transform().orientation());
        o.anchor(new BlockFaceAnchor(Face.NORTH, Face.UP, 0, 65, 5));
        return scene.add(o);
    }

    public Rig look(float yaw, float pitch) {
        camera.rotation(yaw, pitch);
        return this;
    }

    public Rig lookAt(double x, double y, double z) {
        camera.lookAlong(x - camera.position().x(), y - camera.position().y(), z - camera.position().z());
        return this;
    }

    public Rig frame() {
        scene.update(camera, ViewFrustum.ALL, DT);
        return this;
    }

    public Rig frames(int n) {
        for (int i = 0; i < n; i++) frame();
        return this;
    }

    public boolean press(InputCode code) {
        keys.hold(code);
        return scene.press(code);
    }

    public boolean release(InputCode code) {
        keys.release(code);
        return scene.release(code);
    }

    public boolean click(InputCode code) {
        boolean a = press(code);
        boolean b = release(code);
        return a || b;
    }

    public long count(InteractionEventKind kind) {
        return events.stream().filter(e -> e.kind() == kind).count();
    }

    public InteractionEvent last() {
        return events.isEmpty() ? null : events.get(events.size() - 1);
    }
}
