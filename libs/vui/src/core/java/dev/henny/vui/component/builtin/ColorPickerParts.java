package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Controls;
import dev.henny.vui.component.Internal;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Handler;
import dev.henny.vui.expr.Values;
import dev.henny.vui.style.StateFlags;

import java.util.ArrayList;
import java.util.List;

final class ColorPickerParts {
    static final String PANEL = "colorpicker-panel";
    static final String AREA = "colorpicker-area";
    static final String SWATCHES = "colorpicker-swatches";
    static final String SWATCH = "colorpicker-swatch";

    private ColorPickerParts() {
    }

    static void build(Element picker) {
        Element panel = Internal.child(picker, PANEL);
        Internal.child(panel, AREA);
        Element hex = Internal.child(panel, "textfield");
        hex.classes().addFixed("colorpicker-hex");
        hex.prop("max-length", 9.0);
        hex.addHandler(new Handler("change", List.of(), ev -> commitHex(picker, hex)));
        hex.addHandler(new Handler("submit", List.of(), ev -> commitHex(picker, hex)));
        swatches(picker);
        sync(picker);
    }

    static void swatches(Element picker) {
        Element panel = Internal.find(picker, PANEL);
        if (panel == null) return;
        Internal.clear(panel, SWATCHES);
        List<Object> colors = presets(picker);
        if (colors.isEmpty()) return;
        Element row = Internal.create(panel, SWATCHES);
        panel.sources().add(1, new dev.henny.vui.dom.StaticChild(row));
        panel.rebuildChildren();
        for (Object c : colors) {
            Element s = Internal.child(row, SWATCH);
            s.prop("value", c);
        }
    }

    static void sync(Element picker) {
        Element panel = Internal.find(picker, PANEL);
        if (panel == null) return;
        Object value = Controls.value(picker);
        int argb = ColorValue.parse(value);
        Element hex = Internal.find(panel, "textfield");
        if (hex != null) {
            hex.flags().shown = picker.prop("hex") == null || Values.truthy(picker.prop("hex"));
            if (!hex.has(StateFlags.FOCUS)) hex.prop("value", ColorValue.format(argb, ""));
            hex.state(StateFlags.INVALID, false);
        }
        Element row = Internal.find(panel, SWATCHES);
        if (row == null) return;
        for (int i = 0; i < row.childCount(); i++) {
            Element s = row.child(i);
            s.state(StateFlags.SELECTED, ColorValue.parse(s.prop("value")) == argb);
        }
    }

    static void resetArea(Element picker) {
        Element panel = Internal.find(picker, PANEL);
        Element area = panel == null ? null : Internal.find(panel, AREA);
        if (area != null) ColorAreaBehavior.reset(area, ColorValue.parse(Controls.value(picker)));
    }

    static Element picker(Element part) {
        for (Element p = part; p != null; p = p.parent()) if (p.tag().equals("colorpicker")) return p;
        return null;
    }

    static void choose(Element picker, Object color) {
        Controls.commit(picker, ColorValue.format(ColorValue.parse(color), Controls.value(picker)));
        resetArea(picker);
        sync(picker);
    }

    private static void commitHex(Element picker, Element hex) {
        String text = Values.display(hex.prop("value")).trim();
        if (!text.startsWith("#")) text = "#" + text;
        if (!ColorValue.valid(text)) {
            hex.state(StateFlags.INVALID, true);
            return;
        }
        choose(picker, text);
    }

    private static List<Object> presets(Element picker) {
        Object raw = Values.unwrap(picker.prop("presets") != null ? picker.prop("presets") : picker.prop("swatches"));
        List<Object> out = new ArrayList<>();
        if (raw instanceof Iterable<?> list) for (Object o : list) out.add(Values.unwrap(o));
        else if (raw instanceof CharSequence s) for (String part : s.toString().split(",")) if (!part.isBlank()) out.add(part.trim());
        return out;
    }
}
