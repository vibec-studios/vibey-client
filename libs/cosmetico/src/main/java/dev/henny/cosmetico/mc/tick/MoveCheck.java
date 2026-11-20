package dev.henny.cosmetico.mc.tick;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

final class MoveCheck {
    private MoveCheck() {}

    static boolean moving(Minecraft mc, double startX, double startZ) {
        Options o = mc.options;
        //? if >=26.2 {
        /*if (mc.gui.screen() != null) return false;
        *///?} else
        if (mc.screen != null) return false;
        if (o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown() || o.keyJump.isDown()) return true;
        double dx = mc.player.getX() - startX, dz = mc.player.getZ() - startZ;
        return dx * dx + dz * dz > 0.25;
    }
}
