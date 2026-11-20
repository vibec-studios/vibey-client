package dev.henny.vui;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.style.Theme;
import dev.henny.vui.style.ThemeLoader;

import java.util.ArrayList;
import java.util.List;

final class ThemeFiles {
    private final Vui vui;
    private final List<String> paths = new ArrayList<>();
    private Theme base;
    private Diagnostics diagnostics = new Diagnostics();

    ThemeFiles(Vui vui) {
        this.vui = vui;
    }

    void load(String resourcePath) {
        if (base == null) base = vui.theme();
        if (!paths.contains(resourcePath)) paths.add(resourcePath);
        apply();
    }

    boolean changed(String relativePath) {
        if (!paths.contains(relativePath)) return false;
        apply();
        return true;
    }

    Diagnostics diagnostics() {
        return diagnostics;
    }

    private void apply() {
        Diagnostics d = new Diagnostics();
        Theme next = base;
        for (String path : paths) {
            String source = vui.registry().readResource(path);
            if (source == null) d.error(new SourcePos(path, 1, 1), "theme file " + path + " not found", "check the resource roots");
            else next = ThemeLoader.load(vui.registry().describe(path), source, next, d);
        }
        diagnostics = d;
        vui.theme(next);
    }
}
