package dev.henny.vibeyclient.module.accessibility;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.ui.hud.HudPaint;

public final class AccessibilityModule extends VibeyModule {
    private final EnumSetting<ColorFilter> filter = choice("filter", ColorFilter.OFF);
    private final BoolSetting hud = bool("hud", false);
    private final EnumSetting<SubtitleStyle> subtitles = choice("subtitles", SubtitleStyle.VANILLA);

    public AccessibilityModule() {
        super("accessibility", ModuleCategory.CORE);
    }

    @Override
    public void onLoad() {
        HudPaint.highContrastWhen(() -> isEnabled() && hud.on());
    }

    public static void filterWorld(net.minecraft.client.Minecraft minecraft,
                                   com.mojang.blaze3d.resource.GraphicsResourceAllocator pool) {
        AccessibilityModule module = ModuleManager.get().get(AccessibilityModule.class);
        if (module == null || !module.isEnabled() || minecraft.level == null || module.filter.get() == ColorFilter.OFF) return;
        var id = Compat.id("accessibility_" + module.filter.get().name().toLowerCase(java.util.Locale.ROOT));
        var chain = minecraft.getShaderManager().getPostChain(id, net.minecraft.client.renderer.LevelTargetBundle.MAIN_TARGETS);
        //? if >=26.2 {
        /*if (chain != null) chain.process(minecraft.gameRenderer.mainRenderTarget(), pool);
        *///?} else
        if (chain != null) chain.process(minecraft.getMainRenderTarget(), pool);
    }

    public static SubtitleStyle subtitleStyle() {
        AccessibilityModule module = ModuleManager.get().get(AccessibilityModule.class);
        return module == null || !module.isEnabled() ? SubtitleStyle.VANILLA : module.subtitles.get();
    }

    private enum ColorFilter {
        OFF, PROTANOPIA, DEUTERANOPIA, TRITANOPIA
    }

    public enum SubtitleStyle {
        VANILLA(0), WHITE(0xFFFFFFFF), YELLOW(0xFFFFFF77);

        private final int color;

        SubtitleStyle(int color) {
            this.color = color;
        }

        public int text(int original) {
            return this == VANILLA ? original : color;
        }

        public int background(int original) {
            return this == VANILLA ? original : 0xFF000000;
        }
    }
}
