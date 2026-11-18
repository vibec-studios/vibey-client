package dev.henny.vibeyclient.api.service;

/**
 * Small read-only social numbers for menus (title screen status bar, pause menu badges). Registered by the social
 * module next to {@link VibeBackend}: {@code Services.register(SocialStatus.class, impl)}. Cheap getters, called
 * every frame on the render thread – return cached values.
 */
public interface SocialStatus {
    /** Friends that are online right now, or -1 if unknown (not logged in yet). */
    int onlineFriends();

    /** Unread chat messages + open friend requests (badge on the Friends entry), 0 if none. */
    default int badge() {
        return 0;
    }
}
