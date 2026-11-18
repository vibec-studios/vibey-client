package dev.henny.vibeyclient.backend;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.util.UUID;

/**
 * The game session, as far as the backend login needs it. The Minecraft access token is only handed to Mojang's
 * session server ({@code joinServer}, like joining a Minecraft server) and never sent to the Vibe backend.
 */
public final class MojangJoin {
    private MojangJoin() {}

    public static String name() {
        return Minecraft.getInstance().getUser().getName();
    }

    public static UUID profileId() {
        return Minecraft.getInstance().getUser().getProfileId();
    }

    /**
     * The game runs with a real Microsoft session (its token is a JWT). Offline/dev sessions ("0", "FabricMC") can
     * never pass Mojang's check, so they do not log in by themselves (no pointless challenges against the backend).
     */
    public static boolean hasOnlineSession() {
        String t = Minecraft.getInstance().getUser().getAccessToken();
        return t != null && t.length() > 40 && t.chars().filter(c -> c == '.').count() == 2;
    }

    /** Blocking (network): call it on a worker thread. Returns null on success, else an error text. */
    public static String joinServer(String serverId) {
        Minecraft mc = Minecraft.getInstance();
        User user = mc.getUser();
        try {
            //? if >=1.21.9 {
            mc.services().sessionService().joinServer(user.getProfileId(), user.getAccessToken(), serverId);
            //?} else
            /*mc.getMinecraftSessionService().joinServer(user.getProfileId(), user.getAccessToken(), serverId);*/
            return null;
        } catch (Exception e) {
            return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
    }
}
