package dev.henny.vibeyclient.module.visuals.effects.screen;

import dev.henny.vibeyclient.ui.vui.settings.SettingRow;

public final class EffectItem {
    public final String id;
    public final SettingRow row;
    public final String test;

    EffectItem(SettingRow row, String test) {
        this.id = row.getId().replace('.', '-');
        this.row = row;
        this.test = test == null ? "" : test;
    }
}
