package dev.henny.vibeyclient.avatar;

/** Switches the modules set from their settings; read by the render hooks (any thread). */
public final class AvatarConfig {
    private AvatarConfig() {}

    public static volatile boolean cosmetics = true;
    public static volatile boolean showOwn = true;
    public static volatile boolean showOthers = true;
    public static volatile boolean emotes = true;
    public static volatile boolean showOthersEmotes = true;
    /** Per {@link Slot}: shown at all (local toggle). */
    public static final boolean[] slotShown = new boolean[Slot.all().length];

    /** Per {@link Slot}: switched off by the current server (server rule {@code cosmetics.<plural>}, e.g. {@code cosmetics.pets}). */
    public static final boolean[] slotServerOff = new boolean[Slot.all().length];
    /** The current server disabled emotes (server rule {@code emotes}): nobody's emotes play, previews still do. */
    public static volatile boolean emotesServerOff;

    static {
        java.util.Arrays.fill(slotShown, true);
    }

    /** Shown in the world: the local toggle is on and the server allows the slot. */
    public static boolean slotVisible(Slot slot) {
        return slotShown[slot.ordinal()] && !slotServerOff[slot.ordinal()];
    }

    /** Our own UUID (the local player / profile), set by the cosmetics module. */
    public static volatile java.util.UUID self;
}
