package dev.henny.vibeyclient.module.lyrics;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.lyrics.library.SongLibrary;
import dev.henny.vibeyclient.module.lyrics.render.HudLyrics;
import dev.henny.vibeyclient.module.lyrics.render.WorldLyrics;
import dev.henny.vibeyclient.module.lyrics.render.WorldSink;
import dev.henny.vibeyclient.module.lyrics.screen.LyricsScreen;
import dev.henny.vibeyclient.ui.Gfx;
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
 * Synced lyrics in the HUD, the sky and the world (port of Vibey Lyrics). Fabric only, like the standalone mod: this
 * package is not compiled for NeoForge and the module is not registered there. Conflicts with the mod "vibeylyrics":
 * then this module is off, registers no keybinds/hooks and its mixins (sky, fog, clouds) are skipped.
 *
 * <p>The lyrics screens edit {@link LyricsConfig}; the options shown in the Vibey menu are typed settings below that
 * are mirrored into it both ways. Songs live in {@code .minecraft/vibey-lyrics/} (shared with the standalone mod).
 */
public final class LyricsModule extends VibeyModule {
    public static final Logger LOG = LoggerFactory.getLogger("VibeyClient/lyrics");
    private static LyricsModule instance;

    public final ActionSetting openMenu = action("openMenu", () -> open(ClientCompat.screen()));
    public final EnumSetting<LyricsConfig.Position> position = choice("position", LyricsConfig.Position.SKY);
    public final EnumSetting<LyricsConfig.Preset> preset = choice("preset", LyricsConfig.Preset.LAVENDER);
    public final IntSetting volume = intRange("volume", 80, 0, 100, 5).unit("%");
    public final IntSetting offset = intRange("offsetMs", 0, -5000, 5000, 100).unit(" ms");
    public final BoolSetting hideInMenus = bool("hideInMenus", true);
    public final BoolSetting showNext = bool("showNext", true);
    public final BoolSetting pill = bool("pill", true);
    public final BoolSetting sparkles = bool("sparkles", true);
    public final BoolSetting muteMusic = bool("muteVanillaMusic", true);
    public final BoolSetting onlineLookup = bool("onlineLookup", true);
    public final BoolSetting pcMode = bool("pcMode", false);
    public final BoolSetting pauseButton = bool("pauseButton", false);
    public final KeybindSetting open = keybind("open", Keys.K).onPress(() -> {
        if (ClientCompat.screen() == null) open(null);
    });
    public final KeybindSetting play = keybind("play", Keys.UNBOUND).onPress(Playback::toggle);
    public final KeybindSetting earlier = keybind("earlier", Keys.UNBOUND).onPress(() -> shift(-100));
    public final KeybindSetting later = keybind("later", Keys.UNBOUND).onPress(() -> shift(100));
    public final KeybindSetting next = keybind("next", Keys.UNBOUND).onPress(Playback::next);
    public final KeybindSetting previous = keybind("previous", Keys.UNBOUND).onPress(Playback::previous);
    public final KeybindSetting hide = keybind("hide", Keys.UNBOUND).onPress(Playback::toggleHidden);
    public final BoolSetting hideClouds = bool("hideClouds", true).advanced();
    public final BoolSetting shootingStars = bool("shootingStars", true).advanced();
    public final BoolSetting skyOnTop = bool("skyOnTop", true).advanced();

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

    public LyricsModule() {
        super("lyrics", ModuleCategory.MEDIA);
        mirror(position, c -> c.position, (c, v) -> c.position = v);
        mirror(preset, c -> c.preset, (c, v) -> {
            if (c.preset != v) c.applyPreset(v);
        });
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

    /** Typed settings -> config. */
    void copyInto(LyricsConfig c) {
        for (Mirror<?> m : mirrors) m.into(c);
    }

    /** Config -> typed settings (after a lyrics screen changed something). */
    void copyFrom(LyricsConfig c) {
        for (Mirror<?> m : mirrors) m.from(c);
    }

    public static LyricsModule instance() {
        return instance;
    }

    /** The module is loaded and switched on (renderers and mixins check this). */
    public static boolean running() {
        LyricsModule m = instance;
        return m != null && m.isEnabled();
    }

    /** Songs and .lrc files: {@code .minecraft/vibey-lyrics/} (the same folder as the standalone mod). */
    public static Path songsDir() {
        return Vibey.platform().gameDir().resolve("vibey-lyrics");
    }

    /** Fabric only, like the standalone mod (this package is not even compiled for NeoForge). */
    @Override
    public boolean available() {
        return Vibey.platform().isFabric();
    }

    @Override
    public int accentColor() {
        return 0xFFFF5470;
    }

    // ============================================================================================ lifecycle

    @Override
    public void onLoad() {
        instance = this;
        LyricsConfig.load(this);
        registerHooks();
        on(VibeyEvents.ScreenInit.class, e -> {
            if (pauseButton.get()) LyricsPauseMenu.decorate(e.screen(), e.widgets(), e.addWidget());
        });
    }

    @Override
    public void onEnable() {
        SongLibrary.rescan();
        if (LyricsConfig.get().pcMode) Playback.setPc(true);
    }

    @Override
    public void onDisable() {
        Playback.suspend();
    }

    @Override
    public void tick() {
        Playback.tick(Minecraft.getInstance());
    }

    @Override
    public void renderHud(Gfx g) {
        HudLyrics.render(g);
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

    /** World text and shutdown; registered once, they only act while the module runs. */
    private void registerHooks() {
        if (hooksRegistered) return;
        hooksRegistered = true;
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> Playback.shutdown());
        // 1.21.9/1.21.10 have no Fabric world render event, see mixin/LevelRendererMixin.
        //? if >=26.1 {
        /*LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> WorldLyrics.render(ctx.poseStack(), new WorldSink(ctx.submitNodeCollector())));
        *///?}
        //? if >=1.21.11 <26.1 {
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> WorldLyrics.render(ctx.matrices(), new WorldSink(ctx.commandQueue())));
        //?}
        //? if <1.21.9 {
        /*WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            PoseStack pose = ctx.matrixStack() != null ? ctx.matrixStack() : new PoseStack();
            if (ctx.consumers() != null) WorldLyrics.render(pose, new WorldSink(ctx.consumers()));
        });
        *///?}
    }
}
