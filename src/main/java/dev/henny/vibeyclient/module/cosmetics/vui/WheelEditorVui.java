package dev.henny.vibeyclient.module.cosmetics.vui;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.vui.SyncedScreen;
import dev.henny.vibeyclient.ui.vui.VibeyVui;
import net.minecraft.client.gui.screens.Screen;

public final class WheelEditorVui extends SyncedScreen {
    public static final String DOC = "vibeyclient:wardrobe/wheel-editor";

    private final WheelEditorController controller;

    private WheelEditorVui(Screen parent, WheelEditorController controller) {
        super(DOC, controller, parent);
        this.controller = controller;
        background(!ClientCompat.inWorld());
    }

    public static void open(Screen parent) {
        VibeyVui.init();
        WheelEditorController c = new WheelEditorController(() -> ClientCompat.openScreen(parent));
        ClientCompat.openScreen(new WheelEditorVui(parent, c));
    }

    public WheelEditorController controller() {
        return controller;
    }

    @Override
    protected void init() {
        super.init();
        controller.layout(width, height);
    }

    @Override
    protected void sync() {
        controller.sync();
    }
}
