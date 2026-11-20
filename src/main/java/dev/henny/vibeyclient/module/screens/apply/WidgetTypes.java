package dev.henny.vibeyclient.module.screens.apply;

import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.TabButton;

import java.util.Locale;

public final class WidgetTypes {
    private WidgetTypes() {}

    public static String type(AbstractWidget w) {
        if (w instanceof TabButton) return "tab";
        if (w instanceof CycleButton<?>) return "cycle";
        if (w instanceof AbstractSliderButton) return "slider";
        if (w instanceof EditBox) return "edit";
        if (w instanceof Checkbox) return "checkbox";
        if (w instanceof PlainTextButton) return "link";
        if (w instanceof ImageButton || w instanceof SpriteIconButton) return "icon";
        if (w instanceof Button) return "button";
        if (w instanceof AbstractSelectionList<?>) return "list";
        if (w instanceof AbstractStringWidget) return "text";
        if (w instanceof AbstractContainerWidget) return "area";
        String n = w.getClass().getSimpleName();
        return n.isEmpty() ? "widget" : n.toLowerCase(Locale.ROOT);
    }

    public static boolean scalable(AbstractWidget w) {
        return !(w instanceof AbstractScrollArea);
    }

    public static boolean resizable(AbstractWidget w) {
        return !(w instanceof AbstractStringWidget) && !(w instanceof ImageButton) && !(w instanceof SpriteIconButton)
                && !(w instanceof Checkbox) && w.getWidth() >= 24;
    }

    static boolean container(Object o) {
        return o instanceof TabNavigationBar;
    }
}
