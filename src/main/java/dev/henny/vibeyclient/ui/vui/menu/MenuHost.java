package dev.henny.vibeyclient.ui.vui.menu;

import net.minecraft.client.gui.screens.Screen;

public interface MenuHost {
    Screen screen();

    void closeNow();
}
