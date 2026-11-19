package dev.henny.vibeyclient.ranks.style;

public record PulseStyle(int[] colors, double speed, double min) implements NameStyle {
    @Override
    public int colorAt(int index, int length, double t) {
        int[] stops = colors.length == 1 ? new int[] {colors[0], ColorMath.mix(0, colors[0], min)} : colors;
        return ColorMath.ramp(stops, ColorMath.triangle(speed * t));
    }

    @Override
    public boolean animated() {
        return speed != 0;
    }
}
