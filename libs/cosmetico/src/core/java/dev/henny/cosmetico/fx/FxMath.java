package dev.henny.cosmetico.fx;

import dev.henny.cosmetico.model.Locator;
import dev.henny.cosmetico.state.Avatar;

public final class FxMath {
    private FxMath() {}

    public static void position(Avatar a, Locator l, double[] out) {
        if (l != null) {
            double[] p = a.locators.peek(l.name());
            if (p != null && !Double.isNaN(p[0])) {
                out[0] = p[0];
                out[1] = p[1];
                out[2] = p[2];
                return;
            }
        }
        out[0] = a.frame.x;
        out[1] = a.frame.y + 1.2;
        out[2] = a.frame.z;
    }

    public static void toWorld(float bodyYaw, float x, float y, float z, float[] out) {
        double ang = Math.toRadians(180f - bodyYaw);
        float c = (float) Math.cos(ang), s = (float) Math.sin(ang);
        out[0] = x * c + z * s;
        out[1] = y;
        out[2] = -x * s + z * c;
    }
}
