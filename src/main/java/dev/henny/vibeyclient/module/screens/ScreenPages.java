package dev.henny.vibeyclient.module.screens;

import dev.henny.vibeyclient.api.StatusCard;
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
    static final int LISTED = 5;

    private ScreenPages() {}

    static boolean any() {
        return !Screens.store().names().isEmpty();
    }

    static StatusCard status(ScreensModule m) {
        List<String> names = Screens.store().names();
        if (names.isEmpty()) return null;
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < Math.min(LISTED, names.size()); i++) labels.add(label(names.get(i)));
        String text = String.join(", ", labels);
        if (names.size() > LISTED) text += " " + m.tr("status.more", names.size() - LISTED);
        return new StatusCard(StatusCard.Kind.OK, m.tr("status.label", names.size()), text, m.tr("status.detail"));
    }

    static String label(String name) {
        ScreenLayout l = Screens.store().get(name);
        return l.label() != null ? l.label() : ScreenNames.readable(name);
    }

    static void resetAll() {
        Screens.store().clear();
        Guard.clear();
        refreshCurrent();
    }

    static void tryOnOptions() {
        Minecraft mc = Minecraft.getInstance();
        Screen options = ClientCompat.optionsScreen(mc.screen);
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
