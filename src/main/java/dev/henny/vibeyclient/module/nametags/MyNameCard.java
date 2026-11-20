package dev.henny.vibeyclient.module.nametags;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.client.names.IconFont;
import dev.henny.vibeyclient.client.names.NameLine;
import dev.henny.vibeyclient.client.names.RankGlyphs;
import dev.henny.vibeyclient.client.names.StyleClock;
import dev.henny.vibeyclient.client.ranks.MyRanksApi;
import dev.henny.vibeyclient.client.ranks.Ranks;
import dev.henny.vibeyclient.ranks.Identity;
import dev.henny.vibeyclient.ranks.NameStyleDef;
import dev.henny.vibeyclient.ranks.OwnRanks;
import dev.henny.vibeyclient.ranks.RankDef;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.Ui;
import dev.henny.vibeyclient.ui.Widgets;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class MyNameCard {
    private static final Object SHOW = new Object(), NONE = new Object(), AUTO = new Object();
    private static final int ROW = 24;

    private final NametagsModule module;
    private volatile boolean busy;

    MyNameCard(NametagsModule module) {
        this.module = module;
    }

    int draw(Ui ui, int x, int y, int w) {
        if (!MyRanksApi.available()) return y;
        OwnRanks own = Ranks.own();
        List<Chips.Chip> styles = styleChips(own);
        List<RankDef> held = MyStyles.primaryRanks(own);
        List<Chips.Chip> ranks = held.size() >= 2 ? rankChips(own, held) : List.of();
        int inner = w - 20;
        int sh = Chips.height(ui.g, styles, inner), rh = Chips.height(ui.g, ranks, inner);
        Widgets.caption(ui.g, module.tr("section.mine"), x + 1, y);
        y += 12;
        Widgets.card(ui.g, x, y, w, ROW + sh + rh);
        preview(ui, own, x, y, w);
        Chips.Chip s = Chips.clicked(ui, styles, x + 10, y + ROW - 4, x + w - 10, !busy);
        if (s != null) run(MyRanksApi.setStyle(s.key() == NONE ? null : (String) s.key()));
        Chips.Chip r = Chips.clicked(ui, ranks, x + 10, y + ROW - 4 + sh, x + w - 10, !busy);
        if (r != null) run(MyRanksApi.setPrimary(r.key() == AUTO ? null : (String) r.key()));
        return y + ROW + sh + rh + 10;
    }

    private void preview(Ui ui, OwnRanks own, int x, int y, int w) {
        Gfx g = ui.g;
        Identity me = new Identity(own.showRank() ? MyStyles.shownRank(own) : null, List.of(), own.style());
        g.text(NameLine.of(me, Minecraft.getInstance().getUser().getName(), Theme.TEXT, true), x + 10, y + 8, Theme.TEXT);
        int tx = x + w - 10 - Widgets.TOGGLE_W;
        g.textRight(module.tr("mine.showRank"), tx - 6, y + 8, Theme.MUTED);
        if (Widgets.toggle(ui, SHOW, tx, y + (ROW - Widgets.TOGGLE_H) / 2, own.showRank(), !busy)) {
            run(MyRanksApi.setShowRank(!own.showRank()));
        }
    }

    private List<Chips.Chip> styleChips(OwnRanks own) {
        List<Chips.Chip> out = new ArrayList<>();
        out.add(new Chips.Chip(NONE, Component.literal(module.tr("mine.noStyle")), own.style() == null));
        double t = StyleClock.seconds(StyleClock.bucket());
        for (NameStyleDef s : MyStyles.allowed(own)) {
            out.add(new Chips.Chip(s.id(), NameLine.styled(s, s.label().get(Lang.german()), t), s.id().equals(own.style())));
        }
        return out.size() == 1 ? List.of() : out;
    }

    private List<Chips.Chip> rankChips(OwnRanks own, List<RankDef> held) {
        List<Chips.Chip> out = new ArrayList<>();
        out.add(new Chips.Chip(AUTO, Component.literal(module.tr("mine.autoRank")), own.primary() == null));
        for (RankDef r : held) out.add(new Chips.Chip(r.id(), rankLabel(r), r.id().equals(own.primary())));
        return out;
    }

    private static Component rankLabel(RankDef r) {
        MutableComponent label = Component.empty();
        RankGlyphs.Glyph gl = RankGlyphs.of(r.id(), Ranks.doc());
        if (gl != null) label.append(IconFont.glyph(gl.c(), gl.color())).append(" ");
        return label.append(Component.literal(r.label().get(Lang.german())).withColor(r.color()));
    }

    private void run(CompletableFuture<Boolean> f) {
        busy = true;
        f.whenComplete((ok, e) -> busy = false);
    }
}
