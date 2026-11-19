package dev.henny.vibeyclient.client.ranks;

import dev.henny.vibeyclient.ranks.NameStyleDef;
import dev.henny.vibeyclient.ranks.OwnRanks;
import dev.henny.vibeyclient.ranks.RankDef;
import dev.henny.vibeyclient.ranks.RanksDoc;

import java.util.concurrent.atomic.AtomicLong;

public final class Ranks {
    private Ranks() {}

    private static volatile RanksDoc doc = RanksDoc.EMPTY;
    private static volatile OwnRanks own = OwnRanks.NONE;
    private static final AtomicLong VERSION = new AtomicLong();

    public static RanksDoc doc() {
        return doc;
    }

    public static OwnRanks own() {
        return own;
    }

    public static RankDef rank(String id) {
        return doc.rank(id);
    }

    public static NameStyleDef style(String id) {
        return doc.style(id);
    }

    public static long version() {
        return VERSION.get();
    }

    public static void setDoc(RanksDoc next) {
        if (next == null || next.equals(doc)) return;
        doc = next;
        VERSION.incrementAndGet();
    }

    public static void setOwn(OwnRanks next) {
        if (next == null || next.equals(own)) return;
        own = next;
        VERSION.incrementAndGet();
    }
}
