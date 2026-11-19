package dev.henny.vibeyclient.module.nametags;

import dev.henny.vibeyclient.client.ranks.Ranks;
import dev.henny.vibeyclient.ranks.NameStyleDef;
import dev.henny.vibeyclient.ranks.OwnRanks;
import dev.henny.vibeyclient.ranks.RankDef;
import dev.henny.vibeyclient.ranks.RanksDoc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class MyStyles {
    private MyStyles() {}

    static List<NameStyleDef> allowed(OwnRanks own) {
        List<NameStyleDef> out = new ArrayList<>();
        for (NameStyleDef s : Ranks.doc().styles()) if (own.styles().contains(s.id())) out.add(s);
        return out;
    }

    static List<RankDef> primaryRanks(OwnRanks own) {
        RanksDoc doc = Ranks.doc();
        List<RankDef> out = new ArrayList<>();
        for (String id : own.held()) {
            RankDef r = doc.rank(id);
            if (r != null && r.canBePrimary()) out.add(r);
        }
        out.sort(Comparator.comparingInt(RankDef::priority).reversed());
        return out;
    }

    static String shownRank(OwnRanks own) {
        if (own.primary() != null) return own.primary();
        List<RankDef> ranks = primaryRanks(own);
        return ranks.isEmpty() ? null : ranks.get(0).id();
    }
}
