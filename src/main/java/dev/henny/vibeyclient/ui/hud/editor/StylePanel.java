package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudOption;
import dev.henny.vibeyclient.ui.hud.HudStyle;

import java.util.Locale;

final class StylePanel {
    private final EditorController c;
    private final HudStyle.Resolved scratch = new HudStyle.Resolved();
    private boolean global;

    StylePanel(EditorController c) {
        this.c = c;
        c.st.value("scope", "one").value("tab", "box");
    }

    void scope(String scope) {
        global = "all".equals(scope);
        c.st.value("scope", global ? "all" : "one");
    }

    void sync() {
        HudElement p = c.session.selection.primary();
        boolean styleable = global || anyStyleable();
        c.st.value("styleable", styleable);
        c.st.value("oneLabel", c.session.selection.size() > 1 ? Lang.tr("ui.hud.count", c.session.selection.size()) : Lang.tr("ui.hud.thisWidget"));
        if (p == null) return;
        HudStyle.Resolved r = global ? HudStyle.resolve(HudLayout.globalStyle(), null, scratch) : HudLayout.style(p);
        HudStyle own = global ? null : HudLayout.placement(p).style;
        StyleValues.fill(c.st, r, own);
        HudLayout.Placement pl = HudLayout.placement(p);
        c.st.value("scale", pl.scale).value("minScale", p.minScale()).value("maxScale", p.maxScale());
        c.st.value("hasVariants", p.hasVariants()).value("autoAnchor", pl.autoAnchor);
        StyleLists.anchors(c.anchors, pl.anchor);
        StyleLists.options(c.opts, c.session.selection.size() == 1 ? p : null);
    }

    private boolean anyStyleable() {
        for (HudElement e : c.session.selection.items()) if (e.styleable()) return true;
        return false;
    }

    void set(String key, Object value) {
        c.session.history.checkpoint("style." + key);
        edit(o -> StyleValues.write(o, key, value));
    }

    void inherit(String key) {
        if (key == null || key.isEmpty() || global) return;
        if ("anchor".equals(key)) {
            anchor("auto");
            return;
        }
        c.session.history.checkpoint(null);
        edit(o -> StyleValues.write(o, key, null));
    }

    private void edit(java.util.function.Consumer<HudStyle> change) {
        if (global) {
            change.accept(HudLayout.globalStyle());
        } else {
            for (HudElement e : c.session.selection.items()) {
                if (!e.styleable()) continue;
                change.accept(HudLayout.placement(e).style);
                HudLayout.markCustom(e);
            }
        }
        HudLayout.styleChanged();
    }

    void anchor(String id) {
        c.session.history.checkpoint(null);
        Anchor a = "auto".equals(id) ? null : Anchor.valueOf(id.toUpperCase(Locale.ROOT));
        for (HudElement e : c.session.selection.items()) HudLayout.setAnchor(e, a, c.session.width(), c.session.height());
    }

    void option(Items.Option o, String value) {
        HudElement p = c.session.selection.primary();
        if (p == null) return;
        c.session.history.checkpoint(null);
        HudLayout.setOption(p, o.key(), value);
    }

    void step(Items.Option o, int dir) {
        HudElement p = c.session.selection.primary();
        if (p == null) return;
        HudOption opt = o.option();
        String v = HudLayout.placement(p).opts.getOrDefault(opt.key, opt.def);
        int i = opt.values.indexOf(v);
        option(o, opt.values.get(i < 0 ? 0 : Math.floorMod(i + dir, opt.values.size())));
    }

    void capture(Items.Option o) {
        String v = o.option().captureValue();
        if (v != null) option(o, v);
    }
}
