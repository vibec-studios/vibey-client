package dev.henny.vibeyclient.module.social.streamer;

import dev.henny.vibeyclient.api.privacy.Privacy;
import net.minecraft.network.chat.Component;

public final class Aliases {
    private Aliases() {}

    public static Component of(String name) {
        if (!Privacy.hideNames() || name == null || name.isEmpty()) return null;
        return Component.literal(Privacy.aliases().alias(name));
    }
}
