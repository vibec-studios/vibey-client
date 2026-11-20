package dev.henny.vibeyclient.module.info.pin;

import dev.henny.vibeyclient.module.info.card.CardData;
import dev.henny.vibeyclient.module.info.card.ToolIcons;
import dev.henny.vui.bind.State;

public final class CardController {
    public final State<String> title = new State<>("");
    public final State<String> mod = new State<>("");
    public final State<Boolean> hasMod = new State<>(false);
    public final State<String> mark = new State<>("");
    public final State<Boolean> good = new State<>(true);
    public final State<String> status = new State<>("");
    public final State<Boolean> hasStatus = new State<>(false);

    public void apply(CardData d) {
        title.set(d.title());
        mod.set(d.mod() == null ? "" : d.mod());
        hasMod.set(d.mod() != null && !d.mod().isEmpty());
        good.set(!d.hasHarvest() || d.harvest().canHarvest());
        mark.set(d.hasHarvest() ? (d.harvest().canHarvest() ? "✔" : "✖") : "");
        status.set(statusText(d));
        hasStatus.set(d.hasStatus());
    }

    private static String statusText(CardData d) {
        StringBuilder sb = new StringBuilder();
        if (d.hasHarvest() && d.harvest().showTool()) sb.append(ToolIcons.of(d.harvest()).getHoverName().getString());
        if (d.hasGrowth()) append(sb, (d.mature() ? "✔ " : "") + d.growth() + "%");
        if (d.health() != null) append(sb, "♥ " + d.health());
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (!sb.isEmpty()) sb.append("  ");
        sb.append(part);
    }
}
