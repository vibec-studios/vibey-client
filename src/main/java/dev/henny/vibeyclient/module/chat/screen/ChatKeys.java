package dev.henny.vibeyclient.module.chat.screen;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.chat.ChatModule;
import net.minecraft.client.gui.components.EditBox;

public final class ChatKeys {
    public static final int ESCAPE = 256;
    public static final int ENTER = 257;
    public static final int KEYPAD_ENTER = 335;
    public static final int F = 70;
    public static final int CTRL = 2;
    public static final int SUPER = 8;

    private ChatKeys() {}

    public static boolean key(EditBox input, int key, int modifiers) {
        if (!ChatModule.on()) return false;
        boolean ctrl = (modifiers & (CTRL | SUPER)) != 0 || ClientCompat.controlDown();
        if (ctrl && key == F) {
            SearchMode.toggle(input);
            return true;
        }
        if (!SearchMode.active()) return false;
        if (key == ESCAPE) {
            SearchMode.exit();
            return true;
        }
        return key == ENTER || key == KEYPAD_ENTER;
    }
}
