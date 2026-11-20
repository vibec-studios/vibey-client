package dev.henny.vui.mc.compat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

public final class Mc {
    private Mc() {
    }

    public static Minecraft get() {
        return Minecraft.getInstance();
    }

    public static boolean ready() {
        return Minecraft.getInstance() != null && Minecraft.getInstance().font != null;
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static Window window() {
        return Minecraft.getInstance().getWindow();
    }

    public static int guiWidth() {
        return window().getGuiScaledWidth();
    }

    public static int guiHeight() {
        return window().getGuiScaledHeight();
    }

    public static int guiScale() {
        return Math.max(1, (int) Math.round((double) window().getGuiScale()));
    }

    public static Screen screen() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.screen();
        *///?} else
        return Minecraft.getInstance().screen;
    }

    public static void setScreen(Screen screen) {
        //? if >=26.2 {
        /*Minecraft.getInstance().setScreenAndShow(screen);
        *///?} else
        Minecraft.getInstance().setScreen(screen);
    }

    public static boolean hudHidden() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.hud.isHidden();
        *///?} else
        return Minecraft.getInstance().options.hideGui;
    }

    public static boolean loadingOverlay() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.overlay() != null;
        *///?} else
        return Minecraft.getInstance().getOverlay() != null;
    }

    public static boolean inWorld() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.player != null;
    }

    public static RenderTarget mainRenderTarget() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gameRenderer.mainRenderTarget();
        *///?} else
        return Minecraft.getInstance().getMainRenderTarget();
    }

    public static java.nio.file.Path gameDirectory() {
        return Minecraft.getInstance().gameDirectory.toPath();
    }

    public static int fps() {
        return Minecraft.getInstance().getFps();
    }

    public static void later(Runnable task) {
        Minecraft.getInstance().execute(task);
    }
}
