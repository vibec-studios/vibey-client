package dev.henny.vibeyclient.module.cosmetics.selftest;

import net.minecraft.client.Minecraft;

final class SelfTestShots {
    private SelfTestShots() {}

    static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        try {
            //? if >=26.2 {
            /*net.minecraft.client.Screenshot.grab(mc, false);
            *///?} else if >=1.21.6 {
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1, c -> SelfTestFlags.log("screenshot " + c.getString()));
            //?} else
            /*net.minecraft.client.Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> SelfTestFlags.log("screenshot " + c.getString()));*/
            SelfTestFlags.log("shot " + name);
        } catch (RuntimeException e) {
            SelfTestFlags.log("screenshot failed: " + e);
        }
    }
}
