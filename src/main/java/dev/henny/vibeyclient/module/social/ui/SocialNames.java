package dev.henny.vibeyclient.module.social.ui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.names.IconFont;
import dev.henny.vibeyclient.client.names.NameLine;
import dev.henny.vibeyclient.client.names.RankGlyphs;
import dev.henny.vibeyclient.client.ranks.Ranks;
import dev.henny.vibeyclient.module.social.model.Model.User;
import dev.henny.vibeyclient.ranks.Identity;
import dev.henny.vibeyclient.ranks.RankDef;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;

final class SocialNames {
    private SocialNames() {}

    static void draw(Gfx g, User u, int x, int y, int maxWidth, int color) {
        Identity id = u.identity();
        if (id == null || id.isEmpty()) {
            g.text(g.clip(u.name(), maxWidth), x, y, color);
            return;
        }
        g.text(NameLine.of(id, u.name(), color, true), x, y, color);
    }

    static boolean rankChip(Gfx g, User u, int x, int y) {
        Identity id = u.identity();
        RankDef def = id == null ? null : Ranks.rank(id.rank());
        if (def == null) return false;
        String label = def.label().get(Lang.german());
        int w = g.width(label) + 8 + 10;
        g.rect(x, y, w, 12, Theme.alpha(0xFF000000 | def.color(), 0.14f));
        g.outline(x, y, w, 12, Theme.alpha(0xFF000000 | def.color(), 0.5f));
        RankGlyphs.Glyph gl = RankGlyphs.of(def.id(), Ranks.doc());
        if (gl != null) g.text(IconFont.glyph(gl.c(), gl.color()), x + 3, y + 2, 0xFFFFFFFF);
        g.text(label, x + 13, y + 2, 0xFF000000 | def.color());
        return true;
    }
}
