package dev.henny.r3.scene;

import dev.henny.r3.anchor.EntityAnchor;
import dev.henny.r3.fx.Emitter;
import dev.henny.r3.fx.EmitterShape;
import dev.henny.r3.fx.FloatCurve;
import dev.henny.r3.fx.ParticleStyle;
import dev.henny.r3.fx.ParticleSystem;
import dev.henny.r3.input.InputCode;
import dev.henny.r3.interact.InteractionEventKind;
import dev.henny.r3.object.ObjectContent;
import dev.henny.r3.object.VideoSurface;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.render.Light;
import dev.henny.r3.support.RecordingCanvas;
import dev.henny.r3.support.Rig;
import dev.henny.r3.support.TestPanel;
import dev.henny.r3.text.Text3D;
import dev.henny.r3.view.ViewFrustum;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneTest {
    private final Rig rig = new Rig();
    private final RecordingCanvas canvas = new RecordingCanvas();

    @Test
    void distanceFadeAndCutoff() {
        WorldObject screen = rig.wallScreen();
        screen.visibility().maxDistance(6).fadeDistance(4);
        rig.frame();
        assertEquals((6 - 4.48) / 4, screen.state().alpha(), 0.01);
        rig.camera.position(0.5, 65, -5);
        rig.frame();
        assertFalse(screen.state().visible());
        assertNull(rig.interaction.hover().target());
    }

    @Test
    void frustumCullingHidesAndDisablesPicking() {
        WorldObject screen = rig.wallScreen();
        rig.scene.update(rig.camera, (a, b, c, d, e, f) -> false, Rig.DT);
        assertFalse(screen.state().visible());
        assertNull(rig.interaction.hover().target());
        rig.scene.render(canvas);
        assertEquals(0, canvas.panels);
    }

    @Test
    void panelsAreLaidOutOnceAndDrawnInPanelPixels() {
        rig.wallScreen();
        rig.frame();
        rig.scene.render(canvas);
        rig.scene.render(canvas);
        assertEquals(1, rig.panel.layouts);
        assertEquals(128, rig.panel.width);
        assertEquals(72, rig.panel.height);
        assertEquals(2, canvas.panels);
        assertEquals(0, canvas.depth);
    }

    @Test
    void borderHoverDrawsAHandle() {
        rig.wallScreen();
        rig.lookAt(-0.5, 65, 4.98).frame();
        rig.scene.render(canvas);
        assertEquals(4, canvas.vertices);
    }

    @Test
    void heldObjectGetsAnOutlineAndKeepsFocus() {
        WorldObject screen = rig.wallScreen();
        rig.lookAt(0.5, 65, 4.98).frame();
        rig.press(InputCode.MOUSE_MIDDLE);
        WorldObject blocker = rig.scene.add(WorldObject.create("test:blocker").content(new TestPanel()));
        blocker.transform().size(3, 3).position(0.5, 65, 2).orientation(screen.transform().orientation());
        rig.frames(3);
        rig.release(InputCode.MOUSE_MIDDLE);
        assertSame(screen, rig.last().target());
        rig.scene.render(canvas);
        assertTrue(canvas.panels >= 1);
    }

    @Test
    void entityLabelsFollowAndHideWhenTheEntityIsGone() {
        UUID id = UUID.randomUUID();
        rig.world.entity(id, 0.5, 64, 3, 1.8f, 0, false);
        WorldObject label = rig.scene.add(WorldObject.create("test:name").content(new Text3D("Alex"))
                .anchoredTo(EntityAnchor.aboveHead(id, 0.3)));
        rig.frame();
        assertEquals(66.1, label.transform().position().y, 1e-6);
        assertTrue(label.state().visible());
        rig.world.removeEntity(id);
        rig.frame();
        assertFalse(label.state().visible());
    }

    @Test
    void finishedEffectsRemoveThemselves() {
        ParticleSystem burst = new ParticleSystem(32, ParticleStyle.glowDots(-1).withSize(FloatCurve.constant(1f)), 1)
                .emitter(new Emitter(EmitterShape.POINT).burst(10).lifetime(0.1f, 0.1f)).oneShot(true);
        WorldObject fx = rig.scene.add(WorldObject.create("test:fx").content(burst));
        fx.transform().position(0.5, 65, 3);
        rig.frame();
        rig.scene.render(canvas);
        assertEquals(40, canvas.vertices);
        rig.frames(20);
        assertNull(rig.scene.get(fx.id()));
    }

    @Test
    void opaqueFirstThenTranslucentBackToFront() {
        WorldObject near = rig.scene.add(WorldObject.create("near").content(new Text3D("a")));
        near.transform().position(0.5, 65, 2);
        WorldObject far = rig.scene.add(WorldObject.create("far").content(new Text3D("b")));
        far.transform().position(0.5, 65, 4);
        WorldObject video = rig.scene.add(WorldObject.create("video").content(new VideoSurface()));
        video.transform().position(0.5, 65, 4.5);
        rig.frame();
        rig.scene.render(canvas);
        assertSame(video, rig.scene.visibleObjects().get(0));
        assertSame(far, rig.scene.visibleObjects().get(1));
        assertSame(near, rig.scene.visibleObjects().get(2));
    }

    @Test
    void litObjectsAskTheWorldForLight() {
        WorldObject screen = rig.wallScreen().fullBright(false);
        rig.frame();
        assertEquals(rig.world.light, screen.state().light());
        screen.fullBright(true);
        rig.frame();
        assertEquals(Light.FULL_BRIGHT, screen.state().light());
    }

    @Test
    void tickReachesContent() {
        int[] ticks = {0};
        rig.scene.add(WorldObject.create("t").content(new ObjectContent() {
            @Override
            public void tick() {
                ticks[0]++;
            }
        }));
        rig.scene.tick();
        rig.scene.tick();
        assertEquals(2, ticks[0]);
    }

    @Test
    void replacingAnIdSwapsTheObject() {
        WorldObject a = rig.scene.add(new WorldObject("same", "k"));
        WorldObject b = rig.scene.add(new WorldObject("same", "k"));
        assertEquals(1, rig.scene.objects().size());
        assertSame(b, rig.scene.get("same"));
        assertFalse(rig.scene.objects().contains(a));
        rig.scene.clear();
        assertTrue(rig.scene.objects().isEmpty());
    }

    @Test
    void mouseRayInsteadOfCrosshair() {
        WorldObject screen = rig.wallScreen();
        rig.look(180, 0);
        dev.henny.r3.math.Ray ray = new dev.henny.r3.math.Ray().set(0.5, 65, 0.5, 0, 0, 1);
        rig.scene.update(rig.camera, ViewFrustum.ALL, ray, 64, Rig.DT);
        assertSame(screen, rig.interaction.hover().target());
        assertEquals(0, rig.count(InteractionEventKind.ACTION));
    }
}
