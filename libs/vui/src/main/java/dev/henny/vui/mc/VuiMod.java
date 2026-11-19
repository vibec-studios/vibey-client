package dev.henny.vui.mc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VuiMod {
    public static final String ID = "vui";
    public static final Logger LOG = LoggerFactory.getLogger(ID);

    private VuiMod() {}

    public static void init(String version) {
        LOG.info("vui {} loaded", version);
    }
}
