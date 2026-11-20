package dev.henny.vui.mc.draw;

final class Colors {
    private Colors() {
    }

    static int alpha(int argb, float alpha) {
        if (alpha >= 1f) return argb;
        int a = Math.round((argb >>> 24) * Math.max(0f, alpha));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    static int alphaOf(int argb) {
        return argb >>> 24;
    }
}
