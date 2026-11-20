package dev.henny.vibeyclient.ui.hud;

import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.TreeSet;
import java.util.function.Consumer;

public final class HudStateBuilder {
    private final HudLayout.State s = new HudLayout.State();

    HudStateBuilder() {}

    public HudStateBuilder global(HudStyle style) {
        s.global.set(style);
        return this;
    }

    public Entry place(String id, Anchor a, int dx, int dy, float scale) {
        HudElement e = elementFor(id);
        if (e == null) {
            JsonObject o = new JsonObject();
            o.addProperty("a", a.name().toLowerCase(Locale.ROOT));
            o.addProperty("x", dx);
            o.addProperty("y", dy);
            o.addProperty("s", scale);
            s.unknown.put(id, o);
            return new Entry(this, null);
        }
        HudLayout.Placement p = new HudLayout.Placement(e);
        p.anchor = a;
        p.autoAnchor = true;
        p.dx = dx;
        p.dy = dy;
        p.scale = scale;
        p.hidden = false;
        p.custom = true;
        s.placements.put(id, p);
        return new Entry(this, p);
    }

    private HudElement elementFor(String id) {
        HudElement e = HudRegistry.ELEMENTS.get(id);
        if (e != null) return e;
        int dot = id.lastIndexOf('.');
        HudElement base = dot > 0 ? HudRegistry.ELEMENTS.get(id.substring(0, dot)) : null;
        int n = StateJson.copyIndexOf(id);
        if (base == null || !base.duplicable() || n < 2 || n > 32) return null;
        s.copies.computeIfAbsent(base.id(), k -> new TreeSet<>()).add(n);
        return base.copy(n);
    }

    public HudStateBuilder copies(String baseId, int count) {
        TreeSet<Integer> set = new TreeSet<>();
        for (int i = 2; i <= count; i++) set.add(i);
        if (!set.isEmpty()) s.copies.put(baseId, set);
        return this;
    }

    public HudLayout.State build() {
        return s;
    }

    public static final class Entry {
        private final HudStateBuilder owner;
        private final HudLayout.Placement p;

        Entry(HudStateBuilder owner, HudLayout.Placement p) {
            this.owner = owner;
            this.p = p;
        }

        public Entry style(Consumer<HudStyle> edit) {
            if (p != null) edit.accept(p.style);
            return this;
        }

        public Entry option(String key, String value) {
            if (p != null) p.opts.put(key, value);
            return this;
        }

        public Entry fixed() {
            if (p != null) p.autoAnchor = false;
            return this;
        }

        public Entry size(int w, int h) {
            if (p != null) {
                p.bw = w;
                p.bh = h;
            }
            return this;
        }

        public Entry place(String id, Anchor a, int dx, int dy, float scale) {
            return owner.place(id, a, dx, dy, scale);
        }

        public HudStateBuilder copies(String baseId, int count) {
            return owner.copies(baseId, count);
        }

        public HudLayout.State build() {
            return owner.build();
        }
    }
}
