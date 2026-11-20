package dev.henny.vibeyclient.module.clips.shots;

import dev.henny.r3.mc.client.R3Client;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.client.ClientCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class Shots {
    private static GalleryController controller;
    private static ShotsScreen screen;

    private Shots() {}

    public static GalleryController controller() {
        if (controller == null) controller = new GalleryController();
        return controller;
    }

    public static ShotsScreen openScreen() {
        Screen parent = ClientCompat.screen();
        ShotsScreen s = new ShotsScreen(controller());
        s.parent(parent instanceof ShotsScreen ? null : parent).pause(false);
        screen = s;
        s.open();
        return s;
    }

    public static ShotsScreen screen() {
        return screen;
    }

    public static void closeScreen() {
        ShotsScreen s = screen;
        if (s != null && Minecraft.getInstance().screen == s) s.onClose();
    }

    public static WorldObject placePanel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !R3Client.started()) return null;
        WorldObject panel = ShotsPanel.place(controller());
        closeScreen();
        return panel;
    }

    public static WorldObject panel() {
        return ShotsPanel.object();
    }

    public static void removePanel() {
        ShotsPanel.remove();
        releaseIfUnused();
    }

    public static void tick() {
        if (controller == null) return;
        if (!ShotsPanel.alive()) ShotsPanel.forget();
        if (ShotsPanel.alive()) controller.poll();
        releaseIfUnused();
    }

    static void screenClosed(ShotsScreen s) {
        if (screen == s) screen = null;
        releaseIfUnused();
    }

    private static void releaseIfUnused() {
        if (controller == null || screen != null || ShotsPanel.alive()) return;
        controller.dispose();
        controller = null;
    }
}
