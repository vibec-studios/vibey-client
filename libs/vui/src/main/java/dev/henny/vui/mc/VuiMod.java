package dev.henny.vui.mc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VuiMod {
    public static final String ID = "vui";
    public static final Logger LOG = LoggerFactory.getLogger(ID);

    private static boolean announced;

    private VuiMod() {}

    public static synchronized void init(String version) {
        if (announced) return;
        announced = true;
        LOG.info("vui {} loaded", version);
    }
}
