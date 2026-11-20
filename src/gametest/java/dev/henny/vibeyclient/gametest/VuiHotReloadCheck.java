package dev.henny.vibeyclient.gametest;

import dev.henny.vui.dom.Document;
import dev.henny.vui.headless.TreeDump;
import dev.henny.vui.mc.VuiMc;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

final class VuiHotReloadCheck {
    private static final String DEMO_FILE = "assets/vui/vui/demo.vui";
    private static final String MARK = "Hot reloaded";

    private VuiHotReloadCheck() {
    }

    static void run(ClientGameTestContext context, VuiTestSupport t) {
        try {
            check(context, t);
        } catch (IOException e) {
            throw new AssertionError("hot reload check failed", e);
        }
    }

    private static void check(ClientGameTestContext context, VuiTestSupport t) throws IOException {
        List<Path> roots = context.computeOnClient(mc -> VuiMc.dev().roots());
        Path override = roots.get(0);
        if (!override.getFileName().toString().equals("vui-test-override")) throw new AssertionError("first dev root is " + override);
        Path source = roots.stream().skip(1).map(r -> r.resolve(DEMO_FILE)).filter(Files::isRegularFile).findFirst()
                .orElseThrow(() -> new AssertionError("demo.vui not found in " + roots));
        Path target = override.resolve(DEMO_FILE);
        Files.createDirectories(target.getParent());
        context.waitTicks(10);

        String original = Files.readString(source, StandardCharsets.UTF_8);
        String changed = original.replace("{{ title }}", MARK + " · {{ title }}").replace(".logo { color: $brand; }", ".logo { color: $warn; }");
        int before = reloads(context);
        long start = System.nanoTime();
        Files.writeString(target, changed, StandardCharsets.UTF_8);
        boolean swapped = t.waitUntil(mc -> VuiMc.vui().reloader().reloads() > before && dump(VuiGameTest.document(mc)).contains(MARK), 200);
        long reloadMs = (System.nanoTime() - start) / 1_000_000;
        if (!swapped) throw new AssertionError("the changed demo.vui was not picked up");
        byte[] png = t.bridge("/screenshot");
        long totalMs = (System.nanoTime() - start) / 1_000_000;
        Files.createDirectories(t.review());
        Files.write(t.review().resolve("vui_08_hot_reload_bridge.png"), png);
        System.out.printf(Locale.ROOT, VuiTestSupport.LOG + "hot reload: file saved -> new tree %d ms, -> bridge screenshot %d ms (%d bytes)%n", reloadMs, totalMs, png.length);
        t.shot("vui_08_hot_reload");

        Files.writeString(target, changed.replace("<spacer/>", "<spacer/><buton>typo</buton>"), StandardCharsets.UTF_8);
        boolean failed = t.waitUntil(mc -> !VuiGameTest.document(mc).diagnostics().errors().isEmpty(), 200);
        if (!failed) throw new AssertionError("the broken file did not report an error");
        String errors = new String(t.bridge("/errors"), StandardCharsets.UTF_8);
        if (!errors.contains("vui-test-override") || !errors.contains("buton")) throw new AssertionError("errors lack file:line: " + errors);
        context.waitTicks(4);
        t.shot("vui_09_error_overlay");

        Files.delete(target);
        boolean restored = t.waitUntil(mc -> {
            Document doc = VuiGameTest.document(mc);
            return doc.diagnostics().errors().isEmpty() && !dump(doc).contains(MARK);
        }, 200);
        if (!restored) throw new AssertionError("deleting the override did not restore the original demo");
    }

    private static int reloads(ClientGameTestContext context) {
        return context.computeOnClient(mc -> VuiMc.vui().reloader().reloads());
    }

    private static String dump(Document doc) {
        return doc == null ? "" : TreeDump.dump(doc.root());
    }
}
