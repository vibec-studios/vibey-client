package dev.henny.vibeyclient.module.screens;

import dev.henny.vibeyclient.api.setting.OptionSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.screens.apply.Guard;
import dev.henny.vibeyclient.module.screens.core.ScreenLayout;
import dev.henny.vibeyclient.module.screens.core.ScreenNames;
import dev.henny.vibeyclient.module.screens.edit.EditMode;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

final class ScreenPages {
    private static String picked = "";

    private ScreenPages() {}

    static OptionSetting picker() {
        return new OptionSetting("screen", ScreenPages::current, v -> picked = v, ScreenPages::choices)
                .section("edited").visibleWhen(ScreenPages::any);
    }

    static boolean any() {
        return !Screens.store().names().isEmpty();
    }

    private static String current() {
        List<String> names = Screens.store().names();
        if (names.isEmpty()) return "";
        return names.contains(picked) ? picked : names.get(0);
    }

    private static List<OptionSetting.Choice> choices() {
        List<OptionSetting.Choice> out = new ArrayList<>();
        for (String n : Screens.store().names()) out.add(new OptionSetting.Choice(n, label(n)));
        return out;
    }

    static String label(String name) {
        ScreenLayout l = Screens.store().get(name);
        return l.label() != null ? l.label() : ScreenNames.readable(name);
    }

    static void resetPicked() {
        String n = current();
        if (!n.isEmpty()) Screens.store().remove(n);
        picked = "";
        refreshCurrent();
    }

    static void resetAll() {
        Screens.store().clear();
        Guard.clear();
        refreshCurrent();
    }

    static void tryOnOptions() {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.screen;
        Screen options = ClientCompat.optionsScreen(parent);
        mc.setScreen(options);
        EditMode.enter(options);
    }

    static void refreshCurrent() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) return;
        Window w = mc.getWindow();
        try {
            mc.screen.resize(w.getGuiScaledWidth(), w.getGuiScaledHeight());
        } catch (RuntimeException e) {
            dev.henny.vibeyclient.Vibey.LOG.warn("Screen refresh failed", e);
        }
    }

}
