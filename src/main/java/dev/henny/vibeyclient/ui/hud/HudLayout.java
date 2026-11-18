package dev.henny.vibeyclient.ui.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

/**
 * Where the movable HUD elements sit. Any module registers its {@link HudElement}s once (constructor/onLoad) and
 * draws them with {@link #draw(Gfx, HudElement)} (or {@link #begin}/{@link #end}) from its HUD hook; the player moves
 * and scales them in the {@link HudEditorScreen} ({@link #openEditor(Screen)}). Placements are saved in
 * {@code config/vibeyclient/hud.json} (anchor, offset, scale, hidden), and stay put when the window changes size:
 * each element remembers the nearest screen anchor (corner, edge middle or centre) and its distance to it.
 * Client thread only. See docs/UI.md "HUD editor".
 */
public final class HudLayout {
    private HudLayout() {}

    /** One element's placement. Mutable; changed by the editor only. */
    public static final class Placement {
        public Anchor anchor;
        public int dx;
        public int dy;
        public float scale;
        public boolean hidden;
        boolean custom;

        Placement(HudElement e) {
            reset(e);
        }

        void reset(HudElement e) {
            anchor = e.defaultAnchor();
            dx = e.defaultX();
            dy = e.defaultY();
            scale = e.defaultScale();
            hidden = false;
            custom = false;
        }

        /** The player moved, scaled or hid it (else the element's defaults apply). */
        public boolean isCustom() {
            return custom;
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

    private static final Map<String, HudElement> ELEMENTS = new LinkedHashMap<>();
    private static final Map<String, Placement> PLACEMENTS = new LinkedHashMap<>();
    private static final SettingsGroup FILE = new SettingsGroup("hud");
    private static final Box TMP = new Box();
    private static boolean loaded;
    private static int depth;

    /** Registers (or replaces, same id) an element; returns it. */
    public static synchronized HudElement register(HudElement e) {
        ELEMENTS.put(e.id(), e);
        Placement p = new Placement(e);
        if (loaded) read(e.id(), p);
        PLACEMENTS.put(e.id(), p);
        return e;
    }

    public static synchronized void unregister(String id) {
        ELEMENTS.remove(id);
        PLACEMENTS.remove(id);
    }

    /** All registered elements in registration order. */
    public static synchronized List<HudElement> elements() {
        return Collections.unmodifiableList(new ArrayList<>(ELEMENTS.values()));
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

    /** Computes where {@code e} sits on a sw × sh screen into {@code out} (kept on screen). */
    public static Box box(HudElement e, int sw, int sh, Box out) {
        Placement p = placement(e);
        float s = p.scale;
        float w = e.width() * s;
        float h = e.height() * s;
        float x = p.anchor.fx() * (sw - w) + p.dx;
        float y = p.anchor.fy() * (sh - h) + p.dy;
        out.x = Math.max(0, Math.min(sw - w, x));
        out.y = Math.max(0, Math.min(sh - h, y));
        out.w = w;
        out.h = h;
        out.scale = s;
        return out;
    }

    /** Draws {@code e} at its placement (nothing if the player hid it). */
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
     * nothing) if the player hid it. Always pair a true result with {@link #end(Gfx)}.
     */
    public static boolean begin(Gfx g, HudElement e) {
        Placement p = placement(e);
        // hidden by the player, or the editor is open (it draws every element itself)
        if (p.hidden || HudEditorScreen.open) return false;
        Box b = box(e, g.screenWidth(), g.screenHeight(), TMP);
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

    /** Moves {@code e} so its top left corner is at (x, y) on a sw × sh screen; picks the nearest anchor. */
    static void moveTo(HudElement e, float x, float y, int sw, int sh) {
        Placement p = placement(e);
        float w = e.width() * p.scale;
        float h = e.height() * p.scale;
        x = Math.max(0, Math.min(sw - w, x));
        y = Math.max(0, Math.min(sh - h, y));
        Anchor a = Anchor.nearest(x + w / 2f, y + h / 2f, sw, sh);
        p.anchor = a;
        p.dx = Math.round(x - a.fx() * (sw - w));
        p.dy = Math.round(y - a.fy() * (sh - h));
        p.custom = true;
    }

    /** Scales {@code e} around its centre (clamped to the element's range). */
    static void scaleTo(HudElement e, float scale, int sw, int sh) {
        Placement p = placement(e);
        Box b = box(e, sw, sh, new Box());
        float cx = b.x + b.w / 2f;
        float cy = b.y + b.h / 2f;
        p.scale = Math.max(e.minScale(), Math.min(e.maxScale(), Math.round(scale * 20f) / 20f));
        moveTo(e, cx - e.width() * p.scale / 2f, cy - e.height() * p.scale / 2f, sw, sh);
    }

    static void setHidden(HudElement e, boolean hidden) {
        Placement p = placement(e);
        p.hidden = hidden;
        p.custom = true;
    }

    static void reset(HudElement e) {
        placement(e).reset(e);
    }

    static void resetAll() {
        for (HudElement e : elements()) reset(e);
    }

    /** Copies of every placement (the editor's "cancel"). */
    static Map<String, Placement> snapshot() {
        Map<String, Placement> out = new LinkedHashMap<>();
        for (HudElement e : elements()) {
            Placement src = placement(e);
            Placement c = new Placement(e);
            c.anchor = src.anchor;
            c.dx = src.dx;
            c.dy = src.dy;
            c.scale = src.scale;
            c.hidden = src.hidden;
            c.custom = src.custom;
            out.put(e.id(), c);
        }
        return out;
    }

    static void restore(Map<String, Placement> snap) {
        for (Map.Entry<String, Placement> en : snap.entrySet()) {
            Placement p = PLACEMENTS.get(en.getKey());
            if (p == null) continue;
            Placement s = en.getValue();
            p.anchor = s.anchor;
            p.dx = s.dx;
            p.dy = s.dy;
            p.scale = s.scale;
            p.hidden = s.hidden;
            p.custom = s.custom;
        }
    }

    // ============================================================================================ persistence

    private static void ensureLoaded() {
        if (loaded) return;
        synchronized (HudLayout.class) {
            if (loaded) return;
            FILE.load();
            loaded = true;
            for (Map.Entry<String, Placement> en : PLACEMENTS.entrySet()) read(en.getKey(), en.getValue());
        }
    }

    private static void read(String id, Placement p) {
        JsonElement el = FILE.data().get(id);
        if (el == null || !el.isJsonObject()) return;
        JsonObject o = el.getAsJsonObject();
        try {
            if (o.has("anchor")) p.anchor = Anchor.valueOf(o.get("anchor").getAsString().toUpperCase(Locale.ROOT));
            if (o.has("x")) p.dx = o.get("x").getAsInt();
            if (o.has("y")) p.dy = o.get("y").getAsInt();
            if (o.has("scale")) p.scale = o.get("scale").getAsFloat();
            if (o.has("hidden")) p.hidden = o.get("hidden").getAsBoolean();
            p.custom = true;
            HudElement e = ELEMENTS.get(id);
            if (e != null) p.scale = Math.max(e.minScale(), Math.min(e.maxScale(), p.scale));
        } catch (RuntimeException ex) {
            dev.henny.vibeyclient.Vibey.LOG.warn("Bad HUD placement for {}: {}", id, o);
        }
    }

    /** Writes every customised placement (debounced, on a worker thread). */
    static void save() {
        ensureLoaded();
        JsonObject data = FILE.data();
        synchronized (FILE) {
            for (Map.Entry<String, Placement> en : PLACEMENTS.entrySet()) {
                Placement p = en.getValue();
                if (!p.custom) {
                    data.remove(en.getKey());
                    continue;
                }
                JsonObject o = new JsonObject();
                o.addProperty("anchor", p.anchor.name().toLowerCase(Locale.ROOT));
                o.addProperty("x", p.dx);
                o.addProperty("y", p.dy);
                o.addProperty("scale", p.scale);
                if (p.hidden) o.addProperty("hidden", true);
                data.add(en.getKey(), o);
            }
        }
        FILE.markDirty();
    }
}
