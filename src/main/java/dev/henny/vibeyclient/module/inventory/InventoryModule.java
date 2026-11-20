package dev.henny.vibeyclient.module.inventory;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.inventory.click.ClickSender;
import dev.henny.vibeyclient.module.inventory.core.ClickPacer;
import dev.henny.vibeyclient.module.inventory.core.PresetSet;
import dev.henny.vibeyclient.module.inventory.presets.PresetHud;
import dev.henny.vibeyclient.module.inventory.presets.PresetKeys;
import dev.henny.vibeyclient.module.inventory.presets.Presets;
import dev.henny.vibeyclient.module.inventory.screen.ScreenHooks;
import dev.henny.vibeyclient.module.inventory.tooltip.TooltipHooks;
import dev.henny.vibeyclient.ui.hud.HudLayout;

import java.util.List;

public final class InventoryModule extends VibeyModule {
    private static volatile InventoryModule instance;

    public final ActionSetting savePreset = action("savePreset", Presets::saveActive);
    public final ActionSetting editHud = action("editHud", () -> HudLayout.openEditor(ClientCompat.screen()));

    public final BoolSetting durability = bool("durability", true).section("tooltip");
    public final BoolSetting preview = bool("preview", true).section("tooltip");

    public final BoolSetting tweaks = bool("tweaks", false).section("tweaks").serverRule(InventoryRules.TWEAKS);
    public final BoolSetting dragMove = bool("dragMove", true).section("tweaks").serverRule(InventoryRules.TWEAKS)
            .visibleWhen(this::tweaksOn);
    public final BoolSetting dragMatching = bool("dragMatching", false).section("tweaks").serverRule(InventoryRules.TWEAKS)
            .visibleWhen(this::tweaksOn);
    public final BoolSetting wheelMove = bool("wheelMove", true).section("tweaks").serverRule(InventoryRules.TWEAKS)
            .visibleWhen(this::tweaksOn);
    public final BoolSetting wheelInvert = bool("wheelInvert", false).section("tweaks").serverRule(InventoryRules.TWEAKS)
            .visibleWhen(this::tweaksOn);
    public final IntSetting clickRate = intRange("clickRate", ClickPacer.DEFAULT_RATE, ClickPacer.MIN_RATE, ClickPacer.MAX_RATE)
            .unit("/s").section("tweaks").serverRule(InventoryRules.TWEAKS);

    public final BoolSetting search = bool("search", true).section("search");
    public final BoolSetting searchKeep = bool("searchKeep", false).section("search").visibleWhen(this::searchOn);

    public final BoolSetting presets = bool("presets", true).section("presets").serverRule(InventoryRules.PRESETS);
    public final IntSetting presetCount = intRange("presetCount", 3, PresetSet.MIN, PresetSet.MAX).section("presets")
            .serverRule(InventoryRules.PRESETS).visibleWhen(this::presetsOn);
    public final BoolSetting presetAlways = bool("presetAlways", false).section("presets")
            .serverRule(InventoryRules.PRESETS).visibleWhen(this::presetsOn);

    public final KeybindSetting presetKey = keybind("presetKey", Keys.R).serverRule(InventoryRules.PRESETS);
    public final KeybindSetting presetSaveKey = keybind("presetSaveKey", Keys.UNBOUND).serverRule(InventoryRules.PRESETS)
            .onPress(Presets::saveActive);

    public InventoryModule() {
        super("inventory", ModuleCategory.MISC);
        instance = this;
    }

    public static InventoryModule get() {
        return instance;
    }

    @Override
    public int accentColor() {
        return 0xFF6CC8FF;
    }

    @Override
    public List<String> serverRuleKeys() {
        return InventoryRules.ALL;
    }

    @Override
    public void onLoad() {
        for (String key : InventoryRules.ALL) ServerRules.supports(key);
        ServerRules.listen(ClickSender::clear);
        clickRate.onChange(ClickSender::rate);
        ClickSender.rate(clickRate.get());
        presetCount.onChange(n -> Presets.resize(n));
        Presets.load(this);
        TooltipHooks.register(this);
        ScreenHooks.register(this);
        PresetHud.register(this);
    }

    @Override
    public void onDisable() {
        ClickSender.clear();
        PresetKeys.reset();
    }

    @Override
    public void onWorldLeave() {
        ClickSender.clear();
        PresetKeys.reset();
    }

    @Override
    public void tick() {
        ClickSender.pump();
        PresetKeys.tick(this);
    }

    private boolean tweaksOn() {
        return tweaks.on();
    }

    private boolean searchOn() {
        return search.on();
    }

    private boolean presetsOn() {
        return presets.on();
    }

    public boolean tweaksActive() {
        return isEnabled() && tweaks.on() && !ServerRules.isDisabled(InventoryRules.TWEAKS);
    }

    public boolean presetsActive() {
        return isEnabled() && presets.on() && !ServerRules.isDisabled(InventoryRules.PRESETS);
    }

    public boolean searchActive() {
        return isEnabled() && search.on();
    }

    public boolean durabilityActive() {
        return isEnabled() && durability.on();
    }

    public boolean previewActive() {
        return isEnabled() && preview.on();
    }
}
