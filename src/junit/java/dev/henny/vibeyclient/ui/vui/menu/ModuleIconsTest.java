package dev.henny.vibeyclient.ui.vui.menu;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.icon.IconSet;
import dev.henny.vui.icon.IconSetParser;
import dev.henny.vui.icon.PixelIcon;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleIconsTest {
    private static final String FILE = "/assets/vibeyclient/vui/icons.json";

    @Test
    void everyModuleHasAColoured16x16Icon() throws IOException {
        Diagnostics d = new Diagnostics();
        IconSet set;
        try (InputStream in = ModuleIconsTest.class.getResourceAsStream(FILE)) {
            assertNotNull(in, FILE);
            set = IconSetParser.parse("vibeyclient", "icons.json", new String(in.readAllBytes(), StandardCharsets.UTF_8), d);
        }
        assertTrue(d.isEmpty(), d.format());
        assertNotNull(set.get("puzzle"));
        for (String id : ModuleLook.COLORED) {
            PixelIcon icon = set.get("module_" + id);
            assertNotNull(icon, id);
            assertFalse(icon.tinted(), id);
            assertEquals(16, icon.width(), id);
            assertEquals(16, icon.height(), id);
        }
    }
}
