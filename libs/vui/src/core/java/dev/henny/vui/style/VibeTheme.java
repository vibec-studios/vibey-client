package dev.henny.vui.style;

public final class VibeTheme {
    private VibeTheme() {
    }

    public static Theme create() {
        return new Theme()
                .put("bg", "#000000")
                .put("surface", "#0b0b0e")
                .put("surface2", "#121217")
                .put("surface3", "#18181f")
                .put("panel", "#08080b")
                .put("line", "#ffffff17")
                .put("line2", "#ffffff29")
                .put("text", "#f4f4f5")
                .put("muted", "#a1a1aa")
                .put("dim", "#6b6b75")
                .put("brand", "#8b6cff")
                .put("brand2", "#3fe0c5")
                .put("brand-hover", "#9e84ff")
                .put("on-brand", "#ffffff")
                .put("ok", "#3fe0c5")
                .put("danger", "#ff5470")
                .put("warn", "#ffb547")
                .put("scrim", "#000000c8")
                .put("pad", "10")
                .put("row", "24")
                .put("gap", "4")
                .put("duration", "150ms");
    }
}
