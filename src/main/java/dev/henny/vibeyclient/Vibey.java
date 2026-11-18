package dev.henny.vibeyclient;

import dev.henny.vibeyclient.net.ServerRelay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (server-safe) entry point. Each loader sets the {@link Platform} and forwards its events. Nothing in here
 * may touch client classes: the same jar runs on dedicated servers, where it only relays emote/loadout packets.
 * The client side starts in {@code dev.henny.vibeyclient.core.VibeyCore}.
 */
public final class Vibey {
    public static final String MOD_ID = "vibeyclient";
    public static final String NAME = "Vibey Client";
    public static final Logger LOG = LoggerFactory.getLogger("VibeyClient");

    private static Platform platform;

    private Vibey() {}

    /** Idempotent: the first call wins (NeoForge may construct the client @Mod before the common one). */
    public static synchronized void initCommon(Platform p) {
        if (platform != null) return;
        platform = p;
        LOG.info("{} {} for Minecraft {} on {}", NAME, BuildInfo.VERSION, BuildInfo.MINECRAFT_LABEL, p.loader());
        ServerRelay.init();
    }

    public static Platform platform() {
        if (platform == null) throw new IllegalStateException("Vibey Client is not initialised yet");
        return platform;
    }
}
