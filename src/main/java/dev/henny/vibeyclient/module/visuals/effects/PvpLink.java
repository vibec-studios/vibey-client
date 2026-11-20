package dev.henny.vibeyclient.module.visuals.effects;

import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Setting;

public final class PvpLink {
    public static final String MODULE = "pvp";

    private PvpLink() {}

    public static VibeyModule module() {
        ModuleManager mm = ModuleManager.get();
        return mm == null ? null : mm.get(MODULE);
    }

    public static Setting<?, ?> setting(String key) {
        VibeyModule m = module();
        return m == null ? null : m.settings().get(key);
    }

    public static boolean on() {
        VibeyModule m = module();
        return m != null && m.isEnabled();
    }

    public static void enable() {
        ModuleManager mm = ModuleManager.get();
        if (mm != null) mm.setEnabled(MODULE, true);
    }
}
