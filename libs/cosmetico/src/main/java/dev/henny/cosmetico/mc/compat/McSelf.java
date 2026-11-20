package dev.henny.cosmetico.mc.compat;

import net.minecraft.client.Minecraft;

import java.util.UUID;

public final class McSelf {
    private McSelf() {}

    public static UUID id() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) return mc.player.getUUID();
        return mc.getUser().getProfileId();
    }

    public static String name() {
        return Minecraft.getInstance().getUser().getName();
    }

    public static boolean onRenderThread() {
        return Minecraft.getInstance().isSameThread();
    }
}
