package dev.henny.vibeyclient.client.branding;

import dev.henny.vibeyclient.Vibey;

final class VanillaBrandLines {
    private VanillaBrandLines() {}

    static int count() {
        return loaderLines() + (Vibey.platform().isModLoaded("modmenu") ? 1 : 0);
    }

    private static int loaderLines() {
        //? if neoforge {
        /*int[] n = {0};
        net.neoforged.neoforge.internal.BrandingControl.forEachLine(true, true, (i, s) -> n[0]++);
        return Math.max(1, n[0]);
        *///?} else
        return 1;
    }
}
