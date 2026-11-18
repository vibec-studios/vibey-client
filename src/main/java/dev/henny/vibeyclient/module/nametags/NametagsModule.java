package dev.henny.vibeyclient.module.nametags;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;

/**
 * A small seal before the names of players who run Vibey Client (in-world nametag and tab list), plus badge icons
 * (staff, supporter, creator) from the backend (VIBE-API-2 §5). Who runs Vibey comes from
 * {@link dev.henny.vibeyclient.client.VibeyPlayers} (cosmetics lookup + server channel). The icons are glyphs of
 * the own font {@code vibeyclient:icons}; the mixins only prepend a component to the vanilla name.
 */
public final class NametagsModule extends VibeyModule {
    static volatile NametagsModule instance;

    public final BoolSetting nametag = bool("nametag", true);
    public final BoolSetting tabList = bool("tabList", true);
    public final BoolSetting badges = bool("badges", true);

    public NametagsModule() {
        super("nametags", ModuleCategory.SOCIAL);
        instance = this;
    }

    @Override
    public int accentColor() {
        return 0xFF3FE0C5;
    }

    @Override
    public void onLoad() {
        nametag.onChange(v -> NameIcons.clearCaches());
        tabList.onChange(v -> NameIcons.clearCaches());
        badges.onChange(v -> NameIcons.clearCaches());
    }

    @Override
    public void onDisable() {
        NameIcons.clearCaches();
    }
}
