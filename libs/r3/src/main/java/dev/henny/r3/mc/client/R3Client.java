package dev.henny.r3.mc.client;

import dev.henny.r3.mc.compat.Game;
import dev.henny.r3.mc.input.HeldInput;
import dev.henny.r3.mc.input.InputRouter;
import dev.henny.r3.mc.input.VanillaGuard;
import dev.henny.r3.mc.render.McWorldCanvas;
import dev.henny.r3.mc.store.R3Storage;
import dev.henny.r3.mc.view.CameraReader;
import dev.henny.r3.mc.vui.WorldVui;
import dev.henny.r3.mc.world.McWorldQuery;
import dev.henny.r3.scene.R3Scene;
import dev.henny.r3.view.CameraFrustum;
import dev.henny.r3.view.CameraState;
import org.joml.Matrix4fc;

public final class R3Client {
    private static R3Client instance;

    private final HeldInput held = new HeldInput();
    private final R3Storage storage;
    private final R3Scene scene;
    private final InputRouter input;
    private final WorldSession session;
    private final McWorldCanvas canvas = new McWorldCanvas();
    private final CameraState camera = new CameraState();
    private final CameraFrustum frustum = new CameraFrustum();
    private final FrameClock clock = new FrameClock();
    private WorldVui vui;
    private long frames;

    private R3Client() {
        storage = new R3Storage(R3Storage.defaultDir());
        scene = R3Scene.create(new McWorldQuery(), held, storage.hints());
        storage.loadBindings(scene.interaction().bindings());
        input = new InputRouter(held, scene);
        session = new WorldSession(storage, scene);
    }

    public static synchronized R3Client get() {
        if (instance == null) instance = new R3Client();
        return instance;
    }

    public static boolean started() {
        return instance != null;
    }

    public R3Scene scene() {
        return scene;
    }

    public InputRouter input() {
        return input;
    }

    public R3Storage storage() {
        return storage;
    }

    public CameraState camera() {
        return camera;
    }

    public long frames() {
        return frames;
    }

    public synchronized WorldVui vui() {
        if (vui == null) vui = new WorldVui(held);
        return vui;
    }

    public void listen(WorldListener listener) {
        session.listen(listener);
    }

    public String worldKey() {
        return session.key();
    }

    public McWorldCanvas renderWorld(Matrix4fc base) {
        session.check();
        if (Game.level() == null || !CameraReader.read(camera)) return null;
        float dt = clock.next();
        CameraReader.frustum(camera, frustum);
        scene.update(camera, frustum, dt);
        canvas.begin(camera.position(), base);
        scene.render(canvas);
        frames++;
        return canvas;
    }

    public void startTick() {
        VanillaGuard.startTick(scene);
    }

    public void endTick() {
        session.check();
        if (Game.level() != null) scene.tick();
    }

    public String hint() {
        return Game.playing() ? scene.interaction().hintText() : null;
    }
}
