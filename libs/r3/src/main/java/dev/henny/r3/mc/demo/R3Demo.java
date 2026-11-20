package dev.henny.r3.mc.demo;

import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.anchor.BlockHit;
import dev.henny.r3.anchor.FaceSnap;
import dev.henny.r3.interact.InteractionEvent;
import dev.henny.r3.interact.InteractionEventKind;
import dev.henny.r3.math.Face;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.mc.R3Mod;
import dev.henny.r3.mc.client.R3Client;
import dev.henny.r3.mc.client.WorldListener;
import dev.henny.r3.mc.view.CameraReader;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.scene.R3Scene;
import dev.henny.r3.view.CameraState;

import java.util.ArrayList;
import java.util.List;

public final class R3Demo {
    public static final String DOCUMENT = "r3:demo";
    public static final double REACH = 24;

    private static final List<String> actions = new ArrayList<>();
    private static DemoController controller;
    private static WorldObject panel;
    private static WorldObject title;
    private static WorldObject ring;
    private static boolean listening;

    private R3Demo() {}

    public static boolean start() {
        stop();
        R3Scene scene = R3Mc.scene();
        CameraState camera = new CameraState();
        if (!CameraReader.read(camera)) return false;
        BlockHit hit = new BlockHit();
        if (!scene.query().raycastBlocks(camera.position(), camera.look(), REACH, hit)) return false;
        controller = new DemoController();
        panel = R3Mc.panel(DOCUMENT, controller);
        Face up = FaceSnap.upFor(hit.face(), camera.yaw(), 0);
        FaceSnap.pose(hit.point(), hit.face(), up, FaceSnap.DEFAULT_OFFSET, FaceSnap.DEFAULT_GRID, panel.transform());
        panel.anchor(BlockFaceAnchor.of(hit, up));
        scene.add(panel);
        title = scene.add(DemoObjects.title(panel.transform()));
        ring = scene.add(DemoObjects.ring(panel.transform()));
        listen(scene);
        R3Mod.LOG.info("r3 demo placed on {} at {}", hit.face(), panel.transform().position());
        return true;
    }

    public static void stop() {
        if (panel != null) R3Mc.remove(panel);
        if (title != null) R3Mc.remove(title);
        if (ring != null) R3Mc.remove(ring);
        panel = title = ring = null;
        controller = null;
        actions.clear();
    }

    public static WorldObject panel() {
        return panel;
    }

    public static DemoController controller() {
        return controller;
    }

    public static List<String> actions() {
        return actions;
    }

    private static void listen(R3Scene scene) {
        if (listening) return;
        listening = true;
        scene.interaction().listen(R3Demo::event);
        R3Client.get().listen(new WorldListener() {
            @Override
            public void worldLeft(String worldKey) {
                panel = title = ring = null;
                controller = null;
            }
        });
    }

    private static void event(InteractionEvent e) {
        if (panel == null || e.target() != panel) return;
        actions.add(e.kind() == InteractionEventKind.ACTION ? e.controlId() : e.kind().name());
    }
}
