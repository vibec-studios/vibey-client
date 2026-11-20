package dev.henny.cosmetico.mc;

import dev.henny.cosmetico.mc.boot.CosmeticoBoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CosmeticoMod {
    public static final String ID = "cosmetico";
    public static final Logger LOG = LoggerFactory.getLogger(ID);

    private CosmeticoMod() {}

    public static void init(String version) {
        LOG.info("cosmetico {} loaded", version);
        CosmeticoBoot.ensure();
    }
}
