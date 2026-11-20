package dev.henny.cosmetico.mc.boot;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.Engine;
import dev.henny.cosmetico.mc.fx.VanillaFx;

public final class CosmeticoBoot {
    private CosmeticoBoot() {}

    private static volatile boolean ports;

    public static void ensure() {
        if (!ports) installPorts();
        if (!Cosmetico.active()) Cosmetico.init(new McDefaultHost());
    }

    private static synchronized void installPorts() {
        if (ports) return;
        Engine e = Cosmetico.engine();
        e.resources(McResources.INSTANCE);
        e.backendFx(VanillaFx.INSTANCE);
        ports = true;
    }
}
