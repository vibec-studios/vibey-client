package dev.henny.vibeyclient.avatar;

/**
 * What the wardrobe and emote wheel UIs talk to (registered by the cosmetics module as a service:
 * {@code Services.get(Wardrobe.class)}). Client thread only.
 */
public interface Wardrobe {
    int WHEEL_SLOTS = 8;

    /** Equipped item id in {@code slot}, or null. */
    String equipped(Slot slot);

    /** Equips a cosmetic (replaces what is in its slot). Ignored for emotes or items that cannot be used. */
    void equip(AvatarItem item);

    void unequip(Slot slot);

    /** True if the player may use this item right now (owned online, builtin/dev locally). */
    boolean canUse(AvatarItem item);

    /** Short translated reason why {@link #canUse} is false (e.g. "Not unlocked"), or null. */
    String lockedReason(AvatarItem item);

    /** Emote id on wheel slot {@code index} (0 = top, then clockwise), or null. */
    String wheel(int index);

    void setWheel(int index, String emoteId);

    /** Plays an emote on the local player (no-op without a world). */
    void playEmote(String emoteId);

    void stopEmote();

    /** Id of the emote the local player is playing, or null. */
    String playingEmote();
}
