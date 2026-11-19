package dev.henny.vibeyclient.client.names;

import dev.henny.vibeyclient.ranks.Identity;
import dev.henny.vibeyclient.ranks.RanksDoc;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class NamePrefix {
    private NamePrefix() {}

    public static MutableComponent build(boolean seal, Identity id, boolean rank, boolean tag, RanksDoc doc) {
        MutableComponent out = Component.empty();
        int n = 0;
        if (seal) {
            out.append(IconFont.glyph(RankGlyphs.SEAL, 0xFFFFFF));
            n++;
        }
        if (rank) n += add(out, RankGlyphs.of(id.rank(), doc));
        if (tag && !id.tags().isEmpty()) n += add(out, RankGlyphs.of(id.tags().get(0), doc));
        return n == 0 ? null : out;
    }

    private static int add(MutableComponent out, RankGlyphs.Glyph g) {
        if (g == null) return 0;
        out.append(IconFont.glyph(g.c(), g.color()));
        return 1;
    }
}
