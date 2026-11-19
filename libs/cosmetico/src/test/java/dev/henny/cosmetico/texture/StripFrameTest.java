package dev.henny.cosmetico.texture;

import dev.henny.cosmetico.model.Texture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StripFrameTest {
    private static Texture strip(int frameTime, boolean interpolate, int... frames) {
        return new Texture("t", 16, 16, new byte[0], 16, 64, 4, frameTime, frames, interpolate);
    }

    @Test
    void followsTheFrameOrder() {
        Texture t = strip(2, false, 3, 1, 0);
        assertEquals(new StripFrame(3, 1, 0), StripFrame.at(t, 0, 0f));
        assertEquals(new StripFrame(1, 0, 0), StripFrame.at(t, 2, 0.5f));
        assertEquals(new StripFrame(0, 3, 0), StripFrame.at(t, 5, 0f));
        assertEquals(new StripFrame(3, 1, 0), StripFrame.at(t, 6, 0f));
    }

    @Test
    void interpolationIsQuantisedToEighths() {
        Texture t = strip(4, true, 0, 1, 2, 3);
        StripFrame f = StripFrame.at(t, 1, 0.5f);
        assertEquals(new StripFrame(0, 1, 3), f);
        assertEquals(3 / 8f, f.blend(), 1e-6f);
        assertEquals(0 * 4096 + 1 * 8 + 3, f.key());
        assertEquals(7, StripFrame.at(t, 3, 0.99f).q());
    }

    @Test
    void pixelsBlendPerChannel() {
        assertEquals(0x80808080, StripPixels.lerp(0x00000000, 0xFFFFFFFF, 0.5f) | 0x80808080);
        assertEquals(0xFF00FF00, StripPixels.lerp(0xFF00FF00, 0xFF00FF00, 0.3f));
    }
}
