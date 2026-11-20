package dev.henny.vibeyclient.module.media.lyrics;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.media.MediaFeature;
import dev.henny.vibeyclient.module.media.MediaModule;
import dev.henny.vibeyclient.module.media.lyrics.fx.MusicAnalyzer;
import dev.henny.vibeyclient.module.media.lyrics.library.SongLibrary;
import dev.henny.vibeyclient.module.media.lyrics.render.HudLyrics;
import dev.henny.vibeyclient.module.media.lyrics.render.NowPlayingBar;
import dev.henny.vibeyclient.module.media.lyrics.render.SkyHook;
import dev.henny.vibeyclient.module.media.lyrics.world.MusicPanel;
import dev.henny.vibeyclient.module.media.lyrics.world.WorldText;
import dev.henny.vibeyclient.module.media.lyrics.render.WorldSink;
import dev.henny.vibeyclient.module.media.lyrics.screen.LyricsScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
//? if >=26.1 {
/*import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
*///?}
//? if >=1.21.11 <26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
//?}
//? if <1.21.9 {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import com.mojang.blaze3d.vertex.PoseStack;
*///?}

/**
 * Lyrics, songs and skies – the part of Media ported from Vibey Lyrics: synced lyrics (LRCLIB, own .lrc files, the
 * music playing on the PC) in the sky, in front of you, pinned in the world or on the HUD, custom skies that react
 * to the music, and the now playing bar. Off on its own (keybinds not registered, sky/fog/cloud mixins skipped)
 * when the standalone mod "vibeylyrics" is installed; the rest of Media keeps running.
 *
 * <p>The lyrics screens edit {@link LyricsConfig}; the options shown in the Vibey menu are typed settings below that
 * are mirrored into it both ways. Songs live in {@code .minecraft/vibey-lyrics/} (shared with the standalone mod).
 */
public final class LyricsFeature extends MediaFeature {
    public static final Logger LOG = LoggerFactory.getLogger("VibeyClient/media/lyrics");
    private static LyricsFeature instance;

    // shown first: what most people change
    public final ActionSetting openMenu = action("openMenu", () -> open(ClientCompat.screen()));
    public final ActionSetting panel = action("panel", () -> {
        ClientCompat.openScreen(null);
        this.panelOn.set(true);
        MusicPanel.open();
    });
    public final BoolSetting panelOn = bool("panelOn", true);
    public final EnumSetting<LyricsConfig.Position> position = choice("position", LyricsConfig.Position.SKY);
    public final EnumSetting<LyricsConfig.Preset> preset = choice("preset", LyricsConfig.Preset.LAVENDER);
    public final BoolSetting skyFx = bool("skyFx", true);
    public final IntSetting fxStrength = intRange("fxStrength", 50, 0, 100, 10).unit("%");
    public final BoolSetting pcMode = bool("pcMode", false);
    public final IntSetting volume = intRange("volume", 80, 0, 100, 5).unit("%");
    // advanced
    public final EnumSetting<LyricsConfig.SkyMode> skyMode = choice("skyMode", LyricsConfig.SkyMode.VIEW).advanced();
    public final BoolSetting fxFlashes = bool("fxFlashes", false).advanced();
    public final BoolSetting fxPcAudio = bool("fxPcAudio", true).advanced();
    public final IntSetting offset = intRange("offsetMs", 0, -5000, 5000, 100).unit(" ms").advanced();
    public final BoolSetting hideInMenus = bool("hideInMenus", true).advanced();
    public final BoolSetting showNext = bool("showNext", true).advanced();
    public final BoolSetting pill = bool("pill", true).advanced();
    public final BoolSetting sparkles = bool("sparkles", true).advanced();
    public final BoolSetting muteMusic = bool("muteVanillaMusic", true).advanced();
    public final BoolSetting onlineLookup = bool("onlineLookup", true).advanced();
    public final BoolSetting hideClouds = bool("hideClouds", true).advanced();
    public final BoolSetting shootingStars = bool("shootingStars", true).advanced();
    public final BoolSetting skyOnTop = bool("skyOnTop", true).advanced();
    public final KeybindSetting open = keybind("open", Keys.K).onPress(() -> {
        if (ClientCompat.screen() == null) open(null);
    });
    public final KeybindSetting play = keybind("play", Keys.UNBOUND).onPress(Playback::toggle);
    public final KeybindSetting earlier = keybind("earlier", Keys.UNBOUND).onPress(() -> shift(-100));
    public final KeybindSetting later = keybind("later", Keys.UNBOUND).onPress(() -> shift(100));
    public final KeybindSetting next = keybind("next", Keys.UNBOUND).onPress(Playback::next);
    public final KeybindSetting previous = keybind("previous", Keys.UNBOUND).onPress(Playback::previous);
    public final KeybindSetting hide = keybind("hide", Keys.UNBOUND).onPress(Playback::toggleHidden);

    /** HUD lyrics (position "HUD"), movable in the HUD editor. */
    public final HudElement hudText = hudElement(HudElement.builder("text")
            .size(HudLyrics.BOX_W, HudLyrics.BOX_H).defaults(Anchor.CENTER, 0, -18).scale(1f, 0.5f, 2.5f)
            .visibleWhen(() -> LyricsConfig.get().position == LyricsConfig.Position.HUD)
            .render(HudLyrics::render));
    /** The now playing bar (top centre by default). */
    public final HudElement hudBar = hudElement(HudElement.builder("bar")
            .size(NowPlayingBar.BOX_W, NowPlayingBar.H).defaults(Anchor.TOP, 0, 4).scale(1f, 0.5f, 2.5f)
            .visibleWhen(() -> LyricsConfig.get().pill)
            .render(NowPlayingBar::render));

    /** Typed setting <-> config field. */
    private record Mirror<T>(Setting<T, ?> setting, Function<LyricsConfig, T> get, BiConsumer<LyricsConfig, T> set) {
        void into(LyricsConfig c) {
            set.accept(c, setting.get());
        }

        void from(LyricsConfig c) {
            setting.set(get.apply(c));
        }
    }

    private final List<Mirror<?>> mirrors = new ArrayList<>();
    private boolean hooksRegistered;

    public LyricsFeature(MediaModule media) {
        super(media, "lyrics");
        instance = this;
        mirror(position, c -> c.position, (c, v) -> c.position = v);
        mirror(preset, c -> c.preset, (c, v) -> {
            if (c.preset != v) c.applyPreset(v);
        });
        mirror(skyFx, c -> c.skyFx, (c, v) -> c.skyFx = v);
        mirror(fxStrength, c -> c.fxStrength, (c, v) -> c.fxStrength = v);
        mirror(fxFlashes, c -> c.fxFlashes, (c, v) -> c.fxFlashes = v);
        mirror(fxPcAudio, c -> c.fxPcAudio, (c, v) -> c.fxPcAudio = v);
        mirror(skyMode, c -> c.skyMode, (c, v) -> c.skyMode = v);
        mirror(volume, c -> c.volume, (c, v) -> c.volume = v);
        mirror(offset, c -> c.offsetMs, (c, v) -> c.offsetMs = v);
        mirror(hideInMenus, c -> c.hideInMenus, (c, v) -> c.hideInMenus = v);
        mirror(showNext, c -> c.showNext, (c, v) -> c.showNext = v);
        mirror(pill, c -> c.pill, (c, v) -> c.pill = v);
        mirror(sparkles, c -> c.sparkles, (c, v) -> c.sparkles = v);
        mirror(muteMusic, c -> c.muteVanillaMusic, (c, v) -> c.muteVanillaMusic = v);
        mirror(onlineLookup, c -> c.onlineLookup, (c, v) -> c.onlineLookup = v);
        mirror(pcMode, c -> c.pcMode, (c, v) -> c.pcMode = v);
        mirror(hideClouds, c -> c.hideClouds, (c, v) -> c.hideClouds = v);
        mirror(shootingStars, c -> c.shootingStars, (c, v) -> c.shootingStars = v);
        mirror(skyOnTop, c -> c.skyOnTop, (c, v) -> c.skyOnTop = v);
        fxStrength.visibleWhen(() -> usable() && skyFx.get());
        fxFlashes.visibleWhen(() -> usable() && skyFx.get());
        fxPcAudio.visibleWhen(() -> usable() && skyFx.get() && MusicAnalyzer.systemAudioSupported());
        fxPcAudio.onChange(v -> dev.henny.vibeyclient.module.media.lyrics.fx.LoopbackCapture.retry());
    }

    private <T> void mirror(Setting<T, ?> s, Function<LyricsConfig, T> get, BiConsumer<LyricsConfig, T> set) {
        Mirror<T> m = new Mirror<>(s, get, set);
        mirrors.add(m);
        // A change in the Vibey menu goes into the config (only if it really differs, so no loops).
        s.onChange(v -> {
            LyricsConfig c = LyricsConfig.get();
            if (java.util.Objects.equals(get.apply(c), v)) return;
            set.accept(c, v);
            if (s == pcMode && isEnabled()) Playback.setPc((Boolean) v);
            if (s == position && v == LyricsConfig.Position.WORLD && !c.anchorSet) LyricsScreen.placeHere();
            c.save();
        });
    }

    /** Typed settings -> config; settings the file did not have yet (new in this version) go the other way. */
    void copyInto(LyricsConfig c) {
        for (Mirror<?> m : mirrors) {
            if (media().settings().stored(m.setting().key())) m.into(c);
            else m.from(c);
        }
    }

    /** Config -> typed settings (after a lyrics screen changed something). */
    void copyFrom(LyricsConfig c) {
        for (Mirror<?> m : mirrors) m.from(c);
    }

    public static LyricsFeature instance() {
        return instance;
    }

    /** Media is on and this part runs (renderers and mixins check this). */
    public static boolean running() {
        LyricsFeature f = instance;
        return f != null && f.isEnabled() && f.media().isEnabled();
    }

    /** Songs and .lrc files: {@code .minecraft/vibey-lyrics/} (the same folder as the standalone mod). */
    public static Path songsDir() {
        return Vibey.platform().gameDir().resolve("vibey-lyrics");
    }

    // ============================================================================================ lifecycle

    @Override
    public void onLoad() {
        LyricsConfig.load(this);
        registerHooks();
        MusicPanel.hook();
        WorldText.hook();
    }

    @Override
    public void onEnable() {
        SongLibrary.rescan();
        if (LyricsConfig.get().pcMode) Playback.setPc(true);
    }

    @Override
    public void onDisable() {
        MusicPanel.sync(false);
        Playback.suspend();
        MusicAnalyzer.stopAll();
        WorldText.tick();
    }

    @Override
    public void tick() {
        Playback.tick(Minecraft.getInstance());
        MusicAnalyzer.tick();
        MusicPanel.sync(panelOn.get());
        MusicPanel.tick();
        WorldText.tick();
    }

    @Override
    public void renderHud(Gfx g) {
        HudLayout.draw(g, hudBar);
        if (LyricsConfig.get().position == LyricsConfig.Position.HUD) HudLayout.draw(g, hudText);
    }

    public void open(net.minecraft.client.gui.screens.Screen parent) {
        if (!isEnabled()) return;
        ClientCompat.openScreen(new LyricsScreen(parent));
    }

    private static void shift(int ms) {
        LyricsConfig cfg = LyricsConfig.get();
        cfg.offsetMs += ms;
        cfg.save();
    }

    /** World text and shutdown; registered once, they only act while the feature runs. */
    private void registerHooks() {
        if (hooksRegistered) return;
        hooksRegistered = true;
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            Playback.shutdown();
            MusicAnalyzer.stopAll();
        });
        // 1.21.9/1.21.10 have no Fabric world render event, see mixin/LevelRendererMixin.
        //? if >=26.1 {
        /*LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> SkyHook.render(ctx.poseStack(), new WorldSink(ctx.submitNodeCollector())));
        *///?}
        //? if >=1.21.11 <26.1 {
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> SkyHook.render(ctx.matrices(), new WorldSink(ctx.commandQueue())));
        //?}
        //? if <1.21.9 {
        /*WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            PoseStack pose = ctx.matrixStack() != null ? ctx.matrixStack() : new PoseStack();
            if (ctx.consumers() != null) SkyHook.render(pose, new WorldSink(ctx.consumers()));
        });
        *///?}
    }
}
