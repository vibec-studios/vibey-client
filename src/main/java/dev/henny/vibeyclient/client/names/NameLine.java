package dev.henny.vibeyclient.client.names;

import dev.henny.vibeyclient.client.ranks.Ranks;
import dev.henny.vibeyclient.ranks.Identity;
import dev.henny.vibeyclient.ranks.NameStyleDef;
import dev.henny.vibeyclient.ranks.RanksDoc;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class NameLine {
    private NameLine() {}

    public static Component of(Identity id, String name, int color, boolean animate) {
        RanksDoc doc = Ranks.doc();
        Identity who = id == null ? Identity.EMPTY : id;
        MutableComponent out = Component.empty();
        MutableComponent prefix = NamePrefix.build(false, who, true, true, doc);
        if (prefix != null) out.append(prefix).append(Component.literal(" "));
        NameStyleDef style = doc.style(who.style());
        double t = animate ? StyleClock.seconds(StyleClock.bucket()) : 0;
        if (style == null) out.append(Component.literal(name).setStyle(Style.EMPTY.withColor(color & 0xFFFFFF)));
        else out.append(NameRecolor.colored(name, Style.EMPTY, style.style(), t));
        return out;
    }

    public static Component styled(NameStyleDef style, String name, double t) {
        return NameRecolor.colored(name, Style.EMPTY, style.style(), t);
    }
}
