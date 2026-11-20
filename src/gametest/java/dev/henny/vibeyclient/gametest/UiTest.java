package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.geom.Rect;
import dev.henny.vui.layout.ScreenRects;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.mc.host.VuiScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Predicate;

final class UiTest {
    private final ClientGameTestContext ctx;
    private final Path review;

    UiTest(ClientGameTestContext ctx, String folder) {
        this.ctx = ctx;
        this.review = Path.of(System.getProperty("vui.review.dir", "build/review/vui")).resolveSibling(folder);
    }

    ClientGameTestContext ctx() {
        return ctx;
    }

    void window(int w, int h) {
        ctx.getInput().resizeWindow(w, h);
        ctx.waitTicks(2);
    }

    void scale(int scale) {
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
        });
        ctx.waitTicks(2);
    }

    void lang(GlobalSettings.Language l) {
        ctx.runOnClient(mc -> {
            GlobalSettings.language.set(l);
            Lang.tick();
        });
        ctx.waitTicks(2);
    }

    void shot(String name) {
        Path file = ctx.takeScreenshot(name);
        try {
            Files.createDirectories(review);
            Files.copy(file, review.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AssertionError("could not copy " + file + " to " + review, e);
        }
    }

    float[] center(String idOrSelector) {
        return ctx.computeOnClient(mc -> {
            Element e = find(document(mc), idOrSelector);
            if (e == null) throw new AssertionError("no element " + idOrSelector + " in " + mc.screen);
            Rect r = ScreenRects.of(e);
            return new float[] {r.centerX(), r.centerY()};
        });
    }

    float[] box(String idOrSelector) {
        return ctx.computeOnClient(mc -> {
            Element e = find(document(mc), idOrSelector);
            if (e == null) return null;
            Rect r = ScreenRects.of(e);
            return new float[] {r.x(), r.y(), r.width(), r.height()};
        });
    }

    boolean has(String idOrSelector) {
        return ctx.computeOnClient(mc -> mc.screen instanceof VuiScreen && find(document(mc), idOrSelector) != null);
    }

    void hover(float x, float y) {
        double gs = ctx.computeOnClient(mc -> (double) mc.getWindow().getGuiScale());
        ctx.getInput().setCursorPos(x * gs + gs / 2, y * gs + gs / 2);
        ctx.waitTicks(2);
    }

    void click(float x, float y) {
        hover(x, y);
        ctx.getInput().pressMouse(0);
        ctx.waitTicks(3);
    }

    void click(String idOrSelector) {
        float[] c = center(idOrSelector);
        click(c[0], c[1]);
    }

    void away() {
        ctx.getInput().setCursorPos(0, 0);
        ctx.waitTicks(1);
    }

    void waitFor(Predicate<Minecraft> ok, int ticks, String what) {
        for (int i = 0; i < ticks; i++) {
            if (ctx.computeOnClient(ok::test)) return;
            ctx.waitTick();
        }
        throw new AssertionError("timed out: " + what);
    }

    void assertNoErrors() {
        String errors = ctx.computeOnClient(mc -> {
            StringBuilder sb = new StringBuilder();
            for (Document d : VuiMc.vui().reloader().documents()) {
                if (d.diagnostics().hasErrors()) sb.append(d.diagnostics().format());
            }
            return sb.toString();
        });
        if (!errors.isEmpty()) throw new AssertionError("vui errors:\n" + errors);
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static Document document(Minecraft mc) {
        if (!(mc.screen instanceof VuiScreen s)) throw new AssertionError("not a vui screen: " + mc.screen);
        Document doc = s.document();
        doc.layoutIfNeeded();
        return doc;
    }

    private static Element find(Document doc, String idOrSelector) {
        Element e = doc.find(idOrSelector);
        if (e != null) return e;
        var list = doc.query(idOrSelector);
        return list.isEmpty() ? null : list.get(0);
    }
}
