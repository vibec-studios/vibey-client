package dev.henny.vibeyclient.module.nametags;

import dev.henny.vibeyclient.client.VibeyPlayers;
import dev.henny.vibeyclient.client.names.NamePrefix;
import dev.henny.vibeyclient.client.names.NameRecolor;
import dev.henny.vibeyclient.client.names.StyleClock;
import dev.henny.vibeyclient.client.ranks.Ranks;
import dev.henny.vibeyclient.ranks.Identity;
import dev.henny.vibeyclient.ranks.NameStyleDef;
import dev.henny.vibeyclient.ranks.RanksDoc;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.UUID;

final class Decorator {
    private Decorator() {}

    static boolean animates(UUID player, int flags) {
        if (!Surface.has(flags, Surface.STYLE) || !Surface.has(flags, Surface.ANIMATE)) return false;
        NameStyleDef s = Ranks.style(VibeyPlayers.identity(player).style());
        return s != null && s.style().animated();
    }

    static Component build(UUID player, String profileName, Component name, int flags, long bucket) {
        Identity id = VibeyPlayers.identity(player);
        RanksDoc doc = Ranks.doc();
        Component body = name;
        NameStyleDef style = Surface.has(flags, Surface.STYLE) ? doc.style(id.style()) : null;
        if (style != null) body = NameRecolor.apply(name, profileName, style.style(), StyleClock.seconds(bucket));
        boolean seal = Surface.has(flags, Surface.SEAL) && VibeyPlayers.isVibey(player);
        MutableComponent prefix = NamePrefix.build(seal, id, Surface.has(flags, Surface.RANK), Surface.has(flags, Surface.TAG), doc);
        if (prefix == null) return body;
        return prefix.append(Component.literal(" ")).append(body);
    }
}
