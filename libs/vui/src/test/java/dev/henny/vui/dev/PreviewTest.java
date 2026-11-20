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
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewTest {
    @Test
    void rendersLooseFileWithStateAndImports(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("badge.vui"), "<props>{ \"label\": \"?\" }</props><template><row class=\"badge\"><text>{{ label }}</text></row></template><style>.badge { background: $brand; padding: 2; }</style>");
        Files.writeString(dir.resolve("page.vui"), """
                <import name="badge" src="preview:badge"/>
                <template><column class="p"><text id="title">{{ title }}</text><badge :label="items.length + ' items'"/>
                <text v-for="it in items">{{ it.name }}</text></column></template>
                <style>.p { padding: 4; background: $panel; }</style>
                """);
        Files.writeString(dir.resolve("state.json"), "{ \"title\": \"Hello\", \"items\": [ { \"name\": \"a\" }, { \"name\": \"b\" } ] }");
        Path out = dir.resolve("out.png");
        PrintStream old = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true));
        int errors;
        try {
            errors = Preview.run(PreviewOptions.parse(new String[]{dir.resolve("page.vui").toString(), "160", "90", out.toString(),
                    dir.resolve("state.json").toString(), "--scale", "2"}));
        } finally {
            System.setOut(old);
        }
        String text = captured.toString();
        assertEquals(0, errors, text);
        assertTrue(text.contains("\"Hello\""), text);
        assertTrue(text.contains("\"2 items\""), text);
        BufferedImage img = ImageIO.read(out.toFile());
        assertEquals(320, img.getWidth());
        assertEquals(180, img.getHeight());
    }

    @Test
    void reportsErrorsWithFileAndLine(@TempDir Path dir) throws Exception {
        Path page = dir.resolve("assets/demo/vui/broken.vui");
        Files.createDirectories(page.getParent());
        Files.writeString(page, "<template>\n  <column>\n    <txt/>\n  </column>\n</template>");
        PrintStream old = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true));
        int errors;
        try {
            errors = Preview.run(PreviewOptions.parse(new String[]{page.toString(), "100", "50", dir.resolve("o.png").toString()}));
        } finally {
            System.setOut(old);
        }
        assertEquals(1, errors);
        String text = captured.toString();
        assertTrue(text.contains("broken.vui:3:5: error: unknown element <txt>"), text);
        assertTrue(text.contains("did you mean <text>"), text);
    }
}
