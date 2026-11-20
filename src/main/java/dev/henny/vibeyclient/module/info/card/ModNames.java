package dev.henny.vibeyclient.module.info.card;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.info.core.ModLabel;

import java.util.HashMap;
import java.util.Map;

public final class ModNames {
    private static final Map<String, String> CACHE = new HashMap<>();

    private ModNames() {}

    public static String of(String namespace) {
        return CACHE.computeIfAbsent(namespace, ns -> ModLabel.of(ns, ModNames::installed));
    }

    private static String installed(String namespace) {
        try {
            for (var info : Vibey.platform().mods()) if (info.id().equals(namespace)) return info.name();
        } catch (RuntimeException e) {
            return null;
        }
        return null;
    }
}
