package dev.henny.vibeyclient.ui.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

final class HudRegistry {
    private HudRegistry() {}

    static final Map<String, HudElement> ELEMENTS = new LinkedHashMap<>();
    static final Map<String, HudLayout.Placement> PLACEMENTS = new LinkedHashMap<>();
    static final Map<String, TreeSet<Integer>> COPIES = new LinkedHashMap<>();
    static final HudStyle GLOBAL = HudStyle.defaults();
    static HudElement[] widgets = new HudElement[0];
    private static int version;

    static synchronized HudElement register(HudElement e) {
        put(e);
        TreeSet<Integer> c = COPIES.get(e.id());
        if (c != null && e.duplicable()) for (int i : c) put(e.copy(i));
        return e;
    }

    private static void put(HudElement e) {
        ELEMENTS.put(e.id(), e);
        HudLayout.Placement p = new HudLayout.Placement(e);
        if (HudFile.loaded()) PlacementJson.read(e.id(), p, HudFile.data().get(e.id()));
        PLACEMENTS.put(e.id(), p);
        rebuild();
    }

    static synchronized void unregister(String id) {
        ELEMENTS.remove(id);
        PLACEMENTS.remove(id);
        rebuild();
    }

    static void addCopy(HudElement c, HudLayout.Placement p) {
        ELEMENTS.put(c.id(), c);
        PLACEMENTS.put(c.id(), p);
    }

    static void rebuild() {
        List<HudElement> w = new ArrayList<>();
        for (HudElement e : ELEMENTS.values()) if (e.isWidget()) w.add(e);
        widgets = w.toArray(new HudElement[0]);
        version++;
    }

    static synchronized List<HudElement> elements() {
        return Collections.unmodifiableList(new ArrayList<>(ELEMENTS.values()));
    }

    static synchronized void elements(List<HudElement> out) {
        out.clear();
        out.addAll(ELEMENTS.values());
    }

    static int version() {
        return version;
    }

    static HudElement get(String id) {
        return ELEMENTS.get(id);
    }

    static HudLayout.Placement placement(HudElement e) {
        HudFile.ensureLoaded();
        HudLayout.Placement p = PLACEMENTS.get(e.id());
        if (p == null) {
            p = new HudLayout.Placement(e);
            PLACEMENTS.put(e.id(), p);
        }
        return p;
    }
}
