package dev.henny.vui.component;

import dev.henny.vui.component.builtin.ButtonBehavior;
import dev.henny.vui.component.builtin.CheckboxBehavior;
import dev.henny.vui.component.builtin.ColorPanelBehavior;
import dev.henny.vui.component.builtin.ColorPickerBehavior;
import dev.henny.vui.component.builtin.IconBehavior;
import dev.henny.vui.component.builtin.ImageBehavior;
import dev.henny.vui.component.builtin.KeybindBehavior;
import dev.henny.vui.component.builtin.ListBehavior;
import dev.henny.vui.component.builtin.ModalBehavior;
import dev.henny.vui.component.builtin.OptionGroupBehavior;
import dev.henny.vui.component.builtin.OverlayBehavior;
import dev.henny.vui.component.builtin.PopoverBehavior;
import dev.henny.vui.component.builtin.ProgressBehavior;
import dev.henny.vui.component.builtin.SelectBehavior;
import dev.henny.vui.component.builtin.SliderBehavior;
import dev.henny.vui.component.builtin.SpinnerBehavior;
import dev.henny.vui.component.builtin.TextFieldBehavior;
import dev.henny.vui.component.builtin.ToggleBehavior;
import dev.henny.vui.component.builtin.TooltipBehavior;
import dev.henny.vui.dom.Element;
import dev.henny.vui.template.TagCatalog;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class Behaviors implements TagCatalog {
    private static final Set<String> PLAIN_TAGS = Set.of("root", "box", "row", "column", "stack", "grid", "scroll",
            "text", "span", "b", "i", "divider", "spacer", "toast", "label", "card", "segment", "tab", "select-option",
            Element.TEXT_TAG);

    private final Map<String, Behavior> byTag = new LinkedHashMap<>();

    public static Behaviors standard() {
        Behaviors b = new Behaviors();
        for (String tag : PLAIN_TAGS) b.register(tag, PlainBehavior.INSTANCE);
        b.register("button", new ButtonBehavior());
        b.register("icon", new IconBehavior());
        b.register("image", new ImageBehavior());
        b.register("toggle", new ToggleBehavior());
        b.register("checkbox", new CheckboxBehavior());
        b.register("slider", new SliderBehavior());
        b.register("textfield", new TextFieldBehavior());
        b.register("select", new SelectBehavior());
        b.register("select-menu", new OverlayBehavior("bottom", true));
        b.register("segmented", new OptionGroupBehavior("segment"));
        b.register("tabs", new OptionGroupBehavior("tab"));
        b.register("list", new ListBehavior());
        b.register("tooltip", new TooltipBehavior());
        b.register("tooltip-bubble", new TooltipBehavior());
        b.register("popover", new PopoverBehavior());
        b.register("modal", new ModalBehavior());
        b.register("keybind", new KeybindBehavior());
        b.register("colorpicker", new ColorPickerBehavior());
        b.register("colorpicker-panel", new ColorPanelBehavior());
        b.register("progress", new ProgressBehavior());
        b.register("spinner", new SpinnerBehavior());
        return b;
    }

    public Behaviors register(String tag, Behavior behavior) {
        byTag.put(tag, behavior);
        return this;
    }

    public Behavior get(String tag) {
        Behavior b = byTag.get(tag);
        return b == null ? PlainBehavior.INSTANCE : b;
    }

    @Override
    public boolean isBuiltIn(String tag) {
        return byTag.containsKey(tag);
    }

    @Override
    public Collection<String> builtInTags() {
        return Collections.unmodifiableSet(byTag.keySet());
    }
}
