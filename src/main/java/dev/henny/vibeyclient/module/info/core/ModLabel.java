package dev.henny.vibeyclient.module.info.core;

import java.util.function.Function;

public final class ModLabel {
    private ModLabel() {}

    public static String of(String namespace, Function<String, String> installedName) {
        if (namespace == null || namespace.isEmpty()) return "";
        if ("minecraft".equals(namespace)) return "Minecraft";
        String name = installedName.apply(namespace);
        if (name != null && !name.isBlank()) return name;
        return Character.toUpperCase(namespace.charAt(0)) + namespace.substring(1).replace('_', ' ');
    }
}
