package dev.henny.cosmetico;

import dev.henny.cosmetico.anim.Clock;
import dev.henny.cosmetico.config.CosmeticoConfig;
import dev.henny.cosmetico.dev.DevItems;
import dev.henny.cosmetico.host.CosmeticoHost;
import dev.henny.cosmetico.preview.PreviewView;
import dev.henny.cosmetico.registry.ItemRegistry;
import dev.henny.cosmetico.state.Emotes;
import dev.henny.cosmetico.state.Loadouts;
import dev.henny.cosmetico.version.VersionGate;

import java.util.UUID;

public final class Cosmetico {
    private Cosmetico() {}

    private static volatile CosmeticoHost host = CosmeticoHost.DEFAULT;
    private static volatile boolean active;
    private static final Engine ENGINE = new Engine(() -> host, Clock.system());

    public static void init(CosmeticoHost h) {
        host = h == null ? CosmeticoHost.DEFAULT : h;
        active = true;
    }

    public static boolean active() {
        return active;
    }

    public static CosmeticoHost host() {
        return host;
    }

    public static Engine engine() {
        return ENGINE;
    }

    public static ItemRegistry items() {
        return ENGINE.items;
    }

    public static Loadouts loadouts() {
        return ENGINE.loadouts;
    }

    public static Emotes emotes() {
        return ENGINE.emotes;
    }

    public static CosmeticoConfig config() {
        return ENGINE.config;
    }

    public static PreviewView preview() {
        return ENGINE.preview();
    }

    public static DevItems devItems() {
        return ENGINE.devItems;
    }

    public static UUID self() {
        return ENGINE.config.self;
    }

    public static VersionGate versionGate() {
        return ENGINE.gate;
    }
}
