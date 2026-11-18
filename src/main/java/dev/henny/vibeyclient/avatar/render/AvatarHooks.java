package dev.henny.vibeyclient.avatar.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.avatar.AvatarConfig;
import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.AvatarStates;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.anim.EmotePlay;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.bundle.Bundle.PlayerBone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;

/**
 * The glue between the vanilla player renderer and the engine, called from the mixins in
 * {@code module/cosmetics/mixin} and {@code module/emotes/mixin}. Keeps the per-version differences in the mixins
 * small: they only forward arguments.
 */
public final class AvatarHooks {
    private AvatarHooks() {}

    private static final Quaternionf Q = new Quaternionf();
    public static long frames;

    /**
     * Snapshot of a player for this frame (1.21.2+: at extractRenderState; 1.21.1: at render). Returns the state or
     * null when Vibey has nothing for this player.
     */
    public static AvatarState extract(Player p, float partial) {
        AvatarState st = AvatarStates.peek(p.getUUID());
        if (st == null) return null;
        Minecraft mc = Minecraft.getInstance();
        boolean self = p == mc.player || p.getUUID().equals(AvatarConfig.self);
        st.partial = partial;
        st.x = p.xo + (p.getX() - p.xo) * partial;
        st.y = p.yo + (p.getY() - p.yo) * partial;
        st.z = p.zo + (p.getZ() - p.zo) * partial;
        st.bodyYaw = p.yBodyRotO + wrap(p.yBodyRot - p.yBodyRotO) * partial;
        st.headArmor = !p.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
        st.chestArmor = !p.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
        st.legsArmor = !p.getItemBySlot(EquipmentSlot.LEGS).isEmpty();
        st.feetArmor = !p.getItemBySlot(EquipmentSlot.FEET).isEmpty();
        st.crouching = p.isCrouching();
        st.fallFlying = p.isFallFlying();
        boolean invisible = mc.player != null ? p.isInvisibleTo(mc.player) : p.isInvisible();
        st.hidden = !AvatarConfig.cosmetics || invisible || p.isSpectator() || (self ? !AvatarConfig.showOwn : !AvatarConfig.showOthers);
        return st;
    }

    /** Emote of this avatar (null = none or emotes hidden for it). */
    public static EmotePlay emote(AvatarState st) {
        if (st == null || st.emote == null || !AvatarConfig.emotes) return null;
        if (!st.preview && !AvatarConfig.showOthersEmotes && st.uuid != null && !st.uuid.equals(AvatarConfig.self)) return null;
        return st.emote;
    }

    /** Should the held items be hidden (emote with hideHeldItems)? */
    public static boolean hideHeldItems(AvatarState st) {
        EmotePlay e = emote(st);
        return e != null && e.bundle.emote != null && e.bundle.emote.hideHeldItems() && e.weight > 0.01f;
    }

    // ============================================================================================ emote pose

    /**
     * After vanilla posed the model: blends the emote's player bones over it (Blockbench values relative to the
     * standing pose, converted to model space).
     */
    public static void emotePose(ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg,
                                 ModelPart leftLeg, AvatarState st) {
        EmotePlay e = emote(st);
        if (e == null || e.weight <= 0f) return;
        apply(head, PlayerBone.HEAD, e);
        apply(body, PlayerBone.BODY, e);
        apply(rightArm, PlayerBone.RIGHT_ARM, e);
        apply(leftArm, PlayerBone.LEFT_ARM, e);
        apply(rightLeg, PlayerBone.RIGHT_LEG, e);
        apply(leftLeg, PlayerBone.LEFT_LEG, e);
    }

    private static void apply(ModelPart part, PlayerBone b, EmotePlay e) {
        int ch = e.channels[b.ordinal()];
        if (ch == 0) return;
        float w = e.weight;
        float[] p = e.pose;
        int o = b.ordinal() * 9;
        final float d = (float) (Math.PI / 180.0);
        if ((ch & 1) != 0) {
            part.xRot = lerp(part.xRot, -p[o] * d, w);
            part.yRot = lerp(part.yRot, -p[o + 1] * d, w);
            part.zRot = lerp(part.zRot, p[o + 2] * d, w);
        }
        if ((ch & 2) != 0) {
            part.x = lerp(part.x, -b.px - p[o + 3], w);
            part.y = lerp(part.y, 24f - b.py - p[o + 4], w);
            part.z = lerp(part.z, b.pz + p[o + 5], w);
        }
        if ((ch & 4) != 0) {
            part.xScale = lerp(part.xScale, p[o + 6], w);
            part.yScale = lerp(part.yScale, p[o + 7], w);
            part.zScale = lerp(part.zScale, p[o + 8], w);
        }
    }

    /**
     * End of setupRotations: the pose stack is a Blockbench-like frame at the feet (blocks, +Y up, facing -Z). Applies
     * the emote's {@code root} bone (pivot at the feet). {@code modelScale} is the renderer's model scale (0.9375).
     */
    public static void rootTransform(AvatarState st, PoseStack ps, float modelScale) {
        EmotePlay e = emote(st);
        if (e == null || e.weight <= 0f) return;
        int ch = e.channels[PlayerBone.ROOT.ordinal()];
        if (ch == 0) return;
        float w = e.weight;
        float[] p = e.pose;
        int o = PlayerBone.ROOT.ordinal() * 9;
        if ((ch & 2) != 0) {
            float k = w * modelScale / 16f;
            ps.translate(p[o + 3] * k, p[o + 4] * k, p[o + 5] * k);
        }
        if ((ch & 1) != 0) {
            final float d = (float) (Math.PI / 180.0) * w;
            ps.mulPose(Q.rotationZYX(p[o + 2] * d, p[o + 1] * d, p[o] * d));
        }
        if ((ch & 4) != 0) {
            ps.scale(lerp(1f, p[o + 6], w), lerp(1f, p[o + 7], w), lerp(1f, p[o + 8], w));
        }
    }

    // ============================================================================================ cape

    /** The texture of the equipped cape (vanilla cape model and elytra), or null. */
    public static Tex capeTexture(AvatarState st) {
        if (st == null || st.hidden || !AvatarConfig.slotShown[Slot.CAPE.ordinal()]) return null;
        Bundle b = AvatarDraw.bundle(st, Slot.CAPE);
        if (b == null || b.capeTexture < 0 || AvatarDraw.hiddenByArmor(st, b)) return null;
        GpuBundle gb = GpuBundles.get(b);
        return gb == null ? null : gb.texture(b.capeTexture);
    }

    /** The texture of the equipped shield (64×64 vanilla shield layout), or null. */
    public static Tex shieldTexture(AvatarState st) {
        if (st == null || st.hidden || !AvatarConfig.slotShown[Slot.SHIELD.ordinal()]) return null;
        Bundle b = AvatarDraw.bundle(st, Slot.SHIELD);
        if (b == null || b.shieldTexture < 0) return null;
        GpuBundle gb = GpuBundles.get(b);
        return gb == null ? null : gb.texture(b.shieldTexture);
    }

    // ============================================================================================ util

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    static float wrap(float deg) {
        return ((deg % 360f) + 540f) % 360f - 180f;
    }
}
