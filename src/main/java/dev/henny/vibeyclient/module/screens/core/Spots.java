package dev.henny.vibeyclient.module.screens.core;

import java.util.List;

public final class Spots {
    public static final int MARGIN = 3;

    private Spots() {}

    public static Box pick(float screenW, float screenH, float size, List<Box> taken) {
        float far = screenW - MARGIN - size, low = screenH - MARGIN - size;
        float[][] tries = {{far, low}, {far, MARGIN}, {MARGIN, MARGIN}, {MARGIN, low},
                {screenW / 2f - size / 2f, low}, {far, screenH / 2f - size / 2f}};
        for (float[] t : tries) {
            Box b = new Box(t[0], t[1], size, size);
            if (free(b, taken)) return b;
        }
        return new Box(far, low, size, size);
    }

    static boolean free(Box b, List<Box> taken) {
        for (Box o : taken) {
            if (b.x() < o.right() + 1 && o.x() < b.right() + 1 && b.y() < o.bottom() + 1 && o.y() < b.bottom() + 1) return false;
        }
        return true;
    }
}
