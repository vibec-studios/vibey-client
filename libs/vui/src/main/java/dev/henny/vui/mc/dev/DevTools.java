package dev.henny.vui.mc.dev;

import dev.henny.vui.bind.Model;
import dev.henny.vui.demo.DemoController;
import dev.henny.vui.dev.DevBridge;
import dev.henny.vui.mc.VuiMod;
import dev.henny.vui.mc.VuiRuntime;
import dev.henny.vui.mc.compat.Mc;
import dev.henny.vui.mc.host.VuiScreen;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class DevTools {
    public static final String DEMO = "vui:demo";

    private final VuiRuntime runtime;
    private final boolean enabled;
    private final List<Path> roots;
    private final FrameCapture capture = new FrameCapture();
    private final Map<String, Supplier<Object>> controllers = new HashMap<>();
    private DevBridge bridge;
    private DemoHud demoHud;

    public DevTools(VuiRuntime runtime, boolean enabled, List<Path> roots) {
        this.runtime = runtime;
        this.enabled = enabled;
        this.roots = List.copyOf(roots);
        controller(DEMO, this::demoController);
    }

    public boolean enabled() {
        return enabled;
    }

    public List<Path> roots() {
        return roots;
    }

    public int bridgePort() {
        return bridge == null ? -1 : bridge.port();
    }

    public FrameCapture capture() {
        return capture;
    }

    public DevTools controller(String documentId, Supplier<Object> factory) {
        controllers.put(documentId, factory);
        return this;
    }

    public Object createController(String documentId) {
        Supplier<Object> factory = controllers.get(documentId);
        return factory == null ? new Model() : factory.get();
    }

    public VuiScreen open(String documentId) {
        Screen current = Mc.screen();
        VuiScreen screen = new VuiScreen(documentId, createController(documentId)).parent(current instanceof VuiScreen ? null : current);
        screen.document();
        return screen.open();
    }

    public void demoHud() {
        if (demoHud == null) demoHud = DemoHud.install(runtime.hud());
    }

    public void start() {
        if (!enabled) return;
        try {
            runtime.vui().reloader().watch();
        } catch (IOException e) {
            VuiMod.LOG.warn("vui: hot reload is off, the source folders could not be watched", e);
        }
        try {
            bridge = new DevBridge(runtime.vui(), new McDevTarget(this, runtime)).start(DevMode.port());
            VuiMod.LOG.info("vui dev mode: bridge on http://127.0.0.1:{}/ , override roots {}", bridge.port(), roots);
        } catch (IOException | RuntimeException e) {
            VuiMod.LOG.warn("vui: dev bridge did not start on port {}", DevMode.port(), e);
        }
    }

    public void tick(long frames) {
        if (!enabled) return;
        capture.service(frames);
        if (demoHud != null) demoHud.tick();
    }

    private Object demoController() {
        return new DemoController()
                .toasts(text -> runtime.toasts().push(text, null, null, "ok", 3f))
                .hudEditor(() -> {
                    demoHud();
                    runtime.hud().openEditor();
                });
    }
}
