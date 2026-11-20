package dev.henny.vibeyclient.ui.vui.onboarding;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.vui.SyncedScreen;
import dev.henny.vibeyclient.ui.vui.VibeyVui;
import net.minecraft.client.gui.screens.Screen;

public final class Onboarding extends SyncedScreen {
    public static final String DOC = "vibeyclient:onboarding/onboarding";

    private final OnboardingController controller;

    private Onboarding(Screen next, OnboardingController controller) {
        super(DOC, controller, next);
        this.controller = controller;
    }

    public static Onboarding create(Screen next) {
        VibeyVui.init();
        return new Onboarding(next, new OnboardingController(() -> ClientCompat.openScreen(next)));
    }

    public static void open(Screen next) {
        ClientCompat.openScreen(create(next));
    }

    public OnboardingController controller() {
        return controller;
    }

    @Override
    public void onClose() {
        controller.finish();
    }

    @Override
    protected void sync() {
        controller.sync();
    }

    @Override
    protected boolean keyDown(int key, int scancode) {
        if (view().keyDown(key, scancode)) return true;
        if (key == Keys.RIGHT) {
            controller.next();
            return true;
        }
        if (key == Keys.LEFT) {
            controller.previous();
            return true;
        }
        return false;
    }
}
