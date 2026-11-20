package dev.henny.vibeyclient.ui.vui.onboarding;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.UiScale;
import dev.henny.vibeyclient.ui.vui.VibeyVui;
import dev.henny.vui.mc.host.VuiScreen;
import net.minecraft.client.gui.screens.Screen;

public final class Onboarding extends VuiScreen {
    public static final String DOC = "vibeyclient:onboarding/onboarding";

    private final OnboardingController controller;
    private final Screen next;
    private int ticks;

    private Onboarding(Screen next, OnboardingController controller) {
        super(DOC, controller);
        this.next = next;
        this.controller = controller;
        parent(next);
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
    protected void init() {
        UiScale.apply(this);
        super.init();
    }

    @Override
    public void removed() {
        super.removed();
        UiScale.restore();
    }

    @Override
    public boolean isPauseScreen() {
        return next != null && next.isPauseScreen();
    }

    @Override
    public void tick() {
        super.tick();
        if (++ticks % 5 != 0) return;
        controller.sync();
        document().refresh();
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
