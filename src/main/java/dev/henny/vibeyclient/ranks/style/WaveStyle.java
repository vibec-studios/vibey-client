package dev.henny.vibeyclient.ranks.style;

public record WaveStyle(int[] colors, double speed, double wavelength, boolean rtl) implements NameStyle {
    @Override
    public int colorAt(int index, int length, double t) {
        return ColorMath.ramp(colors, ColorMath.triangle(index / wavelength - ColorMath.sign(rtl) * speed * t));
    }

    @Override
    public boolean animated() {
        return speed != 0;
    }
}
