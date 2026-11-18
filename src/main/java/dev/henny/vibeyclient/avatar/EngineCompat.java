package dev.henny.vibeyclient.avatar;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.UUID;

/** Small client calls of the engine whose shape differs between versions. */
public final class EngineCompat {
    private EngineCompat() {}

    /** The account's UUID (also on the title screen). */
    public static UUID selfId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) return mc.player.getUUID();
        return mc.getUser().getProfileId();
    }

    public static String selfName() {
        return Minecraft.getInstance().getUser().getName();
    }

    /** Physical state of a key mapping's key (also while a screen is open). */
    public static boolean isPhysicallyDown(KeyMapping mapping) {
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

    public static CameraType camera() {
        return Minecraft.getInstance().options.getCameraType();
    }

    public static void setCamera(CameraType type) {
        Minecraft.getInstance().options.setCameraType(type);
    }

    public static boolean onRenderThread() {
        return Minecraft.getInstance().isSameThread();
    }
}
