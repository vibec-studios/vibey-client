package dev.henny.vibeyclient.module.cinematic;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.FloatSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.api.setting.Settings.StringSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.cinematic.afk.AfkEditor;
import dev.henny.vibeyclient.module.cinematic.afk.AfkLayout;
import dev.henny.vibeyclient.module.cinematic.core.plan.Mode;
import dev.henny.vibeyclient.module.cinematic.run.AfkWatch;
import dev.henny.vibeyclient.module.cinematic.run.Cinematic;

import java.util.List;

public final class CinematicModule extends VibeyModule {
    private static volatile CinematicModule instance;

    public final ActionSetting start = action("start", () -> Cinematic.startFromMenu());
    public final EnumSetting<Mode> mode = choice("mode", Mode.MIX).icons(ModeIcons::of).serverRule(CinematicRules.AFK);
    public final BoolSetting auto = bool("auto", true).serverRule(CinematicRules.AFK);
    public final IntSetting afkMinutes = intRange("afkMinutes", 3, 1, 30).unit(" min").visibleWhen(this::autoOn);
    public final FloatSetting speed = floatRange("speed", 1f, 0.5f, 2f, 0.1f).decimals(1);
    public final IntSetting radius = intRange("radius", 32, 12, CinematicRules.MAX_RADIUS);
    public final IntSetting mixSeconds = intRange("mixSeconds", 40, 15, 120).unit(" s").visibleWhen(this::mixOn);
    public final ActionSetting editAfk = action("editAfk", () -> AfkEditor.open(ClientCompat.screen()));

    public final BoolSetting letterbox = bool("letterbox", true).section("look");
    public final BoolSetting vignette = bool("vignette", true).section("look");
    public final BoolSetting grain = bool("grain", false).section("look");
    public final StringSetting title = string("title", "", 40).section("look");

    public final KeybindSetting startKey = keybind("startKey", Keys.F6).serverRule(CinematicRules.AFK)
            .onPress(Cinematic::toggleFromKey);

    public CinematicModule() {
        super("cinematic", ModuleCategory.MISC);
        instance = this;
    }

    public static CinematicModule get() {
        return instance;
    }

    @Override
    public int accentColor() {
        return 0xFFFFB86B;
    }

    @Override
    public List<String> serverRuleKeys() {
        return List.of(CinematicRules.AFK);
    }

    @Override
    public void onLoad() {
        CinematicRules.declare();
        ServerRules.listen(Cinematic::rulesChanged);
        AfkLayout.register();
        mode.onChange(Cinematic::modeChanged);
    }

    @Override
    public void onDisable() {
        Cinematic.abort();
    }

    @Override
    public void onWorldLeave() {
        Cinematic.abort();
    }

    @Override
    public void tick() {
        AfkWatch.tick(this);
        Cinematic.tick();
    }

    private boolean autoOn() {
        return auto.on();
    }

    private boolean mixOn() {
        return mode.get() == Mode.MIX;
    }

    public boolean allowed() {
        return isEnabled() && !CinematicRules.blocked();
    }
}
