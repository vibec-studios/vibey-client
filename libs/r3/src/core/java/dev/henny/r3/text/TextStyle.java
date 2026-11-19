package dev.henny.r3.text;

public record TextStyle(int color, int background, float padding, int glowColor, float glowStrength,
                        int depthColor, boolean seeThrough, float lineSpacing) {
    public static final TextStyle DEFAULT = new TextStyle(0xFFFFFFFF, 0, 2f, 0, 0f, 0, false, 1f);
    public static final TextStyle NAMETAG = new TextStyle(0xFFFFFFFF, 0x40000000, 1f, 0, 0f, 0, false, 1f);

    public TextStyle withColor(int argb) {
        return new TextStyle(argb, background, padding, glowColor, glowStrength, depthColor, seeThrough, lineSpacing);
    }

    public TextStyle withBackground(int argb, float pad) {
        return new TextStyle(color, argb, pad, glowColor, glowStrength, depthColor, seeThrough, lineSpacing);
    }

    public TextStyle withGlow(int argb, float strength) {
        return new TextStyle(color, background, padding, argb, strength, depthColor, seeThrough, lineSpacing);
    }

    public TextStyle withDepth(int argb) {
        return new TextStyle(color, background, padding, glowColor, glowStrength, argb, seeThrough, lineSpacing);
    }

    public TextStyle withSeeThrough(boolean on) {
        return new TextStyle(color, background, padding, glowColor, glowStrength, depthColor, on, lineSpacing);
    }

    public TextStyle withLineSpacing(float spacing) {
        return new TextStyle(color, background, padding, glowColor, glowStrength, depthColor, seeThrough, spacing);
    }
}
