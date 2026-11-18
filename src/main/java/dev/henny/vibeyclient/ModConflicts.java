package dev.henny.vibeyclient;

import java.util.List;
import java.util.Map;

/**
 * Standalone mods that replace a module (SPEC §2). Plain Java on purpose: the mixin config plugin reads it before
 * the game (and any module class) is loaded. A module's {@code conflictingMods()} returns {@link #of(String)}.
 * Keys with a dot name a part of a module ({@code "media.lyrics"}): only that part is switched off.
 */
public final class ModConflicts {
    private ModConflicts() {}

    private static final Map<String, List<String>> BY_MODULE = Map.of(
            "multiplayer", List.of("vibey"),
            // Only the lyrics part of the media module (lyrics, songs, skies); the rest of Media keeps running.
            "media.lyrics", List.of("vibeylyrics"));

    public static List<String> of(String moduleId) {
        return BY_MODULE.getOrDefault(moduleId, List.of());
    }
}
