package dev.henny.vibeyclient.ui.hud;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;

final class HudStates {
    private HudStates() {}

    static HudLayout.State capture() {
        HudFile.ensureLoaded();
        HudLayout.State s = new HudLayout.State();
        s.global.set(HudRegistry.GLOBAL);
        for (Map.Entry<String, HudLayout.Placement> en : HudRegistry.PLACEMENTS.entrySet()) {
            HudElement e = HudRegistry.ELEMENTS.get(en.getKey());
            if (e != null) s.placements.put(en.getKey(), Placements.copyOf(e, en.getValue()));
        }
        for (Map.Entry<String, TreeSet<Integer>> en : HudRegistry.COPIES.entrySet()) s.copies.put(en.getKey(), new TreeSet<>(en.getValue()));
        return s;
    }

    static boolean same(HudLayout.State a, HudLayout.State o) {
        if (!a.global.equals(o.global) || !a.copies.equals(o.copies) || a.placements.size() != o.placements.size()) return false;
        for (Map.Entry<String, HudLayout.Placement> en : a.placements.entrySet()) {
            HudLayout.Placement p = o.placements.get(en.getKey());
            if (p == null || !Placements.same(p, en.getValue())) return false;
        }
        return true;
    }

    static synchronized void apply(HudLayout.State s, boolean hideUnlisted) {
        HudFile.ensureLoaded();
        HudRegistry.GLOBAL.set(HudStyle.defaults());
        HudRegistry.GLOBAL.overlay(s.global);
        syncCopies(s);
        for (Map.Entry<String, HudElement> en : HudRegistry.ELEMENTS.entrySet()) {
            HudElement e = en.getValue();
            HudLayout.Placement p = HudRegistry.placement(e);
            HudLayout.Placement src = s.placements.get(en.getKey());
            if (src != null) {
                Placements.copy(p, src);
                p.scale = HudGeometry.clampScale(e, p.scale);
            } else if (hideUnlisted && HudProfiles.active().hidesUnlisted(e)) {
                Placements.reset(p, e);
                p.hidden = true;
                p.custom = true;
            }
        }
        for (Map.Entry<String, JsonObject> en : s.unknown.entrySet()) HudFile.data().add(en.getKey(), en.getValue().deepCopy());
        HudCaches.styleChanged();
    }

    private static void syncCopies(HudLayout.State s) {
        for (HudElement e : new ArrayList<>(HudRegistry.ELEMENTS.values())) {
            if (!e.isCopy()) continue;
            TreeSet<Integer> want = s.copies.get(e.baseId());
            if (want == null || !want.contains(e.copyIndex())) HudEdits.removeCopy(e);
        }
        for (Map.Entry<String, TreeSet<Integer>> en : s.copies.entrySet()) {
            TreeSet<Integer> have = HudRegistry.COPIES.computeIfAbsent(en.getKey(), k -> new TreeSet<>());
            HudElement base = HudRegistry.ELEMENTS.get(en.getKey());
            for (int i : en.getValue()) {
                if (i < 2 || i > 32 || have.contains(i)) continue;
                have.add(i);
                if (base != null && base.duplicable()) {
                    HudElement c = base.copy(i);
                    HudRegistry.addCopy(c, new HudLayout.Placement(c));
                }
            }
        }
        HudRegistry.rebuild();
    }
}
