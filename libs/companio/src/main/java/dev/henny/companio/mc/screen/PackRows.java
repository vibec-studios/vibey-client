package dev.henny.companio.mc.screen;

import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.pack.Pack;
import dev.henny.companio.pack.Problem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;

final class PackRows {
    private PackRows() {}

    static String key(Pack p) {
        return p.id() != null ? p.id() : "file:" + p.location();
    }

    static PackRow of(Pack p, boolean active) {
        String lang = Minecraft.getInstance().options.languageCode;
        List<PackRow.CompanionChip> chips = new ArrayList<>();
        for (CompanionDef c : p.companions()) chips.add(new PackRow.CompanionChip(c.key(), c.name().resolve(c.lang(), lang)));
        List<PackRow.ProblemLine> problems = new ArrayList<>();
        int i = 0;
        for (Problem pr : p.problems()) if (pr.error()) problems.add(line(pr, i++));
        for (Problem pr : p.problems()) if (!pr.error()) problems.add(line(pr, i++));
        String desc = p.id() == null ? I18n.get("companio.ui.broken") : p.description().resolve(p.lang(), lang);
        String meta = String.join(" · ", nonEmpty(p.builtin() ? I18n.get("companio.ui.builtin") : p.location(),
                p.version(), p.author()));
        return new PackRow(key(p), p.id() == null ? "" : p.id(), p.name().resolve(p.lang(), lang), desc, meta, PackIcons.of(p),
                p.errors(), p.loaded(), active, chips, problems);
    }

    private static PackRow.ProblemLine line(Problem p, int i) {
        return new PackRow.ProblemLine("p" + i, (p.error() ? "✖ " : "⚠ ") + p.format(), p.error());
    }

    private static List<String> nonEmpty(String... parts) {
        List<String> out = new ArrayList<>();
        for (String s : parts) if (s != null && !s.isBlank()) out.add(s);
        return out;
    }
}
