package dev.henny.vibeyclient.module.emotes;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.FloatSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.avatar.AvatarConfig;
import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.AvatarStates;
import dev.henny.vibeyclient.avatar.EmoteBridge;
import dev.henny.vibeyclient.avatar.EngineCompat;
import dev.henny.vibeyclient.avatar.anim.EmotePlay;
import dev.henny.vibeyclient.avatar.fx.Effects;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.cosmetics.CosmeticsModule;
import dev.henny.vibeyclient.module.cosmetics.UiBridge;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import java.util.UUID;

/**
 * Emote wheel and emotes with props, particles and sounds. Plays emotes on the local player (third person while
 * playing, stop on movement), forwards them to others and plays theirs. The pose/props rendering lives in the engine
 * ({@code avatar}).
 */
public final class EmotesModule extends VibeyModule {

    public final KeybindSetting wheel = keybind("wheel", Keys.B).onPress(this::openWheel);
    public final BoolSetting thirdPerson = bool("thirdPerson", true);
    public final FloatSetting volume = floatRange("volume", 1f, 0f, 1f, 0.05f);
    public final BoolSetting showOthers = bool("showOthers", true);

    /** Camera before the emote switched it (restored afterwards), null if we did not switch. */
    private CameraType cameraBefore;
    private CameraType cameraSet;
    private double startX, startZ;
    private int playingTicks;

    public EmotesModule() {
        super("emotes", ModuleCategory.COSMETICS);
    }

    @Override
    public int accentColor() {
        return 0xFFFFB547;
    }

    @Override
    public void onLoad() {
        volume.onChange(v -> Effects.volume = v);
        showOthers.onChange(v -> AvatarConfig.showOthersEmotes = v);
        on(VibeyEvents.RemoteEmote.class, e -> remote(e.player(), e.play() ? e.emoteId() : null));
        AvatarStates.onEmoteEnded = st -> {
            if (st.uuid != null && st.uuid.equals(EngineCompat.selfId())) restoreCamera();
        };
    }

    @Override
    public void onEnable() {
        Effects.volume = volume.get();
        AvatarConfig.emotes = true;
        AvatarConfig.showOthersEmotes = showOthers.get();
        EmoteBridge.play = this::play;
        EmoteBridge.stop = this::stop;
        CosmeticsModule c = CosmeticsModule.get();
        if (c != null) c.sync().remoteEmote = this::remote;
    }

    @Override
    public void onDisable() {
        stop();
        AvatarConfig.emotes = false;
        EmoteBridge.play = id -> {};
        EmoteBridge.stop = () -> {};
    }

    @Override
    public void onWorldLeave() {
        cameraBefore = null;
        cameraSet = null;
    }

    private void openWheel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ClientCompat.screen() != null) return;
        UiBridge.openWheel(() -> EngineCompat.isPhysicallyDown(wheel.mapping()));
    }

    // ============================================================================================ local

    public void play(String emoteId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || emoteId == null) return;
        AvatarState st = AvatarStates.get(mc.player.getUUID());
        if (!AvatarStates.play(st, emoteId)) {
            log.info("Emote {} is not loaded", emoteId);
            return;
        }
        startX = mc.player.getX();
        startZ = mc.player.getZ();
        playingTicks = 0;
        EmotePlay e = st.emote;
        if (thirdPerson.get() && e.bundle.emote != null && e.bundle.emote.thirdPerson()
                && EngineCompat.camera() == CameraType.FIRST_PERSON) {
            cameraBefore = CameraType.FIRST_PERSON;
            cameraSet = CameraType.THIRD_PERSON_BACK;
            EngineCompat.setCamera(cameraSet);
        }
        CosmeticsModule c = CosmeticsModule.get();
        if (c != null) c.broadcastEmote(emoteId, true);
    }

    public void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        AvatarState st = AvatarStates.peek(mc.player.getUUID());
        if (st == null || st.emote == null || st.emote.stopping()) return;
        String id = st.emote.id;
        AvatarStates.stop(st);
        CosmeticsModule c = CosmeticsModule.get();
        if (c != null) c.broadcastEmote(id, false);
    }

    private void restoreCamera() {
        if (cameraBefore != null && EngineCompat.camera() == cameraSet) EngineCompat.setCamera(cameraBefore);
        cameraBefore = null;
        cameraSet = null;
    }

    @Override
    public void tick() {
        dev.henny.vibeyclient.avatar.AvatarTicker.tick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        AvatarState st = AvatarStates.peek(mc.player.getUUID());
        if (st == null || st.emote == null || st.emote.stopping()) return;
        playingTicks++;
        EmotePlay e = st.emote;
        if (e.bundle.emote != null && e.bundle.emote.stopOnMove() && playingTicks > 4 && moving(mc)) stop();
    }

    private boolean moving(Minecraft mc) {
        Options o = mc.options;
        if (ClientCompat.screen() != null) return false;
        if (o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown() || o.keyJump.isDown()) return true;
        double dx = mc.player.getX() - startX, dz = mc.player.getZ() - startZ;
        return dx * dx + dz * dz > 0.25; // pushed away (water, pistons, knockback)
    }

    // ============================================================================================ remote

    private void remote(UUID player, String emoteId) {
        if (player == null || player.equals(EngineCompat.selfId())) return;
        AvatarState st = AvatarStates.get(player);
        if (emoteId == null) AvatarStates.stop(st);
        else AvatarStates.play(st, emoteId);
    }
}
