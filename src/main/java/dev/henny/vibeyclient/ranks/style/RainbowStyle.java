package dev.henny.vibeyclient.ranks.style;

public record RainbowStyle(double saturation, double lightness, double speed, double spread, boolean rtl) implements NameStyle {
    @Override
    public int colorAt(int index, int length, double t) {
        double hue = ColorMath.frac(index * spread - ColorMath.sign(rtl) * speed * t);
        return ColorMath.hsl(hue, saturation, lightness);
    }

    @Override
    public boolean animated() {
        return speed != 0;
    }
}
