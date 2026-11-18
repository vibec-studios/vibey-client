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

    static {
        java.util.Arrays.fill(slotShown, true);
    }

    /** Our own UUID (the local player / profile), set by the cosmetics module. */
    public static volatile java.util.UUID self;
}
