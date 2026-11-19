package dev.henny.r3.scene;

import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.anchor.BlockHit;
import dev.henny.r3.anchor.FaceSnap;
import dev.henny.r3.hint.SetHintStore;
import dev.henny.r3.input.InputCode;
import dev.henny.r3.interact.InteractionEventKind;
import dev.henny.r3.interact.InteractionRules;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.panel.PanelContent;
import dev.henny.r3.render.PanelCanvas;
import dev.henny.r3.support.FakeWorld;
import dev.henny.r3.support.TestKeys;
import dev.henny.r3.view.CameraState;
import dev.henny.r3.view.ViewFrustum;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadmeExampleTest {
    static final class Counter implements PanelContent {
        int clicks;
        int width;

        @Override
        public void layout(int width, int height) {
            this.width = width;
        }

        @Override
        public void draw(PanelCanvas c) {
            c.fill(0, 0, width, 40, 0xE0101014);
            c.text("Clicks: " + clicks, 8, 8, 0xFFFFFFFF);
        }

        @Override
        public String controlAt(float x, float y) {
            return y < 40 ? "count" : null;
        }
    }

    @Test
    void panelOnAWallThatThePlayerCanMoveResizeAndUse() {
        FakeWorld world = new FakeWorld().wallZ(5, -8, 8, 58, 72);
        TestKeys keys = new TestKeys();
        CameraState camera = new CameraState().set(0.5, 65, 0.5, 0, 0);

        R3Scene scene = R3Scene.create(world, keys, new SetHintStore());
        Counter counter = new Counter();
        WorldObject panel = WorldObject.create("example:counter").content(counter).rules(InteractionRules.screen());
        panel.transform().size(2f, 1f);
        BlockHit hit = new BlockHit();
        world.raycastBlocks(camera.position(), camera.look(), 8, hit);
        panel.dimension(world.dimension()).anchor(BlockFaceAnchor.of(hit, FaceSnap.upFor(hit.face(), camera.yaw(), 0)));
        FaceSnap.pose(hit.point(), hit.face(), FaceSnap.upFor(hit.face(), camera.yaw(), 0),
                FaceSnap.DEFAULT_OFFSET, FaceSnap.DEFAULT_GRID, panel.transform());
        scene.add(panel);
        List<String> saved = new ArrayList<>();
        scene.interaction().listen(e -> {
            if (e.kind() == InteractionEventKind.ACTION && "count".equals(e.controlId())) counter.clicks++;
            if (e.changedPose()) saved.add(e.kind().name());
        });

        camera.lookAlong(0, 0.4, 4.5);
        scene.update(camera, ViewFrustum.ALL, 0.016f);
        scene.press(InputCode.MOUSE_LEFT);
        scene.release(InputCode.MOUSE_LEFT);
        assertEquals(1, counter.clicks);

        camera.rotation(0, 0);
        scene.update(camera, ViewFrustum.ALL, 0.016f);
        scene.wheel(2);
        assertEquals(2.5f, panel.transform().worldWidth(), 1e-5);

        scene.press(InputCode.MOUSE_MIDDLE);
        camera.lookAlong(3, 1, 4.5);
        scene.update(camera, ViewFrustum.ALL, 0.016f);
        scene.release(InputCode.MOUSE_MIDDLE);
        assertTrue(panel.transform().position().x > 2);
        assertEquals(List.of("RESIZED", "MOVED"), saved);
    }
}
