package dev.henny.vibeyclient.ui.hud;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;

/**
 * Where the movable HUD elements sit and how they look. Modules register their {@link HudElement}s once
 * (constructor/onLoad). Widgets ({@link HudElement.Builder#widget}) are drawn by the core every frame
 * ({@link #renderWidgets}); plain elements are drawn by their owner with {@link #draw(Gfx, HudElement)} (or
 * {@link #begin}/{@link #end}). The player arranges everything in the {@link HudEditorScreen} ({@link #openEditor}).
 *
 * <p>Placement model: each element stores an {@link Anchor} (picked automatically from where it was dropped unless
 * the player fixed it), its offset from that anchor, its scale and (widgets resized freely) its box size, so layouts
 * survive other window sizes and GUI scales. Widgets also store style overrides ({@link HudStyle}) and options.
 * Saved in {@code config/vibeyclient/hud-layout.json}; entries of elements that are not registered in this run (a
 * module that is off) are kept. Client thread only. See docs/HUD.md.
 */
public final class HudLayout {
    private HudLayout() {}

    /** One element's placement. Changed by the editor (and templates) only. */
    public static final class Placement {
        public Anchor anchor;
        /** The anchor follows the position (else the player fixed it). */
        public boolean autoAnchor = true;
        public int dx;
        public int dy;
        public float scale;
        /** Box size in unscaled GUI px set by free resizing (0 = natural size). Widgets only. */
        public int bw;
        public int bh;
        public boolean hidden;
        /** Style overrides (null fields inherit the global style). */
        public final HudStyle style = new HudStyle();
        /** Widget options (key → value). */
        public final Map<String, String> opts = new LinkedHashMap<>();
        boolean custom;
        final HudStyle.Resolved resolved = new HudStyle.Resolved();
        int resolvedVersion = -1;
        int rulesVersion = -1;
        boolean rulesDisabled;

        Placement(HudElement e) {
            reset(e);
        }

        void reset(HudElement e) {
            anchor = e.defaultAnchor();
            autoAnchor = true;
            dx = e.defaultX();
            dy = e.defaultY();
            scale = e.defaultScale();
            bw = 0;
            bh = 0;
            hidden = e.startHidden();
            style.clear();
            opts.clear();
            custom = false;
            styleVersion++;
        }

        void copyFrom(Placement o) {
            anchor = o.anchor;
            autoAnchor = o.autoAnchor;
            dx = o.dx;
            dy = o.dy;
            scale = o.scale;
            bw = o.bw;
            bh = o.bh;
            hidden = o.hidden;
            style.set(o.style);
            opts.clear();
            opts.putAll(o.opts);
            custom = o.custom;
            styleVersion++;
        }

        /** The player moved, scaled, styled or hid it (else the element's defaults apply). */
        public boolean isCustom() {
            return custom;
        }

        boolean sameAs(Placement o) {
            return anchor == o.anchor && autoAnchor == o.autoAnchor && dx == o.dx && dy == o.dy && scale == o.scale
                    && bw == o.bw && bh == o.bh && hidden == o.hidden && style.equals(o.style) && opts.equals(o.opts);
        }
    }

    /** A placed element on a screen of a given size (GUI pixels; w/h are scaled). Reused, do not keep it. */
    public static final class Box {
        public float x;
        public float y;
        public float w;
        public float h;
        public float scale;
    }

    /**
     * The whole layout at one moment (global style, placements, which copies exist): undo/redo, cancel, templates and
     * share codes work on these. Immutable once made by {@link #capture()} (copies are taken).
     */
    public static final class State {
        final HudStyle global = new HudStyle();
        final Map<String, Placement> placements = new LinkedHashMap<>();
        /** base id → copy indices (2, 3, ...). */
        final Map<String, TreeSet<Integer>> copies = new LinkedHashMap<>();
        /** Element ids a template/share code mentions but this run does not know (kept for the file). */
        final Map<String, JsonObject> unknown = new LinkedHashMap<>();

        /** Same layout (styles, placements, copies). */
        public boolean sameAs(State o) {
            if (!global.equals(o.global) || !copies.equals(o.copies) || placements.size() != o.placements.size()) return false;
            for (Map.Entry<String, Placement> en : placements.entrySet()) {
                Placement p = o.placements.get(en.getKey());
                if (p == null || !p.sameAs(en.getValue())) return false;
            }
            return true;
        }
    }

    private static final Map<String, HudElement> ELEMENTS = new LinkedHashMap<>();
    private static final Map<String, Placement> PLACEMENTS = new LinkedHashMap<>();
    /** base id → copy indices that should exist (also for bases not registered yet). */
    private static final Map<String, TreeSet<Integer>> COPIES = new LinkedHashMap<>();
    private static final HudStyle GLOBAL = HudStyle.defaults();
    private static final SettingsGroup FILE = new SettingsGroup("hud-layout");
    private static final Box TMP = new Box();
    private static final HudPaint PAINT = new HudPaint();
    private static HudElement[] widgets = new HudElement[0];
    private static Placement[] widgetPlacements = new Placement[0];
    private static boolean loaded;
    private static int depth;
    static int styleVersion;
    private static int rulesVersion;
    private static int registryVersion;
    private static BooleanSupplier hideWhen = () -> false;

    static {
        ServerRules.listen(() -> rulesVersion++);
        Events.subscribe(VibeyEvents.LanguageChanged.class, e -> invalidateWidgets());
    }

    // ============================================================================================ registry

    /** Registers (or replaces, same id) an element; returns it. Copies the player made of it come back too. */
    public static synchronized HudElement register(HudElement e) {
        put(e);
        TreeSet<Integer> c = COPIES.get(e.id());
        if (c != null && e.duplicable()) for (int i : c) put(e.copy(i));
        return e;
    }

    private static void put(HudElement e) {
        ELEMENTS.put(e.id(), e);
        Placement p = new Placement(e);
        if (loaded) read(e.id(), p, FILE.data().get(e.id()));
        PLACEMENTS.put(e.id(), p);
        rebuild();
    }

    public static synchronized void unregister(String id) {
        ELEMENTS.remove(id);
        PLACEMENTS.remove(id);
        rebuild();
    }

    private static void rebuild() {
        List<HudElement> w = new ArrayList<>();
        for (HudElement e : ELEMENTS.values()) if (e.isWidget()) w.add(e);
        widgets = w.toArray(new HudElement[0]);
        widgetPlacements = new Placement[widgets.length];
        for (int i = 0; i < widgets.length; i++) widgetPlacements[i] = PLACEMENTS.get(widgets[i].id());
        registryVersion++;
    }

    /** All registered elements in registration order. */
    public static synchronized List<HudElement> elements() {
        return Collections.unmodifiableList(new ArrayList<>(ELEMENTS.values()));
    }

    /** Fills {@code out} with the registered elements (no allocation once {@code out} has room). */
    public static synchronized void elements(List<HudElement> out) {
        out.clear();
        out.addAll(ELEMENTS.values());
    }

    /** Changes whenever elements are registered or removed. */
    public static int registryVersion() {
        return registryVersion;
    }

    public static HudElement get(String id) {
        return ELEMENTS.get(id);
    }

    public static Placement placement(HudElement e) {
        ensureLoaded();
        Placement p = PLACEMENTS.get(e.id());
        if (p == null) {
            p = new Placement(e);
            PLACEMENTS.put(e.id(), p);
        }
        return p;
    }

    /** The HUD editor is open (normal HUD drawing of the elements is paused). */
    public static boolean editing() {
        return HudEditorScreen.open;
    }

    /** Hidden by the player in the editor. */
    public static boolean hidden(HudElement e) {
        return placement(e).hidden;
    }

    /** -1/0/1: align content left/centre/right inside the box (follows the anchor column). */
    public static int alignX(HudElement e) {
        return placement(e).anchor.alignX();
    }

    /** The global style (inherited by every widget). Change it through the editor or {@link #styleChanged()}. */
    public static HudStyle globalStyle() {
        return GLOBAL;
    }

    /** Call after changing a style or option directly: re-resolves styles and refreshes the widgets. */
    public static void styleChanged() {
        styleVersion++;
        invalidateWidgets();
    }

    /** The resolved style of an element (cached; safe every frame). */
    public static HudStyle.Resolved style(HudElement e) {
        return resolved(placement(e));
    }

    private static HudStyle.Resolved resolved(Placement p) {
        if (p.resolvedVersion != styleVersion) {
            HudStyle.resolve(GLOBAL, p.style, p.resolved);
            p.resolvedVersion = styleVersion;
        }
        return p.resolved;
    }

    /** True while the server disables the element (cached per rules change; cheap). */
    public static boolean serverDisabled(HudElement e) {
        return serverDisabled(e, placement(e));
    }

    private static boolean serverDisabled(HudElement e, Placement p) {
        if (e.rulesKey() == null) return false;
        if (p.rulesVersion != rulesVersion) {
            p.rulesDisabled = e.serverDisabled();
            p.rulesVersion = rulesVersion;
        }
        return p.rulesDisabled;
    }

    /** Widgets are not drawn while this holds (e.g. the HUD module's "hide while F3 is open"). */
    public static void hideWidgetsWhen(BooleanSupplier condition) {
        hideWhen = condition == null ? () -> false : condition;
    }

    public static void invalidateWidgets() {
        for (HudElement e : widgets) e.widget().invalidate();
    }

    // ============================================================================================ geometry

    /** Unscaled box width (natural, or the free size). */
    public static int boxWidth(HudElement e) {
        return boxWidth(e, placement(e));
    }

    public static int boxHeight(HudElement e) {
        return boxHeight(e, placement(e));
    }

    private static int boxWidth(HudElement e, Placement p) {
        int n = e.width();
        if (p.bw <= 0 || !e.isWidget()) return n;
        return Math.max(e.freeSize() ? e.minWidth() : n, p.bw);
    }

    private static int boxHeight(HudElement e, Placement p) {
        int n = e.height();
        if (p.bh <= 0 || !e.isWidget()) return n;
        return Math.max(e.freeSize() ? e.minHeight() : n, p.bh);
    }

    /** Computes where {@code e} sits on a sw × sh screen into {@code out} (kept on screen). */
    public static Box box(HudElement e, int sw, int sh, Box out) {
        return box(e, placement(e), sw, sh, out);
    }

    private static Box box(HudElement e, Placement p, int sw, int sh, Box out) {
        float s = p.scale;
        float w = boxWidth(e, p) * s;
        float h = boxHeight(e, p) * s;
        float x = p.anchor.fx() * (sw - w) + p.dx;
        float y = p.anchor.fy() * (sh - h) + p.dy;
        out.x = Math.max(0, Math.min(sw - w, x));
        out.y = Math.max(0, Math.min(sh - h, y));
        // whole GUI pixels keep 1 px lines crisp
        out.x = Math.round(out.x);
        out.y = Math.round(out.y);
        out.w = w;
        out.h = h;
        out.scale = s;
        return out;
    }

    // ============================================================================================ drawing

    /**
     * Draws every widget at its placement (called by the core from the HUD hook, every frame). Skips hidden ones,
     * the ones the server disabled and everything while the editor is open (it draws them itself). No allocation.
     */
    public static void renderWidgets(Gfx g) {
        if (HudEditorScreen.open || widgets.length == 0) return;
        ensureLoaded();
        boolean hide;
        try {
            hide = hideWhen.getAsBoolean();
        } catch (RuntimeException ex) {
            hide = false;
        }
        if (hide) return;
        long now = System.currentTimeMillis();
        int sw = g.screenWidth(), sh = g.screenHeight();
        HudElement[] arr = widgets;
        Placement[] pls = widgetPlacements;
        for (int i = 0; i < arr.length; i++) {
            HudElement e = arr[i];
            Placement p = pls[i];
            if (p == null || p.hidden || !e.available() || serverDisabled(e, p)) continue;
            paintWidget(g, e, p, now, sw, sh, false);
        }
    }

    /** Draws one widget (editor and HUD). Returns false if it had nothing to show (in game). */
    static boolean paintWidget(Gfx g, HudElement e, Placement p, long now, int sw, int sh, boolean preview) {
        HudWidget w = e.widget();
        HudPaint paint = PAINT;
        paint.g = g;
        paint.preview = preview;
        paint.style = resolved(p);
        paint.now = now;
        paint.element = e;
        paint.placement = p;
        paint.w = boxWidth(e, p);
        paint.h = boxHeight(e, p);
        try {
            w.maybeUpdate(paint, now);
            if (!preview && !w.shown(paint)) return false;
            Box b = box(e, p, sw, sh, TMP);
            paint.w = boxWidth(e, p);
            paint.h = boxHeight(e, p);
            g.push();
            g.translate(b.x, b.y);
            g.scale(b.scale, b.scale);
            try {
                w.paint(paint);
            } finally {
                g.pop();
            }
        } catch (RuntimeException ex) {
            if (!e.brokenLogged) {
                e.brokenLogged = true;
                dev.henny.vibeyclient.Vibey.LOG.warn("HUD widget {} failed", e.id(), ex);
            }
            return false;
        }
        return true;
    }

    static Placement placementOrNull(String id) {
        return PLACEMENTS.get(id);
    }

    /** Draws {@code e} at its placement (nothing if the player hid it). Plain elements. */
    public static void draw(Gfx g, HudElement e) {
        if (!begin(g, e)) return;
        try {
            e.render(g, e.width(), e.height(), false);
        } finally {
            end(g);
        }
    }

    /**
     * Pushes the element's position and scale: afterwards draw into (0, 0)–(width, height). Returns false (and pushes
     * nothing) if the player hid it, the server disabled it or the editor is open. Pair a true result with {@link #end}.
     */
    public static boolean begin(Gfx g, HudElement e) {
        Placement p = placement(e);
        if (p.hidden || HudEditorScreen.open || serverDisabled(e, p)) return false;
        Box b = box(e, p, g.screenWidth(), g.screenHeight(), TMP);
        g.push();
        g.translate(b.x, b.y);
        g.scale(b.scale, b.scale);
        depth++;
        return true;
    }

    public static void end(Gfx g) {
        if (depth <= 0) return;
        depth--;
        g.pop();
    }

    public static void openEditor(Screen parent) {
        ClientCompat.openScreen(new HudEditorScreen(parent));
    }

    // ============================================================================================ editing

    /** Moves {@code e} so its top left corner is at (x, y) on a sw × sh screen; picks the nearest anchor (if auto). */
    public static void moveTo(HudElement e, float x, float y, int sw, int sh) {
        Placement p = placement(e);
        float w = boxWidth(e, p) * p.scale;
        float h = boxHeight(e, p) * p.scale;
        x = Math.max(0, Math.min(sw - w, x));
        y = Math.max(0, Math.min(sh - h, y));
        Anchor a = p.autoAnchor ? Anchor.nearest(x + w / 2f, y + h / 2f, sw, sh) : p.anchor;
        p.anchor = a;
        p.dx = Math.round(x - a.fx() * (sw - w));
        p.dy = Math.round(y - a.fy() * (sh - h));
        p.custom = true;
    }

    /** Scales {@code e} around its centre (clamped to the element's range). */
    public static void scaleTo(HudElement e, float scale, int sw, int sh) {
        Placement p = placement(e);
        Box b = box(e, p, sw, sh, new Box());
        float cx = b.x + b.w / 2f;
        float cy = b.y + b.h / 2f;
        p.scale = clampScale(e, scale);
        moveTo(e, cx - boxWidth(e, p) * p.scale / 2f, cy - boxHeight(e, p) * p.scale / 2f, sw, sh);
    }

    static float clampScale(HudElement e, float s) {
        return Math.max(e.minScale(), Math.min(e.maxScale(), Math.round(s * 40f) / 40f));
    }

    /** Sets the free box size (unscaled px; 0 = natural). */
    static void setSize(HudElement e, int w, int h) {
        Placement p = placement(e);
        p.bw = w <= e.width() && !e.freeSize() ? 0 : w;
        p.bh = h <= e.height() && !e.freeSize() ? 0 : h;
        if (p.bw > 0 && p.bw == e.width() && e.freeSize() && p.bh <= 0) p.bw = 0;
        p.custom = true;
    }

    /** Fixes the anchor ({@code null} = automatic again), keeping the element where it is. */
    public static void setAnchor(HudElement e, Anchor a, int sw, int sh) {
        Placement p = placement(e);
        Box b = box(e, p, sw, sh, new Box());
        p.autoAnchor = a == null;
        if (a != null) p.anchor = a;
        moveTo(e, b.x, b.y, sw, sh);
    }

    public static void setHidden(HudElement e, boolean hidden) {
        Placement p = placement(e);
        p.hidden = hidden;
        p.custom = true;
    }

    public static void setOption(HudElement e, String key, String value) {
        Placement p = placement(e);
        HudOption opt = null;
        for (HudOption o : e.options()) if (o.key.equals(key)) opt = o;
        if (opt == null || !opt.accepts(value)) return;
        if (value.equals(opt.def)) p.opts.remove(key);
        else p.opts.put(key, value);
        p.custom = true;
        if (e.isWidget()) e.widget().invalidate();
    }

    public static void reset(HudElement e) {
        Placement p = placement(e);
        boolean wasHidden = p.hidden;
        p.reset(e);
        // a reset keeps it on the HUD (only "Hide" takes it away)
        p.hidden = wasHidden;
        p.custom = p.hidden != e.startHidden() || e.isCopy();
        if (e.isWidget()) e.widget().invalidate();
    }

    /** Every element back to its default placement and style (call {@link #save()} to keep it). */
    public static void resetAll() {
        for (HudElement e : elements()) {
            placement(e).reset(e);
            if (e.isCopy()) removeCopy(e);
        }
        GLOBAL.set(HudStyle.defaults());
        styleChanged();
    }

    /** Adds a copy of a duplicable widget (placed below the original); returns it, or null. */
    public static synchronized HudElement duplicate(HudElement e, int sw, int sh) {
        HudElement base = ELEMENTS.get(e.baseId());
        if (base == null || !base.duplicable()) return null;
        TreeSet<Integer> set = COPIES.computeIfAbsent(base.id(), k -> new TreeSet<>());
        int i = 2;
        while (set.contains(i)) i++;
        if (i > 32) return null;
        set.add(i);
        HudElement c = base.copy(i);
        ELEMENTS.put(c.id(), c);
        Placement p = new Placement(c);
        Placement src = placement(e);
        p.copyFrom(src);
        p.hidden = false;
        PLACEMENTS.put(c.id(), p);
        rebuild();
        Box b = box(e, src, sw, sh, new Box());
        float ny = b.y + b.h + 2 + b.h <= sh ? b.y + b.h + 2 : b.y - b.h - 2;
        moveTo(c, b.x, ny, sw, sh);
        return c;
    }

    /** Removes a copy (originals cannot be removed, only hidden). */
    public static synchronized void removeCopy(HudElement e) {
        if (!e.isCopy()) return;
        TreeSet<Integer> set = COPIES.get(e.baseId());
        if (set != null) {
            set.remove(e.copyIndex());
            if (set.isEmpty()) COPIES.remove(e.baseId());
        }
        ELEMENTS.remove(e.id());
        PLACEMENTS.remove(e.id());
        FILE.data().remove(e.id());
        rebuild();
    }

    // ============================================================================================ states

    /** A copy of the whole current layout. */
    public static State capture() {
        ensureLoaded();
        State s = new State();
        s.global.set(GLOBAL);
        for (Map.Entry<String, Placement> en : PLACEMENTS.entrySet()) {
            HudElement e = ELEMENTS.get(en.getKey());
            if (e == null) continue;
            Placement c = new Placement(e);
            c.copyFrom(en.getValue());
            s.placements.put(en.getKey(), c);
        }
        for (Map.Entry<String, TreeSet<Integer>> en : COPIES.entrySet()) s.copies.put(en.getKey(), new TreeSet<>(en.getValue()));
        return s;
    }

    /** Makes {@code s} (from {@link #capture()}) the current layout again (undo, redo, cancel). */
    public static void restore(State s) {
        apply(s, false);
    }

    /**
     * Applies a state. With {@code hideUnlisted}, widgets the state does not mention are hidden (templates and share
     * codes describe a complete widget layout); plain elements it does not mention keep their placement.
     */
    public static synchronized void apply(State s, boolean hideUnlisted) {
        ensureLoaded();
        GLOBAL.set(HudStyle.defaults());
        GLOBAL.overlay(s.global);
        // copies: remove the ones not in s, add the missing ones
        for (HudElement e : new ArrayList<>(ELEMENTS.values())) {
            if (!e.isCopy()) continue;
            TreeSet<Integer> want = s.copies.get(e.baseId());
            if (want == null || !want.contains(e.copyIndex())) removeCopy(e);
        }
        for (Map.Entry<String, TreeSet<Integer>> en : s.copies.entrySet()) {
            TreeSet<Integer> have = COPIES.computeIfAbsent(en.getKey(), k -> new TreeSet<>());
            HudElement base = ELEMENTS.get(en.getKey());
            for (int i : en.getValue()) {
                if (i < 2 || i > 32 || have.contains(i)) continue;
                have.add(i);
                if (base != null && base.duplicable()) {
                    HudElement c = base.copy(i);
                    ELEMENTS.put(c.id(), c);
                    PLACEMENTS.put(c.id(), new Placement(c));
                }
            }
        }
        rebuild();
        for (Map.Entry<String, HudElement> en : ELEMENTS.entrySet()) {
            HudElement e = en.getValue();
            Placement p = placement(e);
            Placement src = s.placements.get(en.getKey());
            if (src != null) {
                p.copyFrom(src);
                p.scale = clampScale(e, p.scale);
            } else if (hideUnlisted && e.isWidget()) {
                p.reset(e);
                p.hidden = true;
                p.custom = true;
            }
        }
        for (Map.Entry<String, JsonObject> en : s.unknown.entrySet()) FILE.data().add(en.getKey(), en.getValue().deepCopy());
        styleChanged();
    }

    /** Builds states in code (templates): {@code HudLayout.state().global(style).place("hud.fps", ...)...}. */
    public static StateBuilder state() {
        return new StateBuilder();
    }

    public static final class StateBuilder {
        private final State s = new State();

        public StateBuilder global(HudStyle style) {
            s.global.set(style);
            return this;
        }

        /** Places an element (shown) at anchor + offset with a scale; returns a handle for style/options. */
        public Entry place(String id, Anchor a, int dx, int dy, float scale) {
            HudElement e = ELEMENTS.get(id);
            if (e == null) {
                // a copy that does not exist yet ("hud.items.2"): it is created when the state is applied
                int dot = id.lastIndexOf('.');
                HudElement base = dot > 0 ? ELEMENTS.get(id.substring(0, dot)) : null;
                int n = copyIndexOf(id);
                if (base != null && base.duplicable() && n >= 2 && n <= 32) {
                    e = base.copy(n);
                    s.copies.computeIfAbsent(base.id(), k -> new TreeSet<>()).add(n);
                }
            }
            Placement p = e != null ? new Placement(e) : null;
            if (p == null) {
                // not registered (module off): keep it as raw data so the file still gets it
                JsonObject o = new JsonObject();
                o.addProperty("a", a.name().toLowerCase(Locale.ROOT));
                o.addProperty("x", dx);
                o.addProperty("y", dy);
                o.addProperty("s", scale);
                s.unknown.put(id, o);
                return new Entry(this, null);
            }
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

        public StateBuilder copies(String baseId, int count) {
            TreeSet<Integer> set = new TreeSet<>();
            for (int i = 2; i <= count; i++) set.add(i);
            if (!set.isEmpty()) s.copies.put(baseId, set);
            return this;
        }

        public State build() {
            return s;
        }

        public static final class Entry {
            private final StateBuilder owner;
            private final Placement p;

            Entry(StateBuilder owner, Placement p) {
                this.owner = owner;
                this.p = p;
            }

            /** Style overrides for this element. */
            public Entry style(java.util.function.Consumer<HudStyle> edit) {
                if (p != null) edit.accept(p.style);
                return this;
            }

            public Entry option(String key, String value) {
                if (p != null) p.opts.put(key, value);
                return this;
            }

            /** Fixed anchor (else automatic). */
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

            public StateBuilder copies(String baseId, int count) {
                return owner.copies(baseId, count);
            }

            public State build() {
                return owner.build();
            }
        }
    }

    // ============================================================================================ json (file + share)

    static JsonObject placementJson(Placement p) {
        JsonObject o = new JsonObject();
        o.addProperty("a", p.anchor.name().toLowerCase(Locale.ROOT));
        if (!p.autoAnchor) o.addProperty("fix", true);
        o.addProperty("x", p.dx);
        o.addProperty("y", p.dy);
        o.addProperty("s", Math.round(p.scale * 1000f) / 1000f);
        if (p.bw > 0) o.addProperty("w", p.bw);
        if (p.bh > 0) o.addProperty("h", p.bh);
        if (p.hidden) o.addProperty("hide", true);
        if (!p.style.isEmpty()) o.add("st", p.style.toJson());
        if (!p.opts.isEmpty()) {
            JsonObject op = new JsonObject();
            for (Map.Entry<String, String> en : p.opts.entrySet()) op.addProperty(en.getKey(), en.getValue());
            o.add("o", op);
        }
        return o;
    }

    /** Reads a placement (old "anchor"/"scale"/"hidden" keys too). Bad values are skipped; never throws. */
    static void read(String id, Placement p, JsonElement el) {
        if (el == null || !el.isJsonObject()) return;
        JsonObject o = el.getAsJsonObject();
        HudElement e = ELEMENTS.get(id);
        try {
            String a = str(o, "a", str(o, "anchor", null));
            if (a != null) {
                try {
                    p.anchor = Anchor.valueOf(a.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    // keep the default
                }
            }
            p.autoAnchor = !bool(o, "fix", false);
            p.dx = clampInt(num(o, "x", p.dx), -4096, 4096);
            p.dy = clampInt(num(o, "y", p.dy), -4096, 4096);
            float s = (float) numF(o, "s", numF(o, "scale", p.scale));
            p.scale = Float.isFinite(s) ? s : p.scale;
            p.bw = clampInt(num(o, "w", 0), 0, 2048);
            p.bh = clampInt(num(o, "h", 0), 0, 2048);
            p.hidden = bool(o, "hide", bool(o, "hidden", false));
            p.style.set(HudStyle.fromJson(o.get("st")));
            p.opts.clear();
            if (o.has("o") && o.get("o").isJsonObject()) {
                for (Map.Entry<String, JsonElement> en : o.getAsJsonObject("o").entrySet()) {
                    if (!en.getValue().isJsonPrimitive()) continue;
                    String v = en.getValue().getAsString();
                    HudOption opt = null;
                    if (e != null) for (HudOption x : e.options()) if (x.key.equals(en.getKey())) opt = x;
                    if (e == null || (opt != null && opt.accepts(v))) p.opts.put(en.getKey(), v);
                }
            }
            p.custom = true;
            if (e != null) p.scale = clampScale(e, p.scale);
            else p.scale = Math.max(0.25f, Math.min(4f, p.scale));
        } catch (RuntimeException ex) {
            dev.henny.vibeyclient.Vibey.LOG.warn("Bad HUD placement for {}: {}", id, o);
        }
        styleVersion++;
    }

    /** The state as JSON ({@code {"v":1,"g":{style},"e":{id:{...}},"c":{base:[2,3]}}}). */
    static JsonObject stateJson(State s, boolean onlyCustom) {
        JsonObject root = new JsonObject();
        root.addProperty("v", 1);
        JsonObject g = s.global.toJson();
        if (g.size() > 0) root.add("g", g);
        JsonObject els = new JsonObject();
        for (Map.Entry<String, Placement> en : s.placements.entrySet()) {
            if (onlyCustom && !en.getValue().custom) continue;
            els.add(en.getKey(), placementJson(en.getValue()));
        }
        for (Map.Entry<String, JsonObject> en : s.unknown.entrySet()) if (!els.has(en.getKey())) els.add(en.getKey(), en.getValue());
        root.add("e", els);
        if (!s.copies.isEmpty()) {
            JsonObject c = new JsonObject();
            for (Map.Entry<String, TreeSet<Integer>> en : s.copies.entrySet()) {
                JsonArray arr = new JsonArray();
                for (int i : en.getValue()) arr.add(i);
                c.add(en.getKey(), arr);
            }
            root.add("c", c);
        }
        return root;
    }

    /**
     * Parses a state (share code / file). Unknown element ids are kept as raw data, bad values skipped. Throws
     * {@link IllegalArgumentException} if it is not a layout at all.
     */
    static State parseState(JsonObject root) {
        if (root == null || !root.has("e") || !root.get("e").isJsonObject()) throw new IllegalArgumentException("no elements");
        JsonElement v = root.get("v");
        if (v != null && (!v.isJsonPrimitive() || v.getAsInt() > 1)) throw new IllegalArgumentException("newer version");
        State s = new State();
        s.global.set(HudStyle.fromJson(root.get("g")));
        if (root.has("c") && root.get("c").isJsonObject()) {
            for (Map.Entry<String, JsonElement> en : root.getAsJsonObject("c").entrySet()) {
                if (!validId(en.getKey()) || !en.getValue().isJsonArray()) continue;
                TreeSet<Integer> set = new TreeSet<>();
                for (JsonElement i : en.getValue().getAsJsonArray()) {
                    if (i.isJsonPrimitive() && i.getAsJsonPrimitive().isNumber()) {
                        int n = i.getAsInt();
                        if (n >= 2 && n <= 32) set.add(n);
                    }
                }
                if (!set.isEmpty()) s.copies.put(en.getKey(), set);
            }
        }
        int count = 0;
        for (Map.Entry<String, JsonElement> en : root.getAsJsonObject("e").entrySet()) {
            if (++count > 256) break;
            String id = en.getKey();
            if (!validId(id) || !en.getValue().isJsonObject()) continue;
            HudElement e = ELEMENTS.get(id);
            if (e == null) {
                // a copy of a known base that does not exist yet, or a module that is off
                HudElement base = baseOfCopy(id, s);
                if (base != null) e = base.copy(copyIndexOf(id));
            }
            if (e == null) {
                s.unknown.put(id, en.getValue().getAsJsonObject());
                continue;
            }
            Placement p = new Placement(e);
            readFor(e, p, en.getValue());
            s.placements.put(id, p);
        }
        return s;
    }

    private static void readFor(HudElement e, Placement p, JsonElement el) {
        // read() resolves options against the registered element; copies are not registered yet
        boolean registered = ELEMENTS.containsKey(e.id());
        if (!registered) ELEMENTS.put(e.id(), e);
        try {
            read(e.id(), p, el);
        } finally {
            if (!registered) ELEMENTS.remove(e.id());
        }
    }

    private static HudElement baseOfCopy(String id, State s) {
        int dot = id.lastIndexOf('.');
        if (dot <= 0) return null;
        HudElement base = ELEMENTS.get(id.substring(0, dot));
        int n = copyIndexOf(id);
        if (base == null || !base.duplicable() || n < 2) return null;
        TreeSet<Integer> set = s.copies.get(base.id());
        return set != null && set.contains(n) ? base : null;
    }

    private static int copyIndexOf(String id) {
        try {
            return Integer.parseInt(id.substring(id.lastIndexOf('.') + 1));
        } catch (RuntimeException e) {
            return -1;
        }
    }

    static boolean validId(String id) {
        return id != null && id.length() <= 64 && id.matches("[a-z0-9_.]+");
    }

    private static String str(JsonObject o, String k, String def) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
    }

    private static boolean bool(JsonObject o, String k, boolean def) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : def;
    }

    private static int num(JsonObject o, String k, int def) {
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return def;
        double d = e.getAsDouble();
        return Double.isFinite(d) ? (int) Math.round(d) : def;
    }

    private static double numF(JsonObject o, String k, double def) {
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return def;
        double d = e.getAsDouble();
        return Double.isFinite(d) ? d : def;
    }

    private static int clampInt(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ============================================================================================ persistence

    /** Editor flags kept in the layout file (e.g. "intro" = the first-run intro was shown). */
    public static boolean flag(String key) {
        ensureLoaded();
        JsonElement f = FILE.data().get("_flags");
        return f != null && f.isJsonObject() && f.getAsJsonObject().has(key) && f.getAsJsonObject().get(key).getAsBoolean();
    }

    public static void setFlag(String key, boolean on) {
        ensureLoaded();
        JsonObject f = FILE.data().has("_flags") && FILE.data().get("_flags").isJsonObject()
                ? FILE.data().getAsJsonObject("_flags") : new JsonObject();
        if (on) f.addProperty(key, true);
        else f.remove(key);
        FILE.data().add("_flags", f);
        FILE.markDirty();
    }

    /** No layout was ever saved (first run: the HUD module applies its default template). */
    public static boolean fresh() {
        ensureLoaded();
        return !FILE.data().has("_saved");
    }

    private static void ensureLoaded() {
        if (loaded) return;
        synchronized (HudLayout.class) {
            if (loaded) return;
            FILE.load();
            loaded = true;
            JsonObject data = FILE.data();
            if (data.size() == 0) migrateLegacy(data);
            GLOBAL.set(HudStyle.defaults());
            GLOBAL.overlay(HudStyle.fromJson(data.get("_global")));
            if (data.has("_copies") && data.get("_copies").isJsonObject()) {
                for (Map.Entry<String, JsonElement> en : data.getAsJsonObject("_copies").entrySet()) {
                    if (!en.getValue().isJsonArray()) continue;
                    TreeSet<Integer> set = new TreeSet<>();
                    for (JsonElement i : en.getValue().getAsJsonArray()) {
                        try {
                            int n = i.getAsInt();
                            if (n >= 2 && n <= 32) set.add(n);
                        } catch (RuntimeException ignored) {
                            // skip
                        }
                    }
                    if (!set.isEmpty()) COPIES.put(en.getKey(), set);
                }
            }
            // copies of bases registered before the file was read
            for (HudElement e : new ArrayList<>(ELEMENTS.values())) {
                TreeSet<Integer> c = COPIES.get(e.id());
                if (c != null && e.duplicable()) for (int i : c) if (!ELEMENTS.containsKey(e.id() + "." + i)) {
                    HudElement copy = e.copy(i);
                    ELEMENTS.put(copy.id(), copy);
                    PLACEMENTS.put(copy.id(), new Placement(copy));
                }
            }
            for (Map.Entry<String, Placement> en : PLACEMENTS.entrySet()) read(en.getKey(), en.getValue(), data.get(en.getKey()));
            rebuild();
        }
    }

    /** Before 0.3 the placements lived in hud.json (now the HUD module's settings file). */
    private static void migrateLegacy(JsonObject data) {
        try {
            SettingsGroup old = new SettingsGroup("hud");
            old.load();
            for (Map.Entry<String, JsonElement> en : old.data().entrySet()) {
                if (en.getValue().isJsonObject() && en.getValue().getAsJsonObject().has("anchor")) {
                    data.add(en.getKey(), en.getValue().deepCopy());
                }
            }
            if (data.size() > 0) {
                data.addProperty("_saved", true);
                FILE.markDirty();
            }
        } catch (RuntimeException e) {
            dev.henny.vibeyclient.Vibey.LOG.warn("Could not migrate the old HUD layout", e);
        }
    }

    /** Writes the layout (debounced, on a worker thread). */
    public static void save() {
        ensureLoaded();
        JsonObject data = FILE.data();
        synchronized (FILE) {
            for (Map.Entry<String, Placement> en : PLACEMENTS.entrySet()) {
                Placement p = en.getValue();
                if (!p.custom) data.remove(en.getKey());
                else data.add(en.getKey(), placementJson(p));
            }
            JsonObject g = GLOBAL.toJson();
            HudStyle d = HudStyle.defaults();
            // only what differs from the defaults
            JsonObject dj = d.toJson();
            for (String k : dj.keySet()) if (dj.get(k).equals(g.get(k))) g.remove(k);
            if (g.size() > 0) data.add("_global", g);
            else data.remove("_global");
            JsonObject c = new JsonObject();
            for (Map.Entry<String, TreeSet<Integer>> en : COPIES.entrySet()) {
                JsonArray arr = new JsonArray();
                for (int i : en.getValue()) arr.add(i);
                c.add(en.getKey(), arr);
            }
            if (c.size() > 0) data.add("_copies", c);
            else data.remove("_copies");
            data.addProperty("_saved", true);
        }
        FILE.markDirty();
    }

    /** The layout file (for tests). */
    public static java.nio.file.Path file() {
        return FILE.file();
    }
}
