package dev.henny.vibeyclient.module.nametags;

import dev.henny.vibeyclient.client.names.StyleClock;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public final class NameIcons {
    private NameIcons() {}

    private static final DecorCache NAMETAG = new DecorCache(), TAB = new DecorCache();

    public static Component nametag(UUID player, String profileName, Component name) {
        if (player == null || name == null) return name;
        int flags = Surface.NAMETAG.flags(NametagsModule.instance);
        if (flags == 0) return name;
        long bucket = Decorator.animates(player, flags) && AnimatedSet.contains(player) ? StyleClock.bucket() : 0;
        return NAMETAG.get(player, profileName, name, flags, bucket);
    }

    public static Component tabList(UUID player, String profileName, Component name) {
        if (player == null || name == null) return name;
        int flags = Surface.TAB.flags(NametagsModule.instance);
        if (flags == 0) return name;
        long bucket = Decorator.animates(player, flags) ? StyleClock.bucket() : 0;
        return TAB.get(player, profileName, name, flags, bucket);
    }

    public static Component chat(UUID player, String profileName, Component name) {
        if (player == null || name == null) return name;
        int flags = Surface.CHAT.flags(NametagsModule.instance);
        return flags == 0 ? name : Decorator.build(player, profileName, name, flags, 0);
    }

    public static int animatedCount() {
        return AnimatedSet.size();
    }

    public static void clearCaches() {
        NAMETAG.clear();
        TAB.clear();
    }
}
