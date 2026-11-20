package dev.henny.vui.mc;

import dev.henny.vui.Vui;
import dev.henny.vui.dom.Document;
import dev.henny.vui.mc.compat.Mc;
import dev.henny.vui.mc.dev.DevMode;
import dev.henny.vui.mc.dev.DevRoots;
import dev.henny.vui.mc.dev.DevTools;
import dev.henny.vui.mc.dev.Inspector;
import dev.henny.vui.mc.draw.McCanvas;
import dev.henny.vui.mc.draw.McTextMetrics;
import dev.henny.vui.mc.host.GlobalLayers;
import dev.henny.vui.mc.host.Guard;
import dev.henny.vui.mc.host.HudHost;
import dev.henny.vui.mc.host.OverlayHost;
import dev.henny.vui.mc.host.ToastHost;
import dev.henny.vui.mc.host.VuiScreen;
import dev.henny.vui.mc.input.McInput;
import dev.henny.vui.mc.resource.McTranslator;
import dev.henny.vui.mc.resource.ReloadWatch;
import dev.henny.vui.mc.resource.ResourceManagerRoot;
import dev.henny.vui.mc.texture.McTextures;
import dev.henny.vui.reload.ClasspathRoot;
import dev.henny.vui.reload.DirectoryRoot;
import dev.henny.vui.render.Backend;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class VuiRuntime {
    private static VuiRuntime instance;

    private final McTextMetrics metrics = new McTextMetrics();
    private final McInput input = new McInput();
    private final McTextures textures = new McTextures();
    private final ReloadWatch reloadWatch = new ReloadWatch();
    private final Vui vui;
    private final McCanvas canvas;
    private final HudHost hud;
    private final ToastHost toasts;
    private final OverlayHost overlays;
    private final Inspector inspector;
    private final GlobalLayers layers;
    private final DevTools dev;

    public static synchronized VuiRuntime get() {
        if (instance == null) {
            instance = new VuiRuntime();
            instance.dev.start();
        }
        return instance;
    }

    private VuiRuntime() {
        boolean devMode = DevMode.enabled();
        vui = new Vui(new Backend(metrics, input, textures)).devMode(devMode);
        canvas = new McCanvas(textures).placeholders(devMode);
        List<Path> roots = devMode ? DevRoots.resolve() : List.of();
        for (Path root : roots) vui.registry().addOverride(new DirectoryRoot(root));
        vui.registry().addRoot(new ResourceManagerRoot()).addRoot(new ClasspathRoot(VuiRuntime.class.getClassLoader()));
        vui.functions().translator(new McTranslator());
        hud = new HudHost(vui);
        toasts = new ToastHost(vui);
        overlays = new OverlayHost(vui);
        inspector = new Inspector(canvas, metrics, this::inspectable, devMode);
        layers = new GlobalLayers(hud, toasts, overlays, inspector, canvas);
        dev = new DevTools(this, devMode, roots);
    }

    public Vui vui() {
        return vui;
    }

    public McCanvas canvas() {
        return canvas;
    }

    public McInput input() {
        return input;
    }

    public McTextures textures() {
        return textures;
    }

    public HudHost hud() {
        return hud;
    }

    public ToastHost toasts() {
        return toasts;
    }

    public OverlayHost overlays() {
        return overlays;
    }

    public Inspector inspector() {
        return inspector;
    }

    public GlobalLayers layers() {
        return layers;
    }

    public DevTools dev() {
        return dev;
    }

    public void clientTick() {
        Guard.run(null, () -> {
            dev.tick(layers.frames());
            vui.tick();
            reloadWatch.tick(this::resourcesReloaded);
        });
    }

    private void resourcesReloaded() {
        textures.clear();
        metrics.clear();
        vui.reloader().reloadAll();
    }

    private List<Document> inspectable() {
        List<Document> out = new ArrayList<>(overlays.documents());
        java.util.Collections.reverse(out);
        if (Mc.screen() instanceof VuiScreen screen) out.add(screen.document());
        out.add(toasts.stack().document());
        return out;
    }
}
