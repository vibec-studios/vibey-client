package dev.henny.vibeyclient.ui.hud;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.hud.editor.HudEditorScreen;
import dev.henny.vibeyclient.ui.hud.render.HudRenderer;
import net.minecraft.client.gui.screens.Screen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;

public final class HudLayout {
    private HudLayout() {}

    public static final class Placement {
        public Anchor anchor;
        public boolean autoAnchor = true;
        public int dx;
        public int dy;
        public float scale;
        public int bw;
        public int bh;
        public boolean hidden;
        public final HudStyle style = new HudStyle();
        public final Map<String, String> opts = new LinkedHashMap<>();
        boolean custom;
        final HudStyle.Resolved resolved = new HudStyle.Resolved();
        int resolvedVersion = -1;
        int rulesVersion = -1;
        boolean rulesDisabled;

        Placement(HudElement e) {
            Placements.reset(this, e);
        }

        public boolean isCustom() {
            return custom;
        }
    }

    public static final class Box {
        public float x;
        public float y;
        public float w;
        public float h;
        public float scale;
    }

    public static final class State {
        final HudStyle global = new HudStyle();
        final Map<String, Placement> placements = new LinkedHashMap<>();
        final Map<String, TreeSet<Integer>> copies = new LinkedHashMap<>();
        final Map<String, JsonObject> unknown = new LinkedHashMap<>();

        public boolean sameAs(State o) {
            return HudStates.same(this, o);
        }
    }

    private static boolean editing;

    public static HudElement register(HudElement e) {
        return HudRegistry.register(e);
    }

    public static void unregister(String id) {
        HudRegistry.unregister(id);
    }

    public static List<HudElement> elements() {
        return HudRegistry.elements();
    }

    public static void elements(List<HudElement> out) {
        HudRegistry.elements(out);
    }

    public static int registryVersion() {
        return HudRegistry.version();
    }

    public static HudElement get(String id) {
        return HudRegistry.get(id);
    }

    public static Placement placement(HudElement e) {
        return HudRegistry.placement(e);
    }

    public static boolean editing() {
        return editing;
    }

    public static void editing(boolean open) {
        editing = open;
    }

    public static boolean hidden(HudElement e) {
        return placement(e).hidden;
    }

    public static int alignX(HudElement e) {
        return placement(e).anchor.alignX();
    }

    public static HudStyle globalStyle() {
        return HudRegistry.GLOBAL;
    }

    public static void styleChanged() {
        HudCaches.styleChanged();
    }

    public static HudStyle.Resolved style(HudElement e) {
        return HudCaches.resolved(placement(e));
    }

    public static boolean serverDisabled(HudElement e) {
        return HudCaches.serverDisabled(e, placement(e));
    }

    public static void hideWidgetsWhen(BooleanSupplier condition) {
        HudCaches.hideWhen(condition);
    }

    public static boolean widgetsHiddenNow() {
        return HudCaches.hiddenNow();
    }

    public static void invalidateWidgets() {
        HudCaches.invalidateWidgets();
    }

    public static int boxWidth(HudElement e) {
        return HudGeometry.boxWidth(e, placement(e));
    }

    public static int boxHeight(HudElement e) {
        return HudGeometry.boxHeight(e, placement(e));
    }

    public static Box box(HudElement e, int sw, int sh, Box out) {
        return HudGeometry.box(e, placement(e), sw, sh, out);
    }

    public static void renderWidgets(Gfx g) {
        if (!editing) HudRenderer.get().renderGame(g);
    }

    public static void draw(Gfx g, HudElement e) {
        if (!begin(g, e)) return;
        try {
            e.render(g, e.width(), e.height(), false);
        } finally {
            end(g);
        }
    }

    public static boolean begin(Gfx g, HudElement e) {
        return !editing && PlainDraw.begin(g, e);
    }

    public static void end(Gfx g) {
        PlainDraw.end(g);
    }

    public static void openEditor(Screen parent) {
        ClientCompat.openScreen(new HudEditorScreen(parent));
    }

    public static void openEditor(Screen parent, String profile, Runnable onClose) {
        String before = profile();
        useProfile(profile);
        ClientCompat.openScreen(new HudEditorScreen(parent, () -> {
            useProfile(before);
            if (onClose != null) onClose.run();
        }));
    }

    public static void registerProfile(String id, java.util.Set<String> shownByDefault) {
        HudProfiles.register(id, shownByDefault);
    }

    public static String profile() {
        return HudProfiles.active().id();
    }

    public static boolean useProfile(String id) {
        return HudProfiles.use(id);
    }

    public static void moveTo(HudElement e, float x, float y, int sw, int sh) {
        HudGeometry.moveTo(e, x, y, sw, sh);
    }

    public static void scaleTo(HudElement e, float scale, int sw, int sh) {
        HudGeometry.scaleTo(e, scale, sw, sh);
    }

    public static float clampScale(HudElement e, float s) {
        return HudGeometry.clampScale(e, s);
    }

    public static void setSize(HudElement e, int w, int h) {
        HudGeometry.setSize(e, w, h);
    }

    public static void setAnchor(HudElement e, Anchor a, int sw, int sh) {
        HudGeometry.setAnchor(e, a, sw, sh);
    }

    public static void setHidden(HudElement e, boolean hidden) {
        HudEdits.setHidden(e, hidden);
    }

    public static void setOption(HudElement e, String key, String value) {
        HudEdits.setOption(e, key, value);
    }

    public static void reset(HudElement e) {
        HudEdits.reset(e);
    }

    public static void resetAll() {
        HudEdits.resetAll();
    }

    public static HudElement duplicate(HudElement e, int sw, int sh) {
        return HudEdits.duplicate(e, sw, sh);
    }

    public static void removeCopy(HudElement e) {
        HudEdits.removeCopy(e);
    }

    public static void markCustom(HudElement e) {
        placement(e).custom = true;
    }

    public static State capture() {
        return HudStates.capture();
    }

    public static void restore(State s) {
        HudStates.apply(s, false);
    }

    public static void apply(State s, boolean hideUnlisted) {
        HudStates.apply(s, hideUnlisted);
    }

    public static HudStateBuilder state() {
        return new HudStateBuilder();
    }

    public static boolean flag(String key) {
        return HudFile.flag(key);
    }

    public static void setFlag(String key, boolean on) {
        HudFile.setFlag(key, on);
    }

    public static boolean fresh() {
        return HudFile.fresh();
    }

    public static void save() {
        HudFile.save();
    }

    public static java.nio.file.Path file() {
        return HudFile.file();
    }
}
