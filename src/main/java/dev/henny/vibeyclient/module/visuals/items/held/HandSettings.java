package dev.henny.vibeyclient.module.visuals.items.held;

import dev.henny.vibeyclient.api.setting.Settings.FloatSetting;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.module.visuals.items.ItemRules;
import dev.henny.vibeyclient.module.visuals.items.core.HandPose;

import java.util.List;
import java.util.function.BooleanSupplier;

public final class HandSettings {
    static final String LABELS = "visuals.items.setting.";

    private final FloatSetting x, y, z, rotX, rotY, rotZ, scale;
    private volatile HandPose cached;

    HandSettings(SettingsGroup group, String hand, BooleanSupplier shown) {
        x = offset(group, hand, "X", "x", shown);
        y = offset(group, hand, "Y", "y", shown);
        z = offset(group, hand, "Z", "z", shown);
        rotX = rotation(group, hand, "RotX", "rotX", shown);
        rotY = rotation(group, hand, "RotY", "rotY", shown);
        rotZ = rotation(group, hand, "RotZ", "rotZ", shown);
        scale = add(group, new FloatSetting(hand + "Scale", 1f, HandPose.MIN_SCALE, HandPose.MAX_SCALE, 0.01f)
                .decimals(2), "scale", shown);
    }

    private FloatSetting offset(SettingsGroup g, String hand, String suffix, String label, BooleanSupplier shown) {
        return add(g, new FloatSetting(hand + suffix, 0f, -HandPose.MAX_OFFSET, HandPose.MAX_OFFSET, 0.005f).decimals(3), label, shown);
    }

    private FloatSetting rotation(SettingsGroup g, String hand, String suffix, String label, BooleanSupplier shown) {
        return add(g, new FloatSetting(hand + suffix, 0f, -HandPose.MAX_ROTATION, HandPose.MAX_ROTATION, 1f).decimals(0), label, shown);
    }

    private FloatSetting add(SettingsGroup g, FloatSetting s, String label, BooleanSupplier shown) {
        g.add(s).labelKey(LABELS + label).section("held").serverRule(ItemRules.HELD).visibleWhen(shown);
        s.onChange(v -> cached = null);
        return s;
    }

    public HandPose pose() {
        HandPose p = cached;
        if (p == null) {
            p = new HandPose(x.get(), y.get(), z.get(), rotX.get(), rotY.get(), rotZ.get(), scale.get());
            cached = p;
        }
        return p;
    }

    public void set(HandPose p) {
        x.set(p.x());
        y.set(p.y());
        z.set(p.z());
        rotX.set(p.rotX());
        rotY.set(p.rotY());
        rotZ.set(p.rotZ());
        scale.set(p.scale());
        cached = null;
    }

    List<FloatSetting> all() {
        return List.of(x, y, z, rotX, rotY, rotZ, scale);
    }
}
