package dev.henny.r3.mc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class R3Mod {
    public static final String ID = "r3";
    public static final Logger LOG = LoggerFactory.getLogger(ID);

    private R3Mod() {}

    public static void init(String version) {
        LOG.info("r3 {} loaded", version);
    }
}
