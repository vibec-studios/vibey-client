package dev.henny.vui.dev;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewScriptTest {
    private static String run(String... args) throws Exception {
        PrintStream old = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true));
        try {
            int errors = Preview.run(PreviewOptions.parse(args));
            return errors + "\n" + captured;
        } finally {
            System.setOut(old);
        }
    }

    @Test
    void parsesStepsInOrder() {
        PreviewOptions o = PreviewOptions.parse(new String[]{"a.vui", "10", "10", "o.png", "--hover", "3,4", "--click", "go",
                "--scroll", "-2", "--lang", "ui.json", "--theme", "t.json", "--icons", "i.json", "--textures", "tex", "--quiet"});
        assertEquals(3, o.steps().size());
        assertInstanceOf(PreviewStep.Hover.class, o.steps().get(0));
        assertEquals(new PreviewStep.Click("go"), o.steps().get(1));
        assertEquals(new PreviewStep.Scroll(-2), o.steps().get(2));
        assertTrue(o.quiet());
        assertEquals(Path.of("t.json"), o.theme());
    }

    @Test
    void translatesThemesDrawsIconsAndRunsTheScript(@TempDir Path dir) throws Exception {
        Path vui = dir.resolve("assets/demo/vui");
        Files.createDirectories(vui);
        Files.writeString(vui.resolve("icons.json"), "{ \"icons\": { \"dot\": [\"###\", \"###\", \"###\"] } }");
        Files.writeString(vui.resolve("page.vui"), """
                <template><column class="p">
                  <text id="title">{{ t('ui.menu.title', count) }}</text>
                  <button id="go" @click="clicked = 'yes'" tooltip="Tip">{{ clicked }}</button>
                  <icon id="i" name="demo:dot" style="size: 9; color: $brand"/>
                </column></template>
                <style>.p { background: $panel; padding: 4; gap: 2; }</style>
                """);
        Path lang = dir.resolve("ui.json");
        Files.writeString(lang, "{ \"menu\": { \"title\": \"Modules: {0}\" } }");
        Path theme = dir.resolve("theme.json");
        Files.writeString(theme, "{ \"colors\": { \"panel\": \"#102030\", \"brand\": \"#00ff00\" } }");
        Path state = dir.resolve("state.json");
        Files.writeString(state, "{ \"count\": 3, \"clicked\": \"no\" }");
        Path out = dir.resolve("out.png");
        String text = run(vui.resolve("page.vui").toString(), "60", "40", out.toString(), state.toString(), "--scale", "1",
                "--lang", lang.toString(), "--theme", theme.toString(), "--click", "go", "--hover", "#go");
        assertTrue(text.startsWith("0\n"), text);
        assertTrue(text.contains("\"Modules: 3\""), text);
        assertTrue(text.contains("\"yes\""), text);
        assertTrue(text.contains("overlay:open"), text);
        BufferedImage img = ImageIO.read(out.toFile());
        assertEquals(0xFF102030, img.getRGB(1, 1));
        assertTrue(hasColor(img, 0xFF00FF00), "icon drawn with themed tint");
    }

    @Test
    void unknownClickTargetIsAWarning(@TempDir Path dir) throws Exception {
        Path page = dir.resolve("p.vui");
        Files.writeString(page, "<template><column/></template>");
        String text = run(page.toString(), "20", "20", dir.resolve("o.png").toString(), "--click", "nothing", "--quiet");
        assertTrue(text.startsWith("0\n"), text);
        assertTrue(text.contains("--click nothing: nothing matches"), text);
        assertFalse(text.contains("column ["), text);
    }

    private static boolean hasColor(BufferedImage img, int argb) {
        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) if (img.getRGB(x, y) == argb) return true;
        return false;
    }
}
