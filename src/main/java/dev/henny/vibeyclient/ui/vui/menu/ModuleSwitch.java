package dev.henny.vibeyclient.ui.vui.menu;

import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vui.bind.State;
import dev.henny.vui.expr.Values;

public final class ModuleSwitch extends State<Boolean> {
    private final String id;

    public ModuleSwitch(String id) {
        super(ModuleManager.get().isEnabled(id));
        this.id = id;
    }

    @Override
    public void set(Boolean next) {
        ModuleManager.get().setEnabled(id, Values.truthy(next));
        sync();
    }

    public void sync() {
        super.set(ModuleManager.get().isEnabled(id));
    }
}
