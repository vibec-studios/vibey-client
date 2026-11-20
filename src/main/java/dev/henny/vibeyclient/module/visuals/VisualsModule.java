package dev.henny.vibeyclient.module.visuals;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.module.visuals.core.ParticlePlan;
import dev.henny.vibeyclient.module.visuals.items.ItemRules;
import dev.henny.vibeyclient.module.visuals.items.drop.DropSettings;
import dev.henny.vibeyclient.module.visuals.items.edit.ItemEditor;
import dev.henny.vibeyclient.module.visuals.items.glint.GlintSettings;
import dev.henny.vibeyclient.module.visuals.items.held.HeldSettings;
import dev.henny.vibeyclient.module.visuals.items.swing.SwingSettings;

import java.util.List;

public final class VisualsModule extends VibeyModule {
    private static volatile VisualsModule instance;

    public final HeldSettings held = new HeldSettings(settings(), ItemEditor::open, this::isEnabled);
    public final SwingSettings swing = new SwingSettings(settings());

    public final BoolSetting itemPhysics = bool("itemPhysics", true).section("items");
    public final DropSettings drops = new DropSettings(settings());
    public final GlintSettings glint = new GlintSettings(settings());

    public final IntSetting particles = intRange("particles", 100, 0, ParticlePlan.MAX_PERCENT, 25).unit("%")
            .section("particles");
    public final BoolSetting keepImportant = bool("keepImportant", true).section("particles");
    public final BoolSetting ambientOff = bool("ambientOff", false).section("particles");

    public VisualsModule() {
        super("visuals", ModuleCategory.MISC);
        instance = this;
    }

    public static VisualsModule get() {
        return instance;
    }

    @Override
    public void onLoad() {
        ItemRules.declare();
    }

    @Override
    public List<String> serverRuleKeys() {
        return List.of(ItemRules.HELD);
    }

    @Override
    public int accentColor() {
        return 0xFF9B8CFF;
    }

    public boolean itemPhysicsActive() {
        return isEnabled() && itemPhysics.on();
    }

    public boolean particlesActive() {
        return isEnabled() && (particles.get() != 100 || ambientOff.on());
    }
}
