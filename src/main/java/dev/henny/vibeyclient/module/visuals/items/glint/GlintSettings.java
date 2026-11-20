package dev.henny.vibeyclient.module.visuals.items.glint;

import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.ColorSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.module.visuals.items.core.GlintTint;

public final class GlintSettings {
    private static final String LABELS = "visuals.items.setting.";

    public final IntSetting strength;
    public final IntSetting speed;
    public final BoolSetting tint;
    public final ColorSetting color;
    private volatile float[] modulator;

    public GlintSettings(SettingsGroup group) {
        strength = group.add(new IntSetting("glintStrength", 100, 0, 200, 10)).unit("%").labelKey(LABELS + "glintStrength").section("glint");
        speed = group.add(new IntSetting("glintSpeed", 100, 0, 400, 10)).unit("%").labelKey(LABELS + "glintSpeed").section("glint");
        tint = group.add(new BoolSetting("glintTint", false)).labelKey(LABELS + "glintTint").section("glint");
        color = group.add(new ColorSetting("glintColor", 0xFFFF4D6D)).labelKey(LABELS + "glintColor").section("glint")
                .presets(0xFFFF4D6D, 0xFFFFB000, 0xFF3DDC84, 0xFF2FD8FF, 0xFF4D7CFF, 0xFFFF6BD6, 0xFFFFFFFF)
                .visibleWhen(tint::on);
        strength.onChange(v -> modulator = null);
        tint.onChange(v -> modulator = null);
        color.onChange(v -> modulator = null);
    }

    float[] modulator() {
        float[] m = modulator;
        if (m == null) {
            m = GlintTint.modulator(tint.on(), color.argb(), strength.get() / 100f);
            modulator = m;
        }
        return m;
    }

    boolean changesColor() {
        return tint.on() || strength.get() != 100;
    }
}
