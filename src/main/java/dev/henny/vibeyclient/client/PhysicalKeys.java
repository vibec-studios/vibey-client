package dev.henny.vibeyclient.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class PhysicalKeys {
    private PhysicalKeys() {}

    public static boolean isDown(KeyMapping mapping) {
        if (mapping == null || mapping.isUnbound()) return false;
        InputConstants.Key key = InputConstants.getKey(mapping.saveString());
        //? if >=26.3 {
        /*boolean keyboard = key.getType() == InputConstants.Type.KEYBOARD;
        *///?} else
        boolean keyboard = key.getType() == InputConstants.Type.KEYSYM;
        if (!keyboard) return mapping.isDown();
        Minecraft mc = Minecraft.getInstance();
        //? if >=26.3 {
        /*return InputConstants.isKeyDown(key.getValue());
        *///?} else if >=1.21.9 {
        return InputConstants.isKeyDown(mc.getWindow(), key.getValue());
        //?} else
        /*return InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());*/
    }
}
