package dev.henny.cosmetico.golden;

import dev.henny.cosmetico.Engine;
import dev.henny.cosmetico.anim.Clock;
import dev.henny.cosmetico.host.CosmeticoHost;
import dev.henny.cosmetico.host.Log;

public final class TestHost implements CosmeticoHost {
    @Override
    public String language() {
        return "en";
    }

    @Override
    public String contentVersion() {
        return GoldenVersion.value();
    }

    @Override
    public Log log() {
        return Log.NONE;
    }

    public static Engine engine() {
        TestHost h = new TestHost();
        return new Engine(() -> h, Clock.system());
    }
}
