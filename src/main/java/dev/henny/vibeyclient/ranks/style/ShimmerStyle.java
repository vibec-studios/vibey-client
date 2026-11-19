package dev.henny.vibeyclient.ranks.style;

public record ShimmerStyle(int base, int highlight, double width, double speed, double pause, boolean rtl) implements NameStyle {
    @Override
    public int colorAt(int index, int length, double t) {
        if (speed <= 0) return base;
        double sweep = 1 / speed;
        double cycle = sweep + pause;
        double inCycle = ColorMath.frac(t / cycle) * cycle;
        if (inCycle >= sweep) return base;
        double position = rtl ? length - 1 - index : index;
        double center = -width + (length + 2 * width) * (inCycle / sweep);
        double strength = Math.max(0, 1 - Math.abs(position - center) / width);
        return ColorMath.mix(base, highlight, strength);
    }

    @Override
    public boolean animated() {
        return speed > 0;
    }
}
