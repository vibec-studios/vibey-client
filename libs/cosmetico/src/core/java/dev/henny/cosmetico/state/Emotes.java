package dev.henny.cosmetico.state;

import java.util.UUID;

public interface Emotes {
    boolean play(UUID player, String emoteId);

    void stop(UUID player);

    String playing(UUID player);

    AutoCloseable listen(EmoteListener listener);
}
