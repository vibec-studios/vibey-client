package dev.henny.cosmetico.mc.tick;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.mc.boot.CosmeticoBoot;

public final class CosmeticoTick {
    private CosmeticoTick() {}

    private static boolean listening;

    public static void run() {
        CosmeticoBoot.ensure();
        if (!Cosmetico.active()) return;
        if (!listening) {
            listening = true;
            Cosmetico.emotes().listen(LocalEmotes.cameraRestore());
        }
        Cosmetico.engine().main.drain(Cosmetico.host().log());
        McTicker.tick();
        LocalEmotes.tick();
    }
}
