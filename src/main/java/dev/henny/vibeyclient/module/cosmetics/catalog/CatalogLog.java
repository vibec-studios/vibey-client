package dev.henny.vibeyclient.module.cosmetics.catalog;

import dev.henny.vibeyclient.Vibey;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class CatalogLog {
    private CatalogLog() {}

    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    static void once(String key, String msg, Object... args) {
        if (LOGGED.size() < 4096 && LOGGED.add(key)) Vibey.LOG.info(msg, args);
    }
}
