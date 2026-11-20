package dev.henny.vibeyclient.ui.vui;

import dev.henny.vibeyclient.ui.vui.menu.MenuController;
import dev.henny.vibeyclient.ui.vui.menu.MenuScreen;
import dev.henny.vibeyclient.ui.vui.onboarding.Onboarding;
import dev.henny.vibeyclient.ui.vui.onboarding.OnboardingController;
import dev.henny.vui.mc.VuiMc;

final class DevControllers {
    private DevControllers() {}

    static void register() {
        if (!VuiMc.dev().enabled()) return;
        VuiMc.dev().controller(MenuScreen.DOC, MenuController::new);
        VuiMc.dev().controller(Onboarding.DOC, () -> new OnboardingController(() -> {}));
    }
}
