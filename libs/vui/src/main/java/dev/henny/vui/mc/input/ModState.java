package dev.henny.vui.mc.input;

import dev.henny.vui.event.Mods;

public final class ModState {
    private ModState() {
    }

    public static int current() {
        int mods = 0;
        if (shift()) mods |= Mods.SHIFT;
        if (control()) mods |= Mods.CTRL;
        if (alt()) mods |= Mods.ALT;
        return mods;
    }

    public static boolean shift() {
        //? if >=1.21.9 {
        return net.minecraft.client.Minecraft.getInstance().hasShiftDown();
        //?} else
        /*return net.minecraft.client.gui.screens.Screen.hasShiftDown();*/
    }

    public static boolean control() {
        //? if >=1.21.9 {
        return net.minecraft.client.Minecraft.getInstance().hasControlDown();
        //?} else
        /*return net.minecraft.client.gui.screens.Screen.hasControlDown();*/
    }

    public static boolean alt() {
        //? if >=1.21.9 {
        return net.minecraft.client.Minecraft.getInstance().hasAltDown();
        //?} else
        /*return net.minecraft.client.gui.screens.Screen.hasAltDown();*/
    }
}
