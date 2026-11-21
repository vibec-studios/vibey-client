package dev.henny.vibeyclient.module.clips;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.FloatSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.clips.encode.Encoder;
import dev.henny.vibeyclient.module.clips.shots.Shots;
import dev.henny.vibeyclient.module.clips.ui.ClipsGalleryScreen;
import dev.henny.vibeyclient.ui.Gfx;

import java.util.ArrayList;
import java.util.List;

/**
 * Clips (docs/CLIPS.md): a replay buffer that keeps the last N seconds of the game in RAM as encoded video and saves
 * them on a key press or automatically after a kill, plus a gallery to watch, keep, rename and delete them. The
 * buffer is off by default; while it runs it records only in game (not in menus, the pause screen, inventories
 * unless enabled, an unfocused or minimised window, or at low FPS). Encoding is FFmpeg with a hardware encoder.
 *
 * <p>Fabric 1.21.11 only for now: gradle/modules.gradle leaves the package (and its mixin config) out of every other
 * target and {@code BuiltinModules} registers it by reflection.
 */
public final class ClipsModule extends VibeyModule {
    /** Server-rule keys (SERVER-RULES.md): no recording at all / no automatic clips. */
    public static final String RULE_CLIPS = "clips", RULE_AUTO = "clips.auto";

    public enum Resolution {
        P480(852, 480), P720(1280, 720), P1080(1920, 1080);

        public final int width, height;

        Resolution(int w, int h) {
            width = w;
            height = h;
        }
    }

    public enum Rate {
        FPS30(30), FPS60(60);

        public final int fps;

        Rate(int fps) {
            this.fps = fps;
        }
    }

    public enum Quality { LOW, MEDIUM, HIGH, CUSTOM }

    public enum EncoderChoice {
        AUTO(null), NVENC(Encoder.NVENC), AMF(Encoder.AMF), QSV(Encoder.QSV), MF_HW(Encoder.MF_HW), MF_SW(Encoder.MF_SW),
        X264(Encoder.X264), OPENH264(Encoder.OPENH264);

        public final Encoder encoder;

        EncoderChoice(Encoder e) {
            encoder = e;
        }
    }

    private static volatile ClipsModule instance;
    final Recorder recorder = new Recorder(this);
    final AutoClips auto = new AutoClips(this);

    // ---------------------------------------------------------------------------------------- buttons (page header)
    public final ActionSetting openGallery = action("openGallery", () -> ClipsGalleryScreen.open(ClientCompat.screen()));
    public final ActionSetting openFolder = action("openFolder", () -> ClipsFiles.openFolder());
    public final ActionSetting openShots = action("openShots", Shots::openScreen);
    public final ActionSetting placeShots = action("placeShots", Shots::placePanel)
            .visibleWhen(() -> net.minecraft.client.Minecraft.getInstance().level != null);
    public final ActionSetting setupFfmpeg = action("setupFfmpeg", () -> recorder.setupFfmpeg())
            .visibleWhen(() -> this.buffer.on() && recorder.state() == Recorder.State.NEED_FFMPEG);

    // ---------------------------------------------------------------------------------------- replay buffer
    public final BoolSetting buffer = bool("buffer", false).section("buffer").serverRule(RULE_CLIPS);
    public final KeybindSetting saveKey = keybind("saveKey", Keys.F8).serverRule(RULE_CLIPS);
    public final IntSetting length = intRange("length", 30, 10, 120, 5).unit(" s").section("buffer").visibleWhen(buffer::on);
    public final BoolSetting longRewind = bool("longRewind", false).section("buffer").serverRule(RULE_CLIPS).visibleWhen(buffer::on);
    public final IntSetting rewindMinutes = intRange("rewindMinutes", 5, 1, 10).unit(" min").section("buffer")
            .visibleWhen(() -> buffer.on() && longRewind.on());
    public final KeybindSetting galleryKey = keybind("galleryKey", Keys.UNBOUND);
    public final KeybindSetting shotsKey = keybind("shotsKey", Keys.UNBOUND).onPress(Shots::openScreen);

    // ---------------------------------------------------------------------------------------- quality
    public final EnumSetting<Resolution> resolution = choice("resolution", Resolution.P720).section("quality");
    public final EnumSetting<Rate> rate = choice("rate", Rate.FPS30).section("quality");
    public final EnumSetting<Quality> quality = choice("quality", Quality.MEDIUM).section("quality");
    public final IntSetting customMbit = intRange("customMbit", 6, 1, 40).unit(" Mbit/s").section("quality")
            .visibleWhen(() -> quality.get() == Quality.CUSTOM);
    public final EnumSetting<EncoderChoice> encoder = choice("encoder", EncoderChoice.AUTO).section("quality");

    // ---------------------------------------------------------------------------------------- automatic clips
    public final BoolSetting autoClips = bool("autoClips", true).section("auto").serverRule(RULE_AUTO);
    public final BoolSetting autoKill = bool("autoKill", true).section("auto").serverRule(RULE_AUTO).visibleWhen(autoClips::on);
    public final BoolSetting autoMultiKill = bool("autoMultiKill", true).section("auto").serverRule(RULE_AUTO).visibleWhen(autoClips::on);
    public final BoolSetting autoClutch = bool("autoClutch", true).section("auto").serverRule(RULE_AUTO).visibleWhen(autoClips::on);
    public final BoolSetting autoDeath = bool("autoDeath", false).section("auto").serverRule(RULE_AUTO).visibleWhen(autoClips::on);
    public final IntSetting postRoll = intRange("postRoll", 3, 0, 10).unit(" s").section("auto").visibleWhen(autoClips::on);
    public final IntSetting maxPerSession = intRange("maxPerSession", 20, 1, 100).section("auto").visibleWhen(autoClips::on);

    // ---------------------------------------------------------------------------------------- saving resources
    public final BoolSetting lowFpsPause = bool("lowFpsPause", true).section("resources");
    public final BoolSetting fpsAuto = bool("fpsAuto", true).section("resources").visibleWhen(lowFpsPause::on);
    public final IntSetting fpsThreshold = intRange("fpsThreshold", 40, 20, 120, 5).section("resources")
            .visibleWhen(() -> lowFpsPause.on() && !fpsAuto.on());
    public final BoolSetting inInventories = bool("inInventories", false).section("resources");
    public final IntSetting ramCap = intRange("ramCap", 96, 32, 256, 16).unit(" MB").section("resources")
            .visibleWhen(() -> !longRewind.on());
    public final IntSetting diskCap = intRange("diskCap", 1024, 256, 8192, 256).unit(" MB").section("resources")
            .visibleWhen(longRewind::on);

    // ---------------------------------------------------------------------------------------- storage
    public final FloatSetting folderLimit = floatRange("folderLimit", 2f, 0.5f, 50f, 0.5f).decimals(1).section("storage");
    public final BoolSetting autoDelete = bool("autoDelete", true).section("storage");
    public final BoolSetting saveServer = bool("saveServer", true).section("storage");

    // ---------------------------------------------------------------------------------------- HUD
    public final BoolSetting indicator = bool("indicator", true).section("hud");

    // ---------------------------------------------------------------------------------------- advanced
    public final BoolSetting stats = bool("stats", Boolean.getBoolean("vibeyclient.clips.stats")).advanced();
    /** The first-time hint ("Clips show your screen including chat") was shown. */
    public final BoolSetting hintShown = bool("hintShown", false).advanced().visibleWhen(() -> false);

    public ClipsModule() {
        super("clips", ModuleCategory.MISC);
        instance = this;
    }

    /** The module while it exists (null on builds without it or before construction). */
    public static ClipsModule get() {
        return instance;
    }

    public Recorder recorder() {
        return recorder;
    }

    public AutoClips auto() {
        return auto;
    }

    @Override
    public int accentColor() {
        return 0xFFFF6B4A;
    }

    @Override
    public List<String> serverRuleKeys() {
        return List.of(RULE_CLIPS, RULE_AUTO);
    }

    /** The status line ("Recording · Intel Quick Sync · 26 MB RAM") and storage notices. */
    @Override
    public List<String> notices() {
        List<String> out = new ArrayList<>();
        String s = recorder.statusLine();
        if (s != null) out.add(s);
        String real = recorder.capNotice();
        if (real != null) out.add(real);
        if (ClipsFiles.keptOverLimit()) out.add(tr("notice.keptOverLimit"));
        return out;
    }

    @Override
    public void onLoad() {
        ServerRules.supports(RULE_CLIPS);
        ServerRules.supports(RULE_AUTO);
        ServerRules.listen(recorder::onRulesChanged);
        saveKey.onPress(() -> recorder.saveHotkey());
        galleryKey.onPress(() -> ClipsGalleryScreen.open(null));
        buffer.onChange(v -> {
            recorder.settingsChanged(true);
            recorder.bufferToggled(v);
        });
        resolution.onChange(v -> recorder.settingsChanged(true));
        rate.onChange(v -> recorder.settingsChanged(true));
        quality.onChange(v -> recorder.settingsChanged(true));
        customMbit.onChange(v -> recorder.settingsChanged(true));
        encoder.onChange(v -> recorder.settingsChanged(true));
        length.onChange(v -> recorder.settingsChanged(false));
        ramCap.onChange(v -> recorder.settingsChanged(false));
        rewindMinutes.onChange(v -> recorder.settingsChanged(false));
        diskCap.onChange(v -> recorder.settingsChanged(false));
        longRewind.onChange(v -> recorder.storageChanged());
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register(ClipsHooks::gameMessage);
        ClipsHud.register(this);
        ClipsFiles.startup(this);
    }

    @Override
    public void onEnable() {
        recorder.settingsChanged(true);
    }

    @Override
    public void onDisable() {
        recorder.stop(true);
        auto.reset();
    }

    @Override
    public void tick() {
        recorder.tick();
        auto.tick();
        Shots.tick();
    }

    @Override
    public void renderHud(Gfx g) {
        ClipsHud.render(g, this);
        if (stats.on()) ClipsStats.render(g, recorder);
    }

    @Override
    public void onWorldJoin() {
        auto.reset();
        recorder.worldJoined();
    }

    @Override
    public void onWorldLeave() {
        recorder.worldLeft();
        auto.reset();
        Shots.removePanel();
    }

    // ============================================================================================ effective values

    /** Recording is allowed here (module on, buffer on, not switched off by the server). */
    public boolean bufferActive() {
        return isEnabled() && buffer.on() && !ServerRules.isDisabled(RULE_CLIPS);
    }

    public boolean autoActive() {
        return bufferActive() && autoClips.on() && !ServerRules.isDisabled(RULE_AUTO);
    }

    public int bitrateKbps() {
        boolean hd = resolution.get() == Resolution.P1080;
        boolean fast = rate.get() == Rate.FPS60;
        int[] table = hd || fast ? new int[] {8000, 12000, 20000} : new int[] {3000, 6000, 10000};
        if (resolution.get() == Resolution.P480 && !fast) table = new int[] {1500, 3000, 5000};
        return switch (quality.get()) {
            case LOW -> table[0];
            case MEDIUM -> table[1];
            case HIGH -> table[2];
            case CUSTOM -> customMbit.get() * 1000;
        };
    }

    /** FPS below which recording pauses (auto: clip fps + 10). */
    public int pauseFps() {
        return fpsAuto.on() ? rate.get().fps + 10 : fpsThreshold.get();
    }

    public long folderLimitBytes() {
        return (long) (folderLimit.get() * 1024L * 1024L * 1024L);
    }

    /** The recording dot is wanted (the HUD system still hides it with F1 or a server rule). */
    boolean indicatorShown() {
        return isEnabled() && indicator.on() && buffer.on();
    }
}
