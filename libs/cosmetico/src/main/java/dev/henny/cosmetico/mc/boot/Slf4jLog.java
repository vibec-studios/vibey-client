package dev.henny.cosmetico.mc.boot;

import dev.henny.cosmetico.host.Log;
import dev.henny.cosmetico.mc.CosmeticoMod;

public final class Slf4jLog implements Log {
    @Override
    public void info(String message) {
        CosmeticoMod.LOG.info(message);
    }

    @Override
    public void warn(String message) {
        CosmeticoMod.LOG.warn(message);
    }

    @Override
    public void error(String message, Throwable error) {
        CosmeticoMod.LOG.error(message, error);
    }

    @Override
    public void debug(String message) {
        CosmeticoMod.LOG.debug(message);
    }
}
