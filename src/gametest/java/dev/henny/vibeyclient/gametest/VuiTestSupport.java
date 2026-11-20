package dev.henny.vibeyclient.gametest;

import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.geom.Rect;
import dev.henny.vui.layout.ScreenRects;
import dev.henny.vui.mc.VuiMc;
import dev.henny.vui.mc.host.VuiScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

final class VuiTestSupport {
    static final String LOG = "[vui-test] ";

    private final ClientGameTestContext context;
    private final Path review;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    VuiTestSupport(ClientGameTestContext context) {
        this.context = context;
        this.review = Path.of(System.getProperty("vui.review.dir", "build/review/vui"));
    }

    Path review() {
        return review;
    }

    void guiScale(int scale) {
        context.runOnClient(mc -> {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
        });
    }

    float[] center(String id) {
        return centerOf(id, -1);
    }

    float[] centerOf(String selectorOrId, int index) {
        return context.computeOnClient(mc -> {
            Document doc = ((VuiScreen) mc.screen).document();
            doc.layoutIfNeeded();
            Element e = index < 0 ? doc.find(selectorOrId) : doc.query(selectorOrId).get(index);
            if (e == null) throw new AssertionError("no element " + selectorOrId);
            Rect r = ScreenRects.of(e);
            return new float[]{r.centerX(), r.centerY()};
        });
    }

    void pointAt(float[] gui) {
        double gs = context.computeOnClient(mc -> (double) mc.getWindow().getGuiScale());
        context.getInput().setCursorPos(gui[0] * gs, gui[1] * gs);
        context.waitTicks(2);
    }

    void click(String id) {
        click(center(id));
    }

    void click(float[] gui) {
        pointAt(gui);
        context.getInput().pressMouse(0);
        context.waitTicks(3);
    }

    void assertNoErrors() {
        String errors = context.computeOnClient(mc -> {
            StringBuilder sb = new StringBuilder();
            for (Document d : VuiMc.vui().reloader().documents()) if (!d.diagnostics().isEmpty()) sb.append(d.diagnostics().format());
            return sb.toString();
        });
        if (!errors.isEmpty()) throw new AssertionError("vui errors:\n" + errors);
    }

    void shot(String name) {
        Path file = context.takeScreenshot(name);
        copy(file, name + ".png");
    }

    void copy(Path file, String name) {
        try {
            Files.createDirectories(review);
            Files.copy(file, review.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AssertionError("could not copy " + file + " to " + review, e);
        }
    }

    byte[] bridge(String path) {
        int port = context.computeOnClient(mc -> VuiMc.dev().bridgePort());
        if (port <= 0) throw new AssertionError("dev bridge not running");
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(10)).build();
        CompletableFuture<HttpResponse<byte[]>> pending = http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
        for (int i = 0; i < 400 && !pending.isDone(); i++) context.waitTick();
        HttpResponse<byte[]> response = pending.getNow(null);
        if (response == null) throw new AssertionError("bridge " + path + " did not answer");
        if (response.statusCode() != 200) throw new AssertionError(path + " -> " + response.statusCode() + " " + new String(response.body()));
        return response.body();
    }

    boolean waitUntil(java.util.function.Predicate<Minecraft> condition, int maxTicks) {
        for (int i = 0; i < maxTicks; i++) {
            if (context.computeOnClient(condition::test)) return true;
            context.waitTick();
        }
        return false;
    }

    static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
