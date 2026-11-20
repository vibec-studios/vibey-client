package dev.henny.vui.mc.resource;

import dev.henny.vui.Vui;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.mc.VuiMod;
import dev.henny.vui.reload.ResourceRoot;
import dev.henny.vui.style.Theme;
import dev.henny.vui.style.ThemeLoader;
import dev.henny.vui.style.VibeTheme;

public final class Themes {
    private static final String[] EXTENSIONS = {".theme.json", ".theme"};

    private Themes() {
    }

    public static Theme load(Vui vui, String id) {
        int colon = id.indexOf(':');
        if (colon <= 0) return null;
        String base = "assets/" + id.substring(0, colon) + "/vui/" + id.substring(colon + 1);
        for (ResourceRoot root : vui.registry().allRoots()) {
            for (String ext : EXTENSIONS) {
                String text = root.read(base + ext);
                if (text == null) continue;
                Diagnostics diagnostics = new Diagnostics();
                Theme theme = ThemeLoader.load(root.describe(base + ext), text, VibeTheme.create(), diagnostics);
                if (!diagnostics.isEmpty()) VuiMod.LOG.warn("vui: theme {}:\n{}", id, diagnostics.format());
                return theme;
            }
        }
        VuiMod.LOG.warn("vui: theme {} not found ({}.theme.json)", id, base);
        return null;
    }
}
