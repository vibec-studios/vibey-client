package dev.henny.vibeyclient.module.emotes;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.mc.compat.McSelf;
import dev.henny.cosmetico.mc.tick.LocalEmotes;
import dev.henny.cosmetico.state.EmoteListener;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.FloatSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.client.PhysicalKeys;
import dev.henny.vibeyclient.module.cosmetics.CosmeticsModule;
import dev.henny.vibeyclient.module.cosmetics.EmoteHook;
import dev.henny.vibeyclient.module.cosmetics.LoadoutSync;
import dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi;
import dev.henny.vibeyclient.ui.ServerBadge;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.UUID;

public final class EmotesModule extends VibeyModule {

    public final KeybindSetting wheel = keybind("wheel", Keys.B).onPress(this::openWheel).serverRule(RULE);
    public final BoolSetting thirdPerson = bool("thirdPerson", true);
    public final FloatSetting volume = floatRange("volume", 1f, 0f, 1f, 0.05f);
    public final BoolSetting showOthers = bool("showOthers", true).serverRule(RULE);

    public static final String RULE = "emotes";

    private long blockedToastAt;

    public EmotesModule() {
        super("emotes", ModuleCategory.COSMETICS);
    }

    @Override
    public int accentColor() {
        return 0xFFFFB547;
    }

    @Override
    public List<String> serverRuleKeys() {
        return List.of(RULE);
    }

    @Override
    public void onLoad() {
        ServerRules.supports(RULE);
        ServerRules.listen(() -> {
            boolean off = ServerRules.isDisabled(RULE);
            Cosmetico.config().emotesServerOff = off;
            if (off && isEnabled()) stop();
        });
        volume.onChange(v -> Cosmetico.config().emoteVolume = v);
        thirdPerson.onChange(v -> Cosmetico.config().thirdPersonEmotes = v);
        showOthers.onChange(v -> Cosmetico.config().showOthersEmotes = v);
        on(VibeyEvents.RemoteEmote.class, e -> {
            if (e.play()) {
                CosmeticsModule c = CosmeticsModule.get();
                if (c == null ? !LoadoutSync.isDefault(e.emoteId()) : !c.sync().allowServerEmote(e.player(), e.emoteId())) return;
            }
            remote(e.player(), e.play() ? e.emoteId() : null);
        });
        Cosmetico.emotes().listen(new Broadcast());
    }

    @Override
    public void onEnable() {
        Cosmetico.config().emoteVolume = volume.get();
        Cosmetico.config().thirdPersonEmotes = thirdPerson.get();
        Cosmetico.config().emotes = true;
        Cosmetico.config().showOthersEmotes = showOthers.get();
        EmoteHook.play = this::play;
        EmoteHook.stop = this::stop;
        CosmeticsModule c = CosmeticsModule.get();
        if (c != null) c.sync().remoteEmote = this::remote;
    }

    @Override
    public void onDisable() {
        stop();
        Cosmetico.config().emotes = false;
        EmoteHook.play = id -> {};
        EmoteHook.stop = () -> {};
    }

    @Override
    public void onWorldLeave() {
        LocalEmotes.forgetCamera();
    }

    private void openWheel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ClientCompat.screen() != null) return;
        if (blockedByServer()) return;
        WardrobeUi.openWheel(() -> PhysicalKeys.isDown(wheel.mapping()));
    }

    public void play(String emoteId) {
        if (Minecraft.getInstance().player == null || emoteId == null || blockedByServer()) return;
        if (!LocalEmotes.play(emoteId)) log.info("Emote {} is not loaded", emoteId);
    }

    public void stop() {
        LocalEmotes.stop();
    }

    private boolean blockedByServer() {
        if (!ServerRules.isDisabled(RULE)) return false;
        long now = System.currentTimeMillis();
        if (now - blockedToastAt > 5_000) {
            blockedToastAt = now;
            Notify.info(Lang.tr("serverrules.emotesOff"), ServerBadge.label());
        }
        return true;
    }

    private void remote(UUID player, String emoteId) {
        if (player == null || player.equals(McSelf.id())) return;
        if (emoteId == null) Cosmetico.emotes().stop(player);
        else Cosmetico.emotes().play(player, emoteId);
    }

    private static final class Broadcast implements EmoteListener {
        @Override
        public void started(UUID player, String emoteId) {
            send(player, emoteId, true);
        }

        @Override
        public void stopped(UUID player, String emoteId) {
            send(player, emoteId, false);
        }

        private static void send(UUID player, String emoteId, boolean play) {
            Minecraft mc = Minecraft.getInstance();
            if (player == null || mc.player == null || !player.equals(mc.player.getUUID())) return;
            CosmeticsModule c = CosmeticsModule.get();
            if (c != null) c.broadcastEmote(emoteId, play);
        }
    }
}
