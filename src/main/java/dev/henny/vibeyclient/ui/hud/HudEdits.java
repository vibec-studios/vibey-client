package dev.henny.vibeyclient.ui.hud;

import java.util.TreeSet;

final class HudEdits {
    private HudEdits() {}

    static void setHidden(HudElement e, boolean hidden) {
        HudLayout.Placement p = HudRegistry.placement(e);
        p.hidden = hidden;
        p.custom = true;
    }

    static void setOption(HudElement e, String key, String value) {
        HudLayout.Placement p = HudRegistry.placement(e);
        HudOption opt = option(e, key);
        if (opt == null || !opt.accepts(value)) return;
        if (value.equals(opt.def)) p.opts.remove(key);
        else p.opts.put(key, value);
        p.custom = true;
        if (e.isWidget()) e.widget().invalidate();
    }

    static HudOption option(HudElement e, String key) {
        for (HudOption o : e.options()) if (o.key.equals(key)) return o;
        return null;
    }

    static void reset(HudElement e) {
        HudLayout.Placement p = HudRegistry.placement(e);
        boolean wasHidden = p.hidden;
        Placements.reset(p, e);
        p.hidden = wasHidden;
        p.custom = p.hidden != HudProfiles.active().hiddenByDefault(e) || e.isCopy();
        if (e.isWidget()) e.widget().invalidate();
    }

    static void resetAll() {
        for (HudElement e : HudRegistry.elements()) {
            Placements.reset(HudRegistry.placement(e), e);
            if (e.isCopy()) removeCopy(e);
        }
        HudRegistry.GLOBAL.set(HudStyle.defaults());
        HudCaches.styleChanged();
    }

    static synchronized HudElement duplicate(HudElement e, int sw, int sh) {
        HudElement base = HudRegistry.ELEMENTS.get(e.baseId());
        if (base == null || !base.duplicable()) return null;
        TreeSet<Integer> set = HudRegistry.COPIES.computeIfAbsent(base.id(), k -> new TreeSet<>());
        int i = 2;
        while (set.contains(i)) i++;
        if (i > 32) return null;
        set.add(i);
        HudElement c = base.copy(i);
        HudLayout.Placement src = HudRegistry.placement(e);
        HudLayout.Placement p = Placements.copyOf(c, src);
        p.hidden = false;
        HudRegistry.addCopy(c, p);
        HudRegistry.rebuild();
        HudLayout.Box b = HudGeometry.box(e, src, sw, sh, new HudLayout.Box());
        float ny = b.y + b.h + 2 + b.h <= sh ? b.y + b.h + 2 : b.y - b.h - 2;
        HudGeometry.moveTo(c, b.x, ny, sw, sh);
        return c;
    }

    static synchronized void removeCopy(HudElement e) {
        if (!e.isCopy()) return;
        TreeSet<Integer> set = HudRegistry.COPIES.get(e.baseId());
        if (set != null) {
            set.remove(e.copyIndex());
            if (set.isEmpty()) HudRegistry.COPIES.remove(e.baseId());
        }
        HudRegistry.ELEMENTS.remove(e.id());
        HudRegistry.PLACEMENTS.remove(e.id());
        HudFile.data().remove(e.id());
        HudRegistry.rebuild();
    }
}
