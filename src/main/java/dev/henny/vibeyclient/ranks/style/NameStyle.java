package dev.henny.vibeyclient.ranks.style;

public sealed interface NameStyle permits GradientStyle, WaveStyle, RainbowStyle, PulseStyle, ShimmerStyle {
    int colorAt(int index, int length, double t);

    boolean animated();
}
