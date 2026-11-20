package dev.henny.vibeyclient.ui.vui.menu;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.ui.vui.settings.PageSections;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ModuleGrid {
    public final ListState<ModuleTile> modules = new ListState<>();
    public final ListState<ModuleTile> creators = new ListState<>();
    public final State<Boolean> showCreators = new State<>(false);
    private final Map<String, ModuleTile> tiles = new LinkedHashMap<>();
    private String query = "";
    private String lang = "";
    private String visible = "";

    public void filter(String q) {
        query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        rebuild();
    }

    public void sync() {
        String l = Lang.code();
        String v = visibleIds();
        if (!l.equals(lang) || !v.equals(visible)) {
            if (!l.equals(lang)) tiles.clear();
            lang = l;
            visible = v;
            rebuild();
        }
        for (ModuleTile t : tiles.values()) t.getOn().sync();
    }

    public ModuleTile tile(String id) {
        for (ModuleTile t : modules.get()) if (t.getId().equals(id)) return t;
        for (ModuleTile t : creators.get()) if (t.getId().equals(id)) return t;
        return null;
    }

    private void rebuild() {
        List<ModuleTile> main = new ArrayList<>();
        List<ModuleTile> creator = new ArrayList<>();
        for (VibeyModule m : ModuleManager.get().all()) {
            if (!m.visible()) continue;
            ModuleTile t = tiles.computeIfAbsent(m.id(), k -> new ModuleTile(m));
            if (query.isEmpty()) {
                (m.category() == ModuleCategory.CREATOR ? creator : main).add(t.hit(""));
                continue;
            }
            String text = (t.getName() + " " + t.getTip()).toLowerCase(Locale.ROOT);
            String setting = text.contains(query) ? null : PageSections.match(m.settings(), query);
            if (text.contains(query) || setting != null) main.add(t.hit(setting));
        }
        modules.replaceAll(main);
        creators.replaceAll(creator);
    }

    private static String visibleIds() {
        StringBuilder sb = new StringBuilder();
        for (VibeyModule m : ModuleManager.get().all()) if (m.visible()) sb.append(m.id()).append(',');
        return sb.toString();
    }
}
