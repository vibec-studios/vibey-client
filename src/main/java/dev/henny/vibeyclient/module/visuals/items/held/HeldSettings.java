package dev.henny.vibeyclient.module.visuals.items.held;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.setting.OptionSetting;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.visuals.items.ItemRules;
import dev.henny.vibeyclient.module.visuals.items.core.HandPose;
import dev.henny.vibeyclient.module.visuals.items.core.HandPreset;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class HeldSettings {
    public static final String CUSTOM = "custom", MAIN = "main", OFF = "off";

    public final ActionSetting edit;
    public final KeybindSetting editKey;
    public final OptionSetting preset;
    public final OptionSetting hand;
    public final HandSettings main;
    public final HandSettings off;
    private String shown = MAIN;

    public HeldSettings(SettingsGroup group, Runnable openEditor, BooleanSupplier enabled) {
        edit = group.add(new ActionSetting("heldEdit", openEditor)).labelKey(HandSettings.LABELS + "heldEdit")
                .section("held").serverRule(ItemRules.HELD);
        preset = group.add(new OptionSetting("heldPreset", this::presetId, this::applyPreset, HeldSettings::presets))
                .labelKey(HandSettings.LABELS + "heldPreset").section("held").serverRule(ItemRules.HELD);
        hand = group.add(new OptionSetting("heldHand", () -> shown, v -> shown = OFF.equals(v) ? OFF : MAIN, HeldSettings::hands))
                .labelKey(HandSettings.LABELS + "heldHand").section("held");
        main = new HandSettings(group, MAIN, () -> MAIN.equals(shown));
        off = new HandSettings(group, OFF, () -> OFF.equals(shown));
        editKey = group.add(new KeybindSetting(group.id(), "itemEditor", Keys.UNBOUND)).onPress(openEditor)
                .labelKey(HandSettings.LABELS + "itemEditor").section("held");
        editKey.setActive(enabled);
    }

    public HandSettings of(boolean mainHand) {
        return mainHand ? main : off;
    }

    private String presetId() {
        HandPreset a = HandPreset.match(main.pose()), b = HandPreset.match(off.pose());
        return a != null && a == b ? a.id() : CUSTOM;
    }

    private void applyPreset(String id) {
        HandPreset p = HandPreset.byId(id);
        if (p == null) return;
        main.set(p.pose());
        off.set(p.pose());
    }

    public void save(HandPose mainPose, HandPose offPose) {
        main.set(mainPose);
        off.set(offPose);
    }

    private static List<OptionSetting.Choice> presets() {
        List<OptionSetting.Choice> out = new ArrayList<>();
        for (HandPreset p : HandPreset.values()) out.add(new OptionSetting.Choice(p.id(), Lang.tr("visuals.items.preset." + p.id())));
        out.add(new OptionSetting.Choice(CUSTOM, Lang.tr("visuals.items.preset.custom")));
        return out;
    }

    private static List<OptionSetting.Choice> hands() {
        return List.of(new OptionSetting.Choice(MAIN, Lang.tr("visuals.items.hand.main")),
                new OptionSetting.Choice(OFF, Lang.tr("visuals.items.hand.off")));
    }
}
