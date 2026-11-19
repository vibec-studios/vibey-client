package dev.henny.vibeyclient.api.service;

/**
 * Hosting the current singleplayer world for friends through the Vibey relay. Registered by the multiplayer module
 * while it is enabled; used by social (invites to your own world, presence "hosting").
 * Look it up with {@code Services.get(HostingService.class)} every time – it disappears when the module is off.
 */
public interface HostingService {
    /** True while a world is open that could be hosted (integrated server running, we are its owner). */
    boolean canHost();

    /** True while the world is online on the relay. */
    boolean isHosting();

    /** Public address friends join with (e.g. {@code mossy-otter-27.axolite.cc}), or null when not hosting. */
    String address();

    /** Port of {@link #address()} (25565 unless the relay says otherwise). */
    int port();

    /** Starts hosting (async; result via {@link HostingChanged} events). Opens no UI. */
    void startHosting();

    /** Stops hosting (keeps the world open). */
    void stopHosting();

    /** Opens the multiplayer module's own host/admin screen. */
    void openHostScreen();

    /** The invite link ({@code https://axolite.cc/j/<code>}, the {@code url} the backend returned) while hosting with one, else null. */
    default String playLink() {
        return null;
    }

    /** Players in the hosted world (host included), or -1 when not hosting. */
    default int players() {
        return -1;
    }

    /** Player limit of the hosted world, or -1 when not hosting. */
    default int maxPlayers() {
        return -1;
    }

    /** Posted on the event bus (client thread) whenever hosting starts, stops or the address changes. */
    record HostingChanged(boolean hosting, String address) {}
}
