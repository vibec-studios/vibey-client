package dev.henny.vui.mc.host;

import dev.henny.vui.Vui;
import dev.henny.vui.hud.HudEditor;
import dev.henny.vui.hud.HudLayer;
import dev.henny.vui.hud.HudPlacement;
import dev.henny.vui.hud.HudWidget;
import dev.henny.vui.mc.VuiMod;
import dev.henny.vui.mc.VuiRuntime;
import dev.henny.vui.mc.compat.Mc;
import dev.henny.vui.mc.draw.McCanvas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HudHost {
    private final Vui vui;
    private final HudLayer layer = new HudLayer();
    private final FrameClock clock = new FrameClock();
    private Path file;
    private String saved;
    private boolean loaded;

    public HudHost(Vui vui) {
        this.vui = vui;
    }

    public HudWidget add(String widgetId, String documentId, Object controller, HudPlacement defaults) {
        remove(widgetId);
        HudWidget w = layer.add(widgetId, vui.open(documentId, controller), defaults);
        load();
        if (saved != null) layer.fromJson(saved);
        return w;
    }

    public void remove(String widgetId) {
        HudWidget w = layer.widget(widgetId);
        if (w == null) return;
        vui.close(w.document());
        layer.remove(widgetId);
    }

    public HudLayer layer() {
        return layer;
    }

    public HudEditor editor() {
        return layer.editor();
    }

    public HudEditScreen openEditor() {
        HudEditScreen screen = new HudEditScreen(this, Mc.screen());
        Mc.setScreen(screen);
        return screen;
    }

    public HudHost file(Path layoutFile) {
        file = layoutFile;
        loaded = false;
        return this;
    }

    public Path file() {
        if (file == null) file = Mc.gameDirectory().resolve("config").resolve("vui").resolve("hud.json");
        return file;
    }

    public void save() {
        saved = layer.toJson();
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), saved, StandardCharsets.UTF_8);
        } catch (IOException e) {
            VuiMod.LOG.warn("vui: could not save the HUD layout to {}", file(), e);
        }
    }

    private void load() {
        if (loaded || Mc.get() == null) return;
        loaded = true;
        try {
            if (Files.isRegularFile(file())) saved = Files.readString(file(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            VuiMod.LOG.warn("vui: could not read the HUD layout {}", file(), e);
        }
    }

    public void renderInGame(Object graphics) {
        if (Mc.hudHidden() || Mc.screen() instanceof HudEditScreen || layer.widgets().isEmpty()) return;
        draw(graphics);
    }

    public void draw(Object graphics) {
        float w = Mc.guiWidth();
        float h = Mc.guiHeight();
        float dt = clock.tick();
        Guard.run(null, () -> {
            if (w != layer.width() || h != layer.height()) layer.resize(w, h);
            layer.frame(dt);
        });
        McCanvas canvas = VuiRuntime.get().canvas().begin(graphics);
        try {
            Guard.run(null, () -> layer.render(canvas));
        } finally {
            canvas.end();
        }
    }
}
