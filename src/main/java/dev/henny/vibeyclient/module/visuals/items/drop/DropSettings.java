package dev.henny.vibeyclient.module.visuals.items.drop;

import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.SettingsGroup;

public final class DropSettings {
    private static final String LABELS = "visuals.items.setting.";

    public final IntSetting dropScale;
    public final IntSetting frameScale;

    public DropSettings(SettingsGroup group) {
        dropScale = group.add(new IntSetting("dropScale", 100, 50, 300, 10)).unit("%").labelKey(LABELS + "dropScale").section("items");
        frameScale = group.add(new IntSetting("frameScale", 100, 50, 200, 10)).unit("%").labelKey(LABELS + "frameScale").section("items");
    }
}
