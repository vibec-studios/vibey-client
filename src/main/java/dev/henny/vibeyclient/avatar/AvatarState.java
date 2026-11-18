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
    /**
     * Optional states (since 0.2.2): sprinting, gliding (elytra or a long fall), standing still for a while
     * ({@code idle_long}) and {@code happy} (the owner plays an emote). 0..1, smoothed in tick.
     */
    public float sprint, glide, idleLong, happy;
    public float sprintTarget, glideTarget, idleLongTarget, happyTarget;
    /** Ticks the player has stood still (no movement, on the ground, no emote), ticks spent falling. */
    public int stillTicks, fallTicks;
    /** False until the first movement tick (no previous position yet). */
    public boolean moveInit;
    /**
     * Clock time at which each optional state started ({@link dev.henny.vibeyclient.avatar.render.AvatarDraw#STATE_ANIMS}
     * order), so their animations play from the start when the state begins. NaN = not running.
     */
    public final double[] stateStart = new double[8];

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
    /** Event clocks of the non-idle cosmetic animations per slot ([slot * MOVE_ANIMS + k]), -1 = not running. */
    public final float[] moveEventTime = new float[Slot.all().length * MOVE_ANIMS];
    /** Number of cosmetic animations besides {@code idle} (walk, fly, sneak, sprint, glide, idle_long, happy). */
    public static final int MOVE_ANIMS = 7;
    /** Smoothed weight of each {@code cosmetic.offsetWhen} rule per (item slot, other slot): [slot * SLOTS + other]. */
    public final float[] offsetWeight = new float[Slot.all().length * Slot.all().length];
    /** Clock time of the last offset smoothing step per item slot (NaN = never). */
    public final double[] offsetClock = new double[Slot.all().length];

    public AvatarState(UUID uuid, boolean preview) {
        this.uuid = uuid;
        this.preview = preview;
        java.util.Arrays.fill(idleEventTime, -1f);
        java.util.Arrays.fill(moveEventTime, -1f);
        java.util.Arrays.fill(stateStart, Double.NaN);
        java.util.Arrays.fill(offsetClock, Double.NaN);
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
