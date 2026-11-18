package dev.henny.vibeyclient.ui.title;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.VibeyScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;

/**
 * Dev-only visual check for versions without the Fabric client gametest ({@code -Dvibeyclient.uishot=true}, e.g. via
 * {@code JAVA_TOOL_OPTIONS} for {@code runClient}): waits for the onboarding / title screen, takes vanilla
 * screenshots (run/screenshots) of the onboarding, the title screen and the Vibey menu, then quits. Does nothing
 * without the system property.
 */
public final class UiShot {
    private UiShot() {}

    private static final boolean ON = Boolean.getBoolean("vibeyclient.uishot");
    private static int stage;
    private static int ticks;

    public static void tick() {
        if (!ON || stage < 0) return;
        Minecraft mc = Minecraft.getInstance();
        Screen s = ClientCompat.screen();
        ticks++;
        switch (stage) {
            case 0 -> { // wait for the first Vibey screen
                if (s instanceof OnboardingScreen || s instanceof net.minecraft.client.gui.screens.TitleScreen) {
                    stage = s instanceof OnboardingScreen ? 1 : 2;
                    ticks = 0;
                } else if (ticks > 200 && s != null) {
                    // a fresh game directory starts with vanilla's accessibility onboarding: go on to the title
                    mc.options.onboardAccessibility = false;
                    ClientCompat.openScreen(new net.minecraft.client.gui.screens.TitleScreen());
                    ticks = 0;
                }
            }
            case 1 -> {
                if (ticks == 40) shot(mc, "onboarding");
                if (ticks == 45) {
                    GlobalSettings.setOnboarded(true);
                    ClientCompat.openScreen(new net.minecraft.client.gui.screens.TitleScreen());
                    stage = 2;
                    ticks = 0;
                }
            }
            case 2 -> {
                if (ticks == 60) shot(mc, "title");
                if (ticks == 65) {
                    ClientCompat.openScreen(new VibeyScreen(s));
                    stage = 3;
                    ticks = 0;
                }
            }
            case 3 -> {
                if (ticks == 30) shot(mc, "menu");
                if (ticks == 35) {
                    Vibey.LOG.info("[vibeyclient-uishot] done");
                    stage = -1;
                    mc.stop();
                }
            }
            default -> {}
        }
    }

    private static void shot(Minecraft mc, String what) {
        Vibey.LOG.info("[vibeyclient-uishot] {} on {}", what, Vibey.platform().loader());
        //? if >=26.2 {
        /*Screenshot.grab(mc.gameDirectory, mc.gameRenderer.mainRenderTarget(), msg -> Vibey.LOG.info("[vibeyclient-uishot] {}", msg.getString()));
        *///?} else
        Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), msg -> Vibey.LOG.info("[vibeyclient-uishot] {}", msg.getString()));
    }
}
