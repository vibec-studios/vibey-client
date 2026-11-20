package dev.henny.vibeyclient.module.visuals.items.swing;

import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.module.visuals.items.ItemRules;
import dev.henny.vibeyclient.module.visuals.items.core.SwingStyle;
import dev.henny.vibeyclient.module.visuals.items.core.SwingTiming;

public final class SwingSettings {
    private static final String LABELS = "visuals.items.setting.";

    public final IntSetting speed;
    public final EnumSetting<SwingStyle> style;
    public final BoolSetting noEquipBob;
    public final BoolSetting hideEmptyHand;

    public SwingSettings(SettingsGroup group) {
        speed = group.add(new IntSetting("swingSpeed", 100, SwingTiming.MIN_PERCENT, SwingTiming.MAX_PERCENT, 5)).unit("%")
                .labelKey(LABELS + "swingSpeed").section("swing");
        style = group.add(new EnumSetting<>("swingStyle", SwingStyle.VANILLA)).labelKey(LABELS + "swingStyle").section("swing");
        noEquipBob = group.add(new BoolSetting("noEquipBob", false)).labelKey(LABELS + "noEquipBob").section("swing");
        hideEmptyHand = group.add(new BoolSetting("hideEmptyHand", false)).labelKey(LABELS + "hideEmptyHand")
                .section("swing").serverRule(ItemRules.HELD);
    }
}
