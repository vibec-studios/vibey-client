package dev.henny.vibeyclient.client.names;

import dev.henny.vibeyclient.ranks.RankDef;
import dev.henny.vibeyclient.ranks.RanksDoc;

import java.util.Map;

public final class RankGlyphs {
    private RankGlyphs() {}

    public static final char SEAL = '';
    public static final char GENERIC = '';

    public record Glyph(char c, int color) {}

    private static final Map<String, Character> BY_ICON = Map.ofEntries(
            Map.entry("staff", ''), Map.entry("heart", ''), Map.entry("pencil", ''),
            Map.entry("shield-crown", ''), Map.entry("shield", ''), Map.entry("shield-hammer", ''),
            Map.entry("hammer", ''), Map.entry("shield-code", ''), Map.entry("code", ''),
            Map.entry("star", ''), Map.entry("flask", ''), Map.entry("crown", ''),
            Map.entry("check", ''), Map.entry("gem", ''), Map.entry("flame", ''));

    private static final Map<String, String> SYSTEM_RANKS = Map.of(
            "owner", "shield-crown", "admin", "shield", "mod", "shield-hammer", "dev", "shield-code", "partner", "star",
            "creator", "pencil", "supporter", "crown", "beta", "flask", "staff", "staff");

    public static Glyph of(String rankId, RanksDoc doc) {
        if (rankId == null) return null;
        RankDef def = doc.rank(rankId);
        String icon = SYSTEM_RANKS.getOrDefault(rankId, def == null ? null : def.icon());
        Character c = icon == null ? null : BY_ICON.get(icon);
        if (c != null) return new Glyph(c, 0xFFFFFF);
        return def == null ? null : new Glyph(GENERIC, def.color());
    }
}
