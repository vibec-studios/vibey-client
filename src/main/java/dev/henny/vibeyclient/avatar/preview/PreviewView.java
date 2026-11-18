package dev.henny.vibeyclient.avatar.preview;

import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.AvatarStates;
import dev.henny.vibeyclient.avatar.EngineCompat;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.anim.Clock;
import dev.henny.vibeyclient.avatar.render.AvatarDraw;
import dev.henny.vibeyclient.avatar.render.SkinOverride;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;
import org.joml.Quaternionf;
import org.joml.Vector3f;
//? if >=1.21.9 {
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.PlayerSkin;
//?} else if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.PlayerSkin;
*///?} else
/*import net.minecraft.client.resources.PlayerSkin;*/

/**
 * One preview of the local player: its own {@link AvatarState} (equipped items + try-on overrides, preview emote)
 * drawn through vanilla's player renderer (so skin layers, cape physics, our layer and the emote mixins all apply)
 * via a synthetic render state, without any player entity. From 1.21.6 on the GUI draws 3D through vanilla's
 * entity picture-in-picture renderer.
 */
final class PreviewView implements AvatarPreview.View {
    private final AvatarState st = new AvatarState(null, true);
    //? if >=1.21.9 {
    private final AvatarRenderState rs = new AvatarRenderState();
    //?} else if >=1.21.2
    /*private final PlayerRenderState rs = new PlayerRenderState();*/
    private java.util.function.Supplier<PlayerSkin> skinLookup;
    private float yaw = 0f, pitch = 0f;
    private boolean auto;
    private double lastFrame;
    // last drawn rect + drag
    private int rx, ry, rw, rh;
    private boolean dragging;
    private double dragX;
    private float dragStartYaw;

    PreviewView() {
        st.petInit = true;
    }

    @Override
    public void draw(Gfx g, int x, int y, int w, int h, float delta) {
        rx = x;
        ry = y;
        rw = w;
        rh = h;
        double now = Clock.now();
        float dt = lastFrame == 0 ? 0 : (float) Math.min(0.1, now - lastFrame);
        lastFrame = now;
        if (auto && !dragging) yaw += dt * 18f;
        syncLoadout();
        AvatarStates.tickEmote(st, now);
        if (st.emote != null && !st.emote.loops() && st.emote.finished(now)) st.emote = null;
        st.hidden = false;
        st.partial = delta;
        st.x = st.y = st.z = 0;
        st.bodyYaw = 180f + yaw;
        st.walk = st.fly = st.sneak = 0;
        boolean pet = AvatarDraw.bundle(st, Slot.PET) != null;
        placePet(now);
        float size = Math.min(h / 2.3f, w / (pet ? 3.1f : 1.6f));
        try {
            render(g, x, y, w, h, size, now);
        } catch (RuntimeException e) {
            dev.henny.vibeyclient.Vibey.LOG.debug("Preview failed: {}", e.toString());
        }
    }

    /** Equipped loadout of the local player + overrides. */
    private void syncLoadout() {
        AvatarState self = AvatarStates.peek(EngineCompat.selfId());
        for (Slot s : Slot.all()) st.slots[s.ordinal()] = self == null ? null : self.slots[s.ordinal()];
    }

    /** The pet stands to the player's right, turning with the preview. */
    private void placePet(double now) {
        double a = Math.toRadians(st.bodyYaw);
        double rxw = -Math.cos(a), rzw = -Math.sin(a), fx = -Math.sin(a), fz = Math.cos(a);
        st.petX = st.petPrevX = rxw * 0.95 - fx * 0.2;
        st.petZ = st.petPrevZ = rzw * 0.95 - fz * 0.2;
        st.petY = st.petPrevY = 0;
        st.petYaw = st.petPrevYaw = st.bodyYaw;
        st.petMove = 0;
    }

    private PlayerSkin skin() {
        Minecraft mc = Minecraft.getInstance();
        PlayerSkin base;
        if (mc.player != null) {
            base = mc.player.getSkin();
        } else {
            //? if >=1.21.9 {
            if (skinLookup == null) skinLookup = mc.getSkinManager().createLookup(mc.getGameProfile(), false);
            base = skinLookup.get();
            //?} else {
            /*if (skinLookup == null) skinLookup = mc.getSkinManager().lookupInsecure(mc.getGameProfile());
            base = skinLookup.get();
            *///?}
        }
        PlayerSkin patched = SkinOverride.patch(st, base);
        if (patched != null) return patched;
        // in a world the player's skin may carry the equipped cape: show the preview's own choice
        //? if >=1.21.9 {
        if (base.cape() != null && AvatarDraw.bundle(st, Slot.CAPE) == null && mc.player != null && isOurs(base.cape().texturePath())) {
            return new PlayerSkin(base.body(), null, null, base.model(), base.secure());
        }
        //?}
        return base;
    }

    private static boolean isOurs(Object id) {
        return id != null && id.toString().startsWith("vibeyclient:");
    }

    private void render(Gfx g, int x, int y, int w, int h, float size, double now) {
        //? if >=1.21.2 {
        PlayerSkin skin = skin();
        rs.skin = skin;
        rs.bodyRot = 180f + yaw;
        rs.yRot = 0f;
        rs.xRot = pitch;
        rs.ageInTicks = (float) (now * 20.0);
        rs.walkAnimationPos = 0f;
        rs.walkAnimationSpeed = 0f;
        rs.scale = 1f;
        rs.boundingBoxWidth = 0.6f;
        rs.boundingBoxHeight = 1.8f;
        rs.isInvisible = false;
        rs.showCape = true;
        rs.capeFlap = 0f;
        rs.capeLean = 0f;
        rs.capeLean2 = 0f;
        rs.nameTag = null;
        ((dev.henny.vibeyclient.avatar.render.AvatarCarrier) rs).vibey$setAvatar(st);
        //?}
        //? if >=1.21.9 {
        rs.lightCoords = 0xF000F0;
        rs.outlineColor = 0;
        rs.shadowPieces.clear();
        //?}
        //? if >=1.21.6 {
        //? if >=26.2 {
        /*rs.entityType = net.minecraft.world.entity.EntityTypes.PLAYER;
        *///?} else
        rs.entityType = net.minecraft.world.entity.EntityType.PLAYER;
        Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cam = new Quaternionf().rotateX(0f);
        rot.mul(cam);
        Vector3f translate = new Vector3f(0f, 1.8f / 2f + 0.06f, 0f);
        //? if >=26.1 {
        /*g.g.entity(rs, size, translate, rot, cam, x, y, x + w, y + h);
        *///?} else
        g.g.submitEntityRenderState(rs, size, translate, rot, cam, x, y, x + w, y + h);
        //?} else if >=1.21.2 {
        /*PreviewLegacy.render(g, x, y, w, h, size, rs, skin);
        *///?} else {
        /*PreviewLegacy.render(g, x, y, w, h, size, yaw, st, skin(), now);
        *///?}
    }

    // ============================================================================================ View

    @Override
    public float yaw() {
        return yaw;
    }

    @Override
    public void setYaw(float degrees) {
        yaw = degrees;
    }

    @Override
    public void setAutoRotate(boolean on) {
        auto = on;
    }

    @Override
    public boolean mouse(AvatarPreview.MouseAction action, double x, double y) {
        switch (action) {
            case PRESS -> {
                if (x >= rx && x < rx + rw && y >= ry && y < ry + rh) {
                    dragging = true;
                    dragX = x;
                    dragStartYaw = yaw;
                    return true;
                }
                return false;
            }
            case DRAG -> {
                if (!dragging) return false;
                yaw = dragStartYaw - (float) (x - dragX) * 0.9f;
                return true;
            }
            default -> {
                boolean was = dragging;
                dragging = false;
                return was;
            }
        }
    }

    @Override
    public void playEmote(String emoteId) {
        AvatarStates.play(st, emoteId);
    }

    @Override
    public void stopEmote() {
        AvatarStates.stop(st);
    }

    @Override
    public String emote() {
        return st.emote == null || st.emote.stopping() ? null : st.emote.id;
    }

    @Override
    public void override(Slot slot, String itemId) {
        st.overridden[slot.ordinal()] = true;
        st.overrides[slot.ordinal()] = itemId;
    }

    @Override
    public void clearOverrides() {
        java.util.Arrays.fill(st.overridden, false);
        java.util.Arrays.fill(st.overrides, null);
    }
}
