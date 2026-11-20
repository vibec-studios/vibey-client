package dev.henny.vui.headless;

import dev.henny.vui.TestUi;
import dev.henny.vui.dom.Element;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TintTest {
    @Test
    void iconTexturesAreTintedLikeTheMcBackend() {
        TestUi ui = new TestUi();
        BufferedImage white = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) white.setRGB(x, y, 0xFFFFFFFF);
        white.setRGB(0, 0, 0xFF808080);
        ui.headless.textures().put("demo:white", white);
        ui.doc = ui.vui.fromSource("t.vui", """
                <template><column><icon id="i" src="demo:white" style="size: 4; color: #3fe0c5"/>
                <image id="m" src="demo:white" tinted="true" style="color: #ff0000"/>
                <image id="n" src="demo:white"/></column></template>
                """, null);
        ui.doc.resize(20, 20);
        ui.doc.frame(0);
        ImageCanvas c = ui.render();
        Element i = ui.find("i");
        assertEquals(0xFF3FE0C5, c.pixel((int) i.box().x + 2, (int) i.box().y + 2));
        assertEquals(0xFF1F7062, c.pixel((int) i.box().x, (int) i.box().y));
        Element m = ui.find("m");
        assertEquals(0xFFFF0000, c.pixel((int) m.box().x + 2, (int) m.box().y + 2));
        Element n = ui.find("n");
        assertEquals(0xFFFFFFFF, c.pixel((int) n.box().x + 2, (int) n.box().y + 2));
    }
}
