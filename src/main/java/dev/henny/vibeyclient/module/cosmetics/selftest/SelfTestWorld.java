package dev.henny.vibeyclient.module.cosmetics.selftest;

import dev.henny.vibeyclient.client.ClientCompat;
import net.minecraft.client.Minecraft;

final class SelfTestWorld {
    private SelfTestWorld() {}

    static void pressLoad() {
        //? if >=1.21.9 {
        var s = ClientCompat.screen();
        if (!(s instanceof net.minecraft.client.gui.screens.BackupConfirmScreen) && !(s instanceof net.minecraft.client.gui.screens.ConfirmScreen)) return;
        for (var c : s.children()) {
            if (c instanceof net.minecraft.client.gui.components.Button b
                    && (b.getMessage().getString().contains("Know What") || b.getMessage().getString().equals("Yes"))) {
                b.onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
                SelfTestFlags.log("pressed Load on the backup prompt");
                return;
            }
        }
        //?}
    }

    static boolean loading(Minecraft mc) {
        //? if >=26.2 {
        /*return false;
        *///?} else
        return mc.getOverlay() != null;
    }
}
