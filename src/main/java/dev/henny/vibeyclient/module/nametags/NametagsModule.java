package dev.henny.vibeyclient.module.nametags;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.ui.Panel;

public final class NametagsModule extends VibeyModule {
    static volatile NametagsModule instance;

    public final BoolSetting nametag = bool("nametag", true).section("show");
    public final BoolSetting tabList = bool("tabList", true).section("show");
    public final BoolSetting chat = bool("chat", true).section("show");
    public final BoolSetting ranks = bool("ranks", true).section("icons").serverRule(NametagRules.RANKS);
    public final BoolSetting badges = bool("badges", true).section("icons").serverRule(NametagRules.RANKS);
    public final BoolSetting nameStyles = bool("nameStyles", true).section("names").serverRule(NametagRules.STYLES);
    public final BoolSetting animate = bool("animate", true).section("names").serverRule(NametagRules.STYLES);

    public NametagsModule() {
        super("nametags", ModuleCategory.SOCIAL);
        instance = this;
        MyNameSettings.declare(this);
    }

    @Override
    public int accentColor() {
        return 0xFF3FE0C5;
    }

    @Override
    public void onLoad() {
        NametagRules.declare();
        for (BoolSetting s : new BoolSetting[] {nametag, tabList, chat, ranks, badges, nameStyles, animate}) {
            s.onChange(v -> NameIcons.clearCaches());
        }
        ServerRules.listen(NameIcons::clearCaches);
    }

    @Override
    public void tick() {
        AnimatedSet.update(Surface.NAMETAG.flags(this));
    }

    @Override
    public void onDisable() {
        NameIcons.clearCaches();
    }

    @Override
    public Panel createSettingsPanel() {
        return new NametagsPanel(this);
    }
}
