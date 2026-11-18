package dev.henny.vibeyclient.avatar;

import dev.henny.vibeyclient.avatar.anim.EmotePlay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Everything the engine keeps per avatar (a player in the world, or a GUI preview): the effective loadout, the emote
 * that is playing, smoothed movement weights for cosmetic animations, the pet's smoothed position and a snapshot of
 * the last render. Client thread only.
 */
public final class AvatarState {
    public final UUID uuid;
    public final boolean preview;

    /** Item id per {@link Slot} (null = empty). */
    public final String[] slots = new String[Slot.all().length];
    /** Preview overrides: when {@code overridden[i]}, {@code overrides[i]} is shown instead of {@code slots[i]}. */
    public final String[] overrides = new String[Slot.all().length];
    public final boolean[] overridden = new boolean[Slot.all().length];

    public EmotePlay emote;

    // movement (smoothed in tick), 0..1
    public float walk, fly, sneak;
    public float walkTarget, flyTarget, sneakTarget;

    // pet (world space, smoothed)
    public double petX, petY, petZ, petPrevX, petPrevY, petPrevZ;
    public float petYaw, petPrevYaw, petMove;
    public boolean petInit;

    // snapshot of the frame (written when the avatar is extracted/rendered)
    public float partial;
    public double x, y, z;
    public float bodyYaw;
    public boolean headArmor, chestArmor, legsArmor, feetArmor;
    /** Hide all cosmetics this frame (invisible, spectator, settings). */
    public boolean hidden;
    /** Local player seen from first person (the world renderer does not draw them, but hands may). */
    public boolean firstPerson;
    public boolean crouching, swimming, fallFlying;
    public long lastSeenTick;

    /** World positions of locators from the last render (for particles/sounds). */
    public final Map<String, double[]> locators = new HashMap<>();
    /** Last processed animation-local time per (slot) for cosmetic idle events. */
    public final float[] idleEventTime = new float[Slot.all().length];
    /** Event clocks of the walk / fly / sneak animations per slot ([slot * 3 + k]), -1 = not running. */
    public final float[] moveEventTime = new float[Slot.all().length * 3];

    public AvatarState(UUID uuid, boolean preview) {
        this.uuid = uuid;
        this.preview = preview;
        java.util.Arrays.fill(idleEventTime, -1f);
        java.util.Arrays.fill(moveEventTime, -1f);
    }

    /** Item id shown in {@code slot} (respecting preview overrides). */
    public String shown(Slot slot) {
        int i = slot.ordinal();
        return overridden[i] ? overrides[i] : slots[i];
    }

    public double[] locator(String name) {
        return locators.computeIfAbsent(name, k -> new double[]{Double.NaN, 0, 0});
    }
}
