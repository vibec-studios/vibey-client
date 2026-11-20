package dev.henny.vibeyclient.module.cosmetics.selftest;

import dev.henny.vibeyclient.Vibey;

final class SelfTestFlags {
    private SelfTestFlags() {}

    private static final String MODE = System.getenv("VIBEYCLIENT_AVATAR_TEST");
    static final boolean ON = Vibey.platform().isDevelopment() && (Boolean.getBoolean("vibeyclient.avatarTest") || MODE != null);
    static final boolean DEVLINK = "devlink".equals(MODE);
    static final boolean WORLD = DEVLINK || "world".equals(MODE);

    static void log(String s) {
        Vibey.LOG.info("[vibeyclient-avatartest] {}", s);
    }
}
