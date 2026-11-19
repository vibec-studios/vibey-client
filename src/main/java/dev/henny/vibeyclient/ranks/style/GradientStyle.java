package dev.henny.vibeyclient.ranks.style;

public record GradientStyle(int[] colors, double speed, boolean rtl) implements NameStyle {
    @Override
    public int colorAt(int index, int length, double t) {
        double along = length <= 1 ? 0 : (double) index / (length - 1);
        double v = speed == 0 ? along : ColorMath.triangle(along / 2 - ColorMath.sign(rtl) * speed * t);
        return ColorMath.ramp(colors, v);
    }

    @Override
    public boolean animated() {
        return speed != 0;
    }
}
