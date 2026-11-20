package dev.henny.vibeyclient.module.cosmetics.host;

import dev.henny.cosmetico.host.Log;
import dev.henny.vibeyclient.Vibey;

final class VibeyLog implements Log {
    @Override
    public void info(String message) {
        Vibey.LOG.info(message);
    }

    @Override
    public void warn(String message) {
        Vibey.LOG.warn(message);
    }

    @Override
    public void error(String message, Throwable error) {
        Vibey.LOG.error(message, error);
    }

    @Override
    public void debug(String message) {
        Vibey.LOG.debug(message);
    }
}
