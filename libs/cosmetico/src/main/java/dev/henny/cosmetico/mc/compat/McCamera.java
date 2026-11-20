package dev.henny.cosmetico.mc.compat;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public final class McCamera {
    private McCamera() {}

    public static CameraType get() {
        return Minecraft.getInstance().options.getCameraType();
    }

    public static void set(CameraType type) {
        Minecraft.getInstance().options.setCameraType(type);
    }
}
