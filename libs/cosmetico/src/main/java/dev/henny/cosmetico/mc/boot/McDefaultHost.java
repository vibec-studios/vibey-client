package dev.henny.cosmetico.mc.boot;

import dev.henny.cosmetico.host.CosmeticoHost;
import dev.henny.cosmetico.host.Log;
import net.minecraft.client.Minecraft;

public final class McDefaultHost implements CosmeticoHost {
    private static final Log LOG = new Slf4jLog();

    @Override
    public String language() {
        try {
            String code = Minecraft.getInstance().options.languageCode;
            return code == null || code.length() < 2 ? "en" : code.substring(0, 2);
        } catch (RuntimeException e) {
            return "en";
        }
    }

    @Override
    public String contentVersion() {
        return CosmeticoHost.DEFAULT.contentVersion();
    }

    @Override
    public Log log() {
        return LOG;
    }
}
