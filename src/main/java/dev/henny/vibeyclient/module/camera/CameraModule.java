package dev.henny.vibeyclient.module.camera;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.camera.blur.MotionBlur;
import dev.henny.vibeyclient.module.camera.core.BlurMath;
import dev.henny.vibeyclient.module.camera.look.CameraState;
import dev.henny.vibeyclient.module.camera.look.FreeLook;

import java.util.List;

public final class CameraModule extends VibeyModule {
    private static volatile CameraModule instance;

    public final BoolSetting freelook = bool("freelook", false).section("look").serverRule(CameraRules.FREELOOK);
    public final BoolSetting freelookToggle = bool("freelookToggle", false).section("look")
            .serverRule(CameraRules.FREELOOK).visibleWhen(this::freelookOn);
    public final BoolSetting orbit = bool("orbit", true).section("look").serverRule(CameraRules.ORBIT);

    public final BoolSetting motionBlur = bool("motionBlur", false).section("blur");
    public final IntSetting blurStrength = intRange("blurStrength", 4, BlurMath.MIN_STRENGTH, BlurMath.MAX_STRENGTH)
            .section("blur").visibleWhen(this::blurOn);

    public final KeybindSetting freelookKey = keybind("freelookKey", Keys.LEFT_ALT).serverRule(CameraRules.FREELOOK)
            .onPress(FreeLook::keyPressed);

    public CameraModule() {
        super("camera", ModuleCategory.MISC);
        instance = this;
    }

    public static CameraModule get() {
        return instance;
    }

    @Override
    public int accentColor() {
        return 0xFF7FD4FF;
    }

    @Override
    public List<String> serverRuleKeys() {
        return CameraRules.ALL;
    }

    @Override
    public List<String> notices() {
        return MotionBlur.unavailable() && motionBlur.on() ? List.of(Lang.tr("camera.notice.blur")) : List.of();
    }

    @Override
    public void onLoad() {
        CameraRules.declare();
        ServerRules.listen(CameraState::reset);
    }

    @Override
    public void onDisable() {
        CameraState.reset();
    }

    @Override
    public void onWorldLeave() {
        CameraState.reset();
    }

    private boolean freelookOn() {
        return freelook.on();
    }

    private boolean blurOn() {
        return motionBlur.on();
    }

    public boolean freelookActive() {
        return isEnabled() && freelook.on() && !CameraRules.freelookBlocked();
    }

    public boolean orbitActive() {
        return isEnabled() && orbit.on() && !CameraRules.orbitBlocked();
    }

    public boolean blurActive() {
        return isEnabled() && motionBlur.on();
    }
}
