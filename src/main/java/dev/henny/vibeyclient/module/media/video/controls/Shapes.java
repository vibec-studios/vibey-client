package dev.henny.vibeyclient.module.media.video.controls;

import java.util.List;

public final class Shapes {
    public static final List<String> LABELS = List.of("16:9", "4:3", "1:1", "21:9", "9:16");
    private static final float[] ASPECTS = {16f / 9f, 4f / 3f, 1f, 21f / 9f, 9f / 16f};

    private Shapes() {}

    public static float aspect(String label) {
        int i = LABELS.indexOf(label);
        return i < 0 ? ASPECTS[0] : ASPECTS[i];
    }

    public static String label(float aspect) {
        int best = 0;
        for (int i = 1; i < ASPECTS.length; i++) if (Math.abs(ASPECTS[i] - aspect) < Math.abs(ASPECTS[best] - aspect)) best = i;
        return LABELS.get(best);
    }
}
