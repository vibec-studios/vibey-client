package dev.henny.vibeyclient.module.cosmetics;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;

/**
 * Capes, wings, auras, pets, shields, hats and back items (.vibey bundles).
 * Stub from the core skeleton: the module owner fills it in. Mixins go to {@code module/cosmetics/mixin} and are listed in
 * {@code vibeyclient.cosmetics.mixins.json}.
 */
public final class CosmeticsModule extends VibeyModule {

    public final BoolSetting showOwn = bool("showOwn", true);
    public final BoolSetting showOthers = bool("showOthers", true);
    public final BoolSetting hideWithArmor = bool("hideWithArmor", true);

    public CosmeticsModule() {
        super("cosmetics", ModuleCategory.COSMETICS);
    }

    @Override
    public int accentColor() {
        return 0xFF8B6CFF;
    }
}
