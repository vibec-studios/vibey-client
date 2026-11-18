package dev.henny.vibeyclient;

import java.util.List;
import java.util.Map;

/**
 * Standalone mods that replace a module (SPEC §2). Plain Java on purpose: the mixin config plugin reads it before
 * the game (and any module class) is loaded. A module's {@code conflictingMods()} returns {@link #of(String)}.
 */
public final class ModConflicts {
    private ModConflicts() {}

    private static final Map<String, List<String>> BY_MODULE = Map.of(
            "multiplayer", List.of("vibey"),
            "lyrics", List.of("vibeylyrics"));

    public static List<String> of(String moduleId) {
        return BY_MODULE.getOrDefault(moduleId, List.of());
    }
}
