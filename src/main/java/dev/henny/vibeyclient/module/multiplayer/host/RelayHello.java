package dev.henny.vibeyclient.module.multiplayer.host;

import com.google.gson.JsonObject;

import java.util.UUID;

/**
 * The opening of a control connection to the Vibey relay (protocol in vibec-studios/vibey-multiplayer
 * {@code relay/README.md}). No game classes here, so a plain JUnit test can host against the real relay code
 * ({@code src/junit/.../RelayHostTest}, run by CI against the relay from vibey-multiplayer's main branch).
 */
public final class RelayHello {
    private RelayHello() {}

    /** Magic bytes before the newline-delimited JSON (a Minecraft packet never starts with 0x00). */
    static final byte[] MAGIC = {0x00, 'V', 'B', 'Y'};
    static final int PROTOCOL = 1;

    /**
     * Client id sent to the relay. The relay (1.3.1+) treats {@code vibey-client} like {@code mod} (a world hosted
     * from the game, slot 0) and the backend's relay API only knows {@code mod} / {@code launcher}. Anyone who wants
     * to tell the two apart must extend the relay AND the backend enum ({@code packages/shared/src/hosting.ts})
     * together; RelayHostTest catches a relay that rejects this value.
     */
    public static final String CLIENT = "vibey-client";

    public static byte[] magic() {
        return MAGIC.clone();
    }

    public static JsonObject message(String name, UUID uuid, String mcVersion, String modVersion) {
        JsonObject hello = new JsonObject();
        hello.addProperty("t", "hello");
        hello.addProperty("v", PROTOCOL);
        hello.addProperty("name", name);
        hello.addProperty("uuid", uuid.toString());
        hello.addProperty("mc", mcVersion);
        hello.addProperty("mod", modVersion);
        hello.addProperty("client", CLIENT);
        return hello;
    }
}
