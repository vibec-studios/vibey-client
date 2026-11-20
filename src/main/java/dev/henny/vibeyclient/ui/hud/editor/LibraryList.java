package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.ServerBadge;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudTemplates;

import java.util.ArrayList;
import java.util.List;

final class LibraryList {
    private final EditorController c;
    private int signature = Integer.MIN_VALUE;
    private String lang;

    LibraryList(EditorController c) {
        this.c = c;
    }

    void sync() {
        int sig = HudLayout.registryVersion();
        for (HudElement e : c.session.available()) sig = sig * 31 + (HudLayout.hidden(e) ? 1 : 2) + (HudLayout.serverDisabled(e) ? 4 : 0);
        String l = Lang.code();
        if (sig == signature && l.equals(lang)) return;
        signature = sig;
        lang = l;
        c.widgets.replaceAll(items());
        c.presets.replaceAll(presets());
    }

    private List<Items.Library> items() {
        List<Items.Library> out = new ArrayList<>();
        for (HudElement e : c.session.available()) {
            boolean off = HudLayout.serverDisabled(e);
            String tip = off ? ServerBadge.hint() : e.descriptionKey() == null ? e.displayName() : Lang.tr(e.descriptionKey());
            out.add(new Items.Library(e.id(), e.displayName(), tip, !HudLayout.hidden(e), off, e.duplicable() && !e.isCopy(), e));
        }
        return out;
    }

    private static List<Items.Preset> presets() {
        List<Items.Preset> out = new ArrayList<>();
        for (HudTemplates.Template t : HudTemplates.all()) {
            String tip = t.descKey() == null ? "" : Lang.tr(t.descKey());
            out.add(new Items.Preset(t.id(), Lang.tr(t.labelKey()), icon(t.id()), tip));
        }
        return out;
    }

    private static String icon(String id) {
        return switch (id) {
            case "pvp" -> "crosshair";
            case "building" -> "cube";
            case "streamer" -> "star";
            case "minimal" -> "hud";
            default -> "layers";
        };
    }
}
