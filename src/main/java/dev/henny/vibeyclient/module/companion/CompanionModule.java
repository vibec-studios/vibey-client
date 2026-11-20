package dev.henny.vibeyclient.module.companion;

import dev.henny.companio.mc.CompanioMc;
import dev.henny.companio.world.Command;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.OptionSetting;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.FloatSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.api.setting.Settings.StringSetting;
import dev.henny.vibeyclient.client.Keys;

import java.util.List;

public final class CompanionModule extends VibeyModule {
    public static final String RULE_SHOW = "companion.show";

    private static volatile CompanionModule instance;

    public final ActionSetting packs = action("packs", CompanioMc::openPacks);
    public final ActionSetting recall = action("recall", CompanioMc::recall);
    public final OptionSetting companion = settings().add(new OptionSetting("companion", CompanionChoices::selected,
            CompanionChoices::select, () -> CompanionChoices.companions(this))).serverRule(RULE_SHOW);
    public final OptionSetting variant = settings().add(new OptionSetting("variant", CompanionChoices::variant,
            CompanionChoices::setVariant, () -> CompanionChoices.variants(this))).visibleWhen(CompanionChoices::hasVariants);
    public final StringSetting name = string("name", "", 24);
    public final FloatSetting size = floatRange("size", 1f, 0.2f, 4f, 0.05f).visibleWhen(CompanionChoices::sizeAdjustable);
    public final OptionSetting mode = settings().add(new OptionSetting("mode", CompanionChoices::command,
            v -> CompanioMc.command(Command.of(v)), () -> CompanionChoices.commands(this)));
    public final FloatSetting volume = floatRange("volume", 1f, 0f, 1f, 0.05f).section("feel");
    public final BoolSetting bubbles = bool("bubbles", true).section("feel");
    public final BoolSetting others = bool("others", true).section("feel");
    public final BoolSetting share = bool("share", true).section("feel");
    public final KeybindSetting menuKey = keybind("menuKey", Keys.UNBOUND).onPress(CompanioMc::openMenu);
    public final KeybindSetting packsKey = keybind("packsKey", Keys.UNBOUND).onPress(CompanioMc::openPacks);

    private final CompanionSync sync = new CompanionSync();
    private final SettingsBridge bridge = new SettingsBridge(this);

    public CompanionModule() {
        super("companion", ModuleCategory.MISC);
        instance = this;
    }

    public static CompanionModule get() {
        return instance;
    }

    @Override
    public String icon() {
        return null;
    }

    @Override
    public int accentColor() {
        return 0xFF7FD4FF;
    }

    @Override
    public List<String> serverRuleKeys() {
        return List.of(RULE_SHOW);
    }

    @Override
    public void onLoad() {
        ServerRules.supports(RULE_SHOW);
        sync.load();
        bridge.listen();
    }

    @Override
    public void onEnable() {
        CompanioMc.hidden(false);
        bridge.push();
    }

    @Override
    public void onDisable() {
        CompanioMc.hidden(true);
        CompanioMc.sync(null);
    }

    @Override
    public void tick() {
        if (!CompanioMc.ready()) return;
        CompanioMc.serverHidden(ServerRules.isDisabled(RULE_SHOW));
        bridge.pull();
        sync.tick(share.get());
    }

    @Override
    public void onWorldLeave() {
        sync.leave();
    }
}
