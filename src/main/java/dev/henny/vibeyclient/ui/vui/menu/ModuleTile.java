package dev.henny.vibeyclient.ui.vui.menu;

import dev.henny.vibeyclient.api.VibeyModule;

public final class ModuleTile {
    private final VibeyModule module;
    private final ModuleSwitch on;
    private final String name;
    private final String tip;
    private String hit = "";

    public ModuleTile(VibeyModule module) {
        this.module = module;
        this.on = new ModuleSwitch(module.id());
        this.name = module.displayName();
        this.tip = ModuleLook.summary(module);
    }

    public String getId() { return module.id(); }
    public String getName() { return name; }
    public String getIcon() { return ModuleLook.icon(module); }
    public String getAccent() { return ModuleLook.accent(module); }
    public ModuleSwitch getOn() { return on; }
    public boolean isAvailable() { return ModuleLook.available(module); }
    public boolean isBlocked() { return ModuleLook.blocked(module); }
    public String getTip() { return tip; }
    public String getReason() { return ModuleLook.reason(module); }
    public String getHit() { return hit; }

    public VibeyModule module() {
        return module;
    }

    ModuleTile hit(String text) {
        hit = text == null ? "" : text;
        return this;
    }
}
