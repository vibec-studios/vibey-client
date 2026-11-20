package dev.henny.vibeyclient.ui.vui.theme;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Accent {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;
    private static final float HOVER = 0.16f;

    private Accent() {}

    public static Map<String, String> tokens(int argb) {
        int brand = 0xFF000000 | argb;
        boolean light = Contrast.ratio(WHITE, brand) >= Contrast.ratio(BLACK, brand);
        int onBrand = light ? WHITE : BLACK;
        int hover = blend(brand, light ? BLACK : WHITE, HOVER);
        Map<String, String> out = new LinkedHashMap<>();
        out.put("brand", Contrast.hex(brand));
        out.put("brand-hover", Contrast.hex(hover));
        out.put("on-brand", Contrast.hex(onBrand));
        return out;
    }

    static int blend(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
