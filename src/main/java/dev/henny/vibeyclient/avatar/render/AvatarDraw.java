package dev.henny.vibeyclient.avatar.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.avatar.AvatarItem;
import dev.henny.vibeyclient.avatar.AvatarItems;
import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.anim.Clock;
import dev.henny.vibeyclient.avatar.anim.EmotePlay;
import dev.henny.vibeyclient.avatar.anim.Sampler;
import dev.henny.vibeyclient.avatar.anim.StateWeights;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.bundle.Bundle.Animation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws an avatar's cosmetics and emote props. Two entry points, called by the player renderer hooks (and by the GUI
 * preview with its own pose stack):
 * <ul>
 *   <li>{@link #attached}: inside the player's model space, after the vanilla model parts were posed (follows
 *       sneaking, swimming, elytra and emote poses automatically).</li>
 *   <li>{@link #world}: at the player's feet in unrotated world axes (pets with their own smoothed position, auras
 *       that stay level while the player tilts).</li>
 * </ul>
 */
public final class AvatarDraw {
    private AvatarDraw() {}

    /** Global switches (cosmetics module settings). */
    public static volatile boolean hideWithArmor = true;

    private static final BoneRenderer.Mix MIX = new BoneRenderer.Mix();
    private static final Quaternionf Q = new Quaternionf();
    private static final Vector3f V = new Vector3f();
    /** Camera position for locator world positions (NaN in GUI previews). */
    private static double camX = Double.NaN, camY, camZ;
    private static AvatarState locatorTarget;

    private static final BoneRenderer.LocatorSink LOCATORS = (l, mat, pivot) -> {
        if (locatorTarget == null || Double.isNaN(camX)) return;
        BoneRenderer.locatorLocal(l, pivot, V);
        mat.transformPosition(V);
        double[] out = locatorTarget.locator(l.name());
        out[0] = V.x + camX;
        out[1] = V.y + camY;
        out[2] = V.z + camZ;
    };

    /** Set before rendering world avatars (camera position), NaN for GUI. */
    public static void camera(double x, double y, double z) {
        camX = x;
        camY = y;
        camZ = z;
    }

    // ============================================================================================ attached

    /** Cosmetics and emote props that hang on the player's parts (and root). */
    public static void attached(AvatarState st, PoseStack ps, MeshSink sink, PlayerParts parts, int light) {
        if (st.hidden) return;
        double now = Clock.now();
        locatorTarget = st.preview || WorldPass.gui > 0 ? null : st;
        for (Slot slot : Slot.all()) {
            if (slot == Slot.SHIELD) continue;
            Bundle b = bundle(st, slot);
            if (b == null || b.model == null || hiddenByArmor(st, b)) continue;
            GpuBundle gb = GpuBundles.get(b);
            if (gb == null) continue;
            BoneRenderer.Mix mix = cosmeticMix(st, b, slot, now);
            if (slot == Slot.PET && shoulder(b) != 0) {
                // shoulder pets sit on the body part, at the top of the shoulder
                ps.pushPose();
                parts.apply(Bundle.PlayerBone.BODY, ps);
                ps.translate(shoulder(b) * -6f / 16f, -0.01f, 0.5f / 16f);
                float s = b.pet == null ? 1f : b.pet.scale();
                ps.scale(s, s, s);
                ps.translate(0f, -1.5f, 0f);
                BoneRenderer.render(gb, mix, ps, sink, null, BoneRenderer.Pass.WORLD, light, -1, LOCATORS);
                ps.popPose();
            }
            BoneRenderer.render(gb, mix, ps, sink, parts, BoneRenderer.Pass.ATTACHED, light, -1, LOCATORS);
        }
        EmotePlay e = st.emote;
        if (e != null && e.bundle.model != null) {
            GpuBundle gb = GpuBundles.get(e.bundle);
            if (gb != null) {
                MIX.clear().add(e.anim, e.localTime, 1f);
                MIX.visibility = e.anim;
                MIX.visibilityTime = e.localTime;
                BoneRenderer.render(gb, MIX, ps, sink, parts, BoneRenderer.Pass.ATTACHED, light, -1, LOCATORS);
            }
        }
        locatorTarget = null;
    }

    // ============================================================================================ world

    /**
     * World-attached bones. {@code ps} is at the player's render position (feet) with world axes; {@code px/py/pz}
     * is that position in the world (for the pet's own position).
     */
    public static void world(AvatarState st, PoseStack ps, MeshSink sink, int light, double px, double py, double pz, float playerScale) {
        if (st.hidden) return;
        double now = Clock.now();
        locatorTarget = st.preview ? null : st;
        for (Slot slot : Slot.all()) {
            if (slot == Slot.SHIELD) continue;
            Bundle b = bundle(st, slot);
            if (b == null || b.model == null || !hasWorldBones(b) || hiddenByArmor(st, b)) continue;
            if (slot == Slot.PET && shoulder(b) != 0) continue;
            GpuBundle gb = GpuBundles.get(b);
            if (gb == null) continue;
            ps.pushPose();
            if (slot == Slot.PET) {
                float pt = st.partial;
                double x = lerp(pt, st.petPrevX, st.petX) - px, y = lerp(pt, st.petPrevY, st.petY) - py, z = lerp(pt, st.petPrevZ, st.petZ) - pz;
                if (!st.petInit) x = y = z = 0;
                ps.translate((float) x, (float) y, (float) z);
                float yaw = lerpDeg(pt, st.petPrevYaw, st.petYaw);
                PoseCompat.rotate(ps, Q.rotationY((float) Math.toRadians(180f - yaw)));
                float s = b.pet == null ? 1f : b.pet.scale();
                ps.scale(s * playerScale, s * playerScale, s * playerScale);
            } else {
                PoseCompat.rotate(ps, Q.rotationY((float) Math.toRadians(180f - st.bodyYaw)));
                ps.scale(playerScale, playerScale, playerScale);
            }
            toModelSpace(ps);
            BoneRenderer.render(gb, cosmeticMix(st, b, slot, now), ps, sink, null, BoneRenderer.Pass.WORLD, light, -1, LOCATORS);
            ps.popPose();
        }
        EmotePlay e = st.emote;
        if (e != null && e.bundle.model != null && hasWorldBones(e.bundle)) {
            GpuBundle gb = GpuBundles.get(e.bundle);
            if (gb != null) {
                ps.pushPose();
                PoseCompat.rotate(ps, Q.rotationY((float) Math.toRadians(180f - st.bodyYaw)));
                ps.scale(playerScale, playerScale, playerScale);
                toModelSpace(ps);
                MIX.clear().add(e.anim, e.localTime, 1f);
                MIX.visibility = e.anim;
                MIX.visibilityTime = e.localTime;
                BoneRenderer.render(gb, MIX, ps, sink, null, BoneRenderer.Pass.WORLD, light, -1, LOCATORS);
                ps.popPose();
            }
        }
        locatorTarget = null;
    }

    /** From a Blockbench-like frame (blocks, +Y up, facing -Z, origin at the feet) into model space. */
    public static void toModelSpace(PoseStack ps) {
        ps.scale(-1f, -1f, 1f);
        ps.translate(0f, -1.5f, 0f);
    }

    // ============================================================================================ helpers

    /** +1 right shoulder, -1 left shoulder, 0 not a shoulder pet. */
    private static int shoulder(Bundle b) {
        if (b.pet == null) return 0;
        return switch (b.pet.follow()) {
            case SHOULDER_RIGHT -> 1;
            case SHOULDER_LEFT -> -1;
            default -> 0;
        };
    }

    public static Bundle bundle(AvatarState st, Slot slot) {
        if (!st.preview && !dev.henny.vibeyclient.avatar.AvatarConfig.slotShown[slot.ordinal()]) return null;
        String id = st.shown(slot);
        if (id == null) return null;
        AvatarItem item = AvatarItems.get(id);
        if (item == null || item.isEmote()) return null;
        Bundle b = item.bundle();
        return b != null && b.slot == slot ? b : null;
    }

    public static boolean hiddenByArmor(AvatarState st, Bundle b) {
        if (!hideWithArmor || b.hideWithArmor.isEmpty()) return false;
        for (Bundle.Armor a : b.hideWithArmor) {
            switch (a) {
                case HEAD -> { if (st.headArmor) return true; }
                case CHEST -> { if (st.chestArmor) return true; }
                case LEGS -> { if (st.legsArmor) return true; }
                case FEET -> { if (st.feetArmor) return true; }
            }
        }
        return false;
    }

    private static boolean hasWorldBones(Bundle b) {
        for (Bundle.Bone bone : b.model.bones) if (bone.parent < 0 && bone.attach == Bundle.Attach.WORLD) return true;
        return false;
    }

    /**
     * Cosmetic animation names with meaning (SPEC §5), in {@link #weights} order. Others are ignored. {@code sprint},
     * {@code glide}, {@code idle_long} and {@code happy} are optional (since 0.2.2); a bundle without them keeps
     * playing {@code walk} / {@code fly} / {@code idle} in those situations, exactly like older clients.
     */
    public static final String[] COSMETIC_ANIMS = StateWeights.NAMES;
    public static final int IDLE = StateWeights.IDLE, WALK = StateWeights.WALK, FLY = StateWeights.FLY, SNEAK = StateWeights.SNEAK,
            SPRINT = StateWeights.SPRINT, GLIDE = StateWeights.GLIDE, IDLE_LONG = StateWeights.IDLE_LONG, HAPPY = StateWeights.HAPPY;
    private static final boolean[] HAS = new boolean[COSMETIC_ANIMS.length];

    /** Blend weights of {@link #COSMETIC_ANIMS} for a cosmetic, see {@link StateWeights}. Pets move by {@code petMove}. */
    public static void weights(AvatarState st, Bundle b, Slot slot, float[] out) {
        for (int k = 0; k < HAS.length; k++) {
            // an empty optional state (e.g. a track the Blockbench wizard prepared but nobody animated) counts as missing
            Animation a = b.animation(COSMETIC_ANIMS[k]);
            HAS[k] = a != null && (k <= SNEAK || a.tracks.length > 0 || a.events.length > 0);
        }
        StateWeights.compute(HAS, st.glide, st.fly, slot == Slot.PET ? st.petMove : st.walk, st.sprint, st.sneak, st.happy, st.idleLong, out);
    }

    private static final float[] W = new float[COSMETIC_ANIMS.length];

    /** All playing animations blended by {@link #weights}; visibility events follow the strongest animation that has some. */
    private static BoneRenderer.Mix cosmeticMix(AvatarState st, Bundle b, Slot slot, double now) {
        weights(st, b, slot, W);
        MIX.clear();
        float best = -1f;
        for (int k = 0; k < COSMETIC_ANIMS.length; k++) {
            Animation a = b.animation(COSMETIC_ANIMS[k]);
            if (a == null) continue;
            float t = time(st, k, a, now);
            MIX.add(a, t, W[k]);
            // idle keeps the visibility unless another animation with visibility events clearly dominates
            float score = k == IDLE ? Math.max(W[0], 0.5f) : W[k];
            if (a.hasVisibility && score > best) {
                best = score;
                MIX.visibility = a;
                MIX.visibilityTime = t;
            }
        }
        if (MIX.visibility == null) {
            Animation idle = b.animation("idle");
            if (idle != null) {
                MIX.visibility = idle;
                MIX.visibilityTime = time(idle, now);
            }
        }
        offsets(st, b, slot, now, MIX);
        return MIX;
    }

    /**
     * {@code cosmetic.offsetWhen}: shifts / turns the item's root bones while another slot is worn (and shown), eased
     * in and out over ~0.3 s. Writes the blended offset into {@code mix}.
     */
    private static void offsets(AvatarState st, Bundle b, Slot slot, double now, BoneRenderer.Mix mix) {
        if (b.offsetWhen.isEmpty()) return;
        int n = Slot.all().length;
        double last = st.offsetClock[slot.ordinal()];
        double dt = Double.isNaN(last) ? 1.0 : Math.max(0.0, Math.min(1.0, now - last));
        float k = (float) (1.0 - Math.exp(-dt * 12.0));
        for (Bundle.OffsetRule r : b.offsetWhen) {
            if (r.slot() == slot) continue;
            Bundle other = bundle(st, r.slot());
            boolean worn = other != null && !hiddenByArmor(st, other);
            int i = slot.ordinal() * n + r.slot().ordinal();
            float w = st.offsetWeight[i];
            w += ((worn ? 1f : 0f) - w) * k;
            if (Math.abs(w - (worn ? 1f : 0f)) < 0.002f) w = worn ? 1f : 0f;
            st.offsetWeight[i] = w;
            if (w <= 0f) continue;
            float e = w * w * (3f - 2f * w); // smoothstep
            for (int c = 0; c < 3; c++) {
                mix.rootOffset[c] += r.offset()[c] * e;
                mix.rootRotation[c] += r.rotation()[c] * e;
            }
            mix.hasRootOffset = true;
        }
        st.offsetClock[slot.ordinal()] = now;
    }

    public static float time(Animation a, double now) {
        return a.loop ? (float) (now % a.length) : Sampler.localTime((float) (now % (a.length * 2)), a.length, false);
    }

    /**
     * Animation time of state {@code k}: the optional states (sprint, glide, idle_long, happy) start from the
     * beginning when the state begins (non-looping ones hold their last frame); the others use the global clock.
     */
    public static float time(AvatarState st, int k, Animation a, double now) {
        if (k >= SPRINT && k < st.stateStart.length && !Double.isNaN(st.stateStart[k])) {
            double el = now - st.stateStart[k];
            return a.loop ? (float) (el % a.length) : (float) Math.min(el, a.length);
        }
        return time(a, now);
    }

    private static double lerp(float t, double a, double b) {
        return a + (b - a) * t;
    }

    private static float lerpDeg(float t, float a, float b) {
        float d = ((b - a) % 360f + 540f) % 360f - 180f;
        return a + d * t;
    }
}
