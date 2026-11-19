package dev.henny.cosmetico.state;

import java.util.UUID;

public interface EmoteListener {
    default void started(UUID player, String emoteId) {}

    default void stopped(UUID player, String emoteId) {}

    default void ended(UUID player, String emoteId) {}
}
