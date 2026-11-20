package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.editor.HudEditorScreen;
import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.geom.Rect;
import dev.henny.vui.layout.ScreenRects;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class HudTestKit {
    static final String LOG = "[vibeyclient-hud-test] ";

    final ClientGameTestContext context;
    final Path review;

    HudTestKit(ClientGameTestContext context) {
        this.context = context;
        Path vui = Path.of(System.getProperty("vui.review.dir", "build/review/vui"));
        this.review = vui.getParent().resolve("hud-vui");
    }

    Path shot(String name) {
        Path file = context.takeScreenshot(name);
        copy(file, review, name + ".png");
        return file;
    }

    static void copy(Path file, Path dir, String name) {
        try {
            Files.createDirectories(dir);
            Files.copy(file, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AssertionError("could not copy " + file, e);
        }
    }

    double gs() {
        return context.computeOnClient(mc -> mc.getWindow().getGuiScale());
    }

    int sw() {
        return context.computeOnClient(mc -> mc.getWindow().getGuiScaledWidth());
    }

    int sh() {
        return context.computeOnClient(mc -> mc.getWindow().getGuiScaledHeight());
    }

    void point(double x, double y) {
        double g = gs();
        context.getInput().setCursorPos(x * g, y * g);
        context.waitTicks(1);
    }

    void drag(double x0, double y0, double x1, double y1, int steps, Runnable midShot) {
        point(x0, y0);
        context.waitTicks(1);
        context.getInput().holdMouse(0);
        context.waitTicks(1);
        for (int i = 1; i <= steps; i++) point(x0 + (x1 - x0) * i / steps, y0 + (y1 - y0) * i / steps);
        if (midShot != null) midShot.run();
        context.getInput().releaseMouse(0);
        context.waitTicks(2);
    }

    void click(double x, double y) {
        point(x, y);
        context.waitTicks(1);
        context.getInput().pressMouse(0);
        context.waitTicks(3);
    }

    void click(String id) {
        float[] c = center(id);
        click(c[0], c[1]);
    }

    float[] center(String id) {
        return context.computeOnClient(mc -> {
            Document doc = ((HudEditorScreen) mc.screen).document();
            doc.layoutIfNeeded();
            Element e = doc.find(id);
            if (e == null) throw new AssertionError("no element #" + id);
            Rect r = ScreenRects.of(e);
            return new float[] {r.centerX(), r.centerY()};
        });
    }

    boolean exists(String id) {
        return context.computeOnClient(mc -> mc.screen instanceof HudEditorScreen s && s.document() != null && s.document().find(id) != null);
    }

    void key(int glfw) {
        context.getInput().pressKey(Keys.toNative(glfw));
        context.waitTicks(2);
    }

    void ctrl(char letter, boolean shift) {
        int code = context.computeOnClient(mc -> {
            for (int c = 'A'; c <= 'Z'; c++) if (ClientCompat.layoutLetter(c) == letter) return c;
            return (int) letter;
        });
        context.getInput().holdKey(Keys.toNative(Keys.LEFT_CONTROL));
        if (shift) context.getInput().holdKey(Keys.toNative(Keys.LEFT_SHIFT));
        context.getInput().pressKey(Keys.toNative(code));
        if (shift) context.getInput().releaseKey(Keys.toNative(Keys.LEFT_SHIFT));
        context.getInput().releaseKey(Keys.toNative(Keys.LEFT_CONTROL));
        context.waitTicks(2);
    }

    HudElement el(String id) {
        HudElement e = context.computeOnClient(mc -> HudLayout.get(id));
        if (e == null) throw new AssertionError("no element " + id);
        return e;
    }

    HudLayout.Box box(String id) {
        HudElement e = el(id);
        return context.computeOnClient(mc -> HudLayout.box(e, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), new HudLayout.Box()));
    }

    String placement(String id) {
        HudElement e = el(id);
        return context.computeOnClient(mc -> {
            HudLayout.Placement p = HudLayout.placement(e);
            return p.anchor + " " + p.dx + "," + p.dy + " x" + p.scale;
        });
    }

    HudEditorScreen editor() {
        return context.computeOnClient(mc -> mc.screen instanceof HudEditorScreen s ? s : null);
    }

    void openEditor() {
        context.setScreen(() -> new HudEditorScreen(null));
        context.waitTicks(8);
        if (editor() == null) throw new AssertionError("HUD editor did not open");
    }

    void assertNoVuiErrors() {
        String errors = context.computeOnClient(mc -> {
            StringBuilder sb = new StringBuilder();
            for (Document d : dev.henny.vui.mc.VuiMc.vui().reloader().documents()) if (!d.diagnostics().isEmpty()) sb.append(d.diagnostics().format());
            return sb.toString();
        });
        if (!errors.isEmpty()) throw new AssertionError("vui errors:\n" + errors);
    }

    void guiScale(int scale) {
        context.runOnClient(mc -> {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
        });
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }
}
