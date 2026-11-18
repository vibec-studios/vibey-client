package dev.henny.vibeyclient.api.event;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** The events the core posts (client thread). Modules may define and post their own event types too. */
public final class VibeyEvents {
    private VibeyEvents() {}

    /** Joined a world/server (play phase started). */
    public record WorldJoin() {}

    /** Left the world/server. */
    public record WorldLeave() {}

    /**
     * A screen was (re)initialised (also after a resize). {@code addWidget}/{@code removeWidget} change its buttons,
     * e.g. to add an entry to the pause or title menu.
     */
    public record ScreenInit(Screen screen, List<? extends GuiEventListener> widgets,
                             Consumer<AbstractWidget> addWidget, Consumer<AbstractWidget> removeWidget) {}

    /** A module was enabled or disabled (by the player, a dependency or a conflict). */
    public record ModuleToggled(String moduleId, boolean enabled) {}

    /** The Vibey language changed (global setting or the game language with "auto"). Re-read cached texts. */
    public record LanguageChanged(String languageCode) {}

    /** The global "online features" switch changed. When off: no backend connection at all. */
    public record OnlineFeaturesChanged(boolean enabled) {}

    /** Posted by the social module once the backend session is up / down. */
    public record BackendConnected() {}

    public record BackendDisconnected(String reason) {}

    /** The server we are on has Vibey Client (answered hello): loadout/emote/module packets work now. */
    public record ServerHello(int protocol) {}

    /** Another player's loadout from the server channel ({@code slot → item id}; empty = nothing / left). */
    public record RemoteLoadout(UUID player, Map<String, String> slots) {}

    /** Another player started/stopped an emote (server channel). {@code startTick} is the server tick. */
    public record RemoteEmote(UUID player, String emoteId, boolean play, long startTick) {}
}
