package dev.henny.vibeyclient.client.branding;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowIconsTest {
    @Test
    void everyStandardSizeIsASquareTransparentPng() throws Exception {
        for (int size : WindowIcons.SIZES) {
            try (InputStream in = WindowIcons.class.getResourceAsStream(WindowIcons.path(size))) {
                assertNotNull(in, WindowIcons.path(size));
                BufferedImage img = ImageIO.read(in);
                assertEquals(size, img.getWidth());
                assertEquals(size, img.getHeight());
                assertTrue(img.getColorModel().hasAlpha());
            }
        }
    }
}
