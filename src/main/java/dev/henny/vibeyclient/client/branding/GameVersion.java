package dev.henny.vibeyclient.client.branding;

import dev.henny.vibeyclient.BuildInfo;
import net.minecraft.SharedConstants;

public final class GameVersion {
    private GameVersion() {}

    public static String name() {
        try {
            //? if >=1.21.6 {
            return SharedConstants.getCurrentVersion().name();
            //?} else
            /*return SharedConstants.getCurrentVersion().getName();*/
        } catch (Throwable t) {
            return BuildInfo.MINECRAFT_LABEL;
        }
    }
}
