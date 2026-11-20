package dev.henny.vibeyclient.ranks;

import java.util.List;
import java.util.Map;

public record RanksDoc(String version, Map<String, RankDef> ranks, List<NameStyleDef> styles) {
    public static final RanksDoc EMPTY = new RanksDoc("", Map.of(), List.of());

    public RankDef rank(String id) {
        return id == null ? null : ranks.get(id);
    }

    public NameStyleDef style(String id) {
        if (id == null) return null;
        for (NameStyleDef s : styles) if (s.id().equals(id)) return s;
        return null;
    }
}
