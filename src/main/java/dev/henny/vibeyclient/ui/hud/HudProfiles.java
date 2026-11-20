package dev.henny.vibeyclient.ui.hud;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class HudProfiles {
    private HudProfiles() {}

    static final HudProfile MAIN = new HudProfile(HudProfile.MAIN, "hud-layout", null);
    private static final Map<String, HudProfile> ALL = new LinkedHashMap<>(Map.of(MAIN.id(), MAIN));
    private static volatile HudProfile active = MAIN;

    static HudProfile active() {
        return active;
    }

    static synchronized void register(String id, Set<String> shown) {
        if (!id.matches("[a-z0-9_]{1,24}") || id.equals(HudProfile.MAIN)) throw new IllegalArgumentException(id);
        ALL.put(id, new HudProfile(id, "hud-layout-" + id, Set.copyOf(shown)));
    }

    static synchronized boolean use(String id) {
        HudProfile p = ALL.get(id);
        if (p == null || p == active) return false;
        HudFile.saveIfLoaded();
        active = p;
        HudFile.reload();
        if (!p.main() && HudFile.fresh()) {
            String def = HudTemplates.defaultId();
            if (def != null && HudTemplates.apply(def)) HudFile.save();
        }
        return true;
    }
}
