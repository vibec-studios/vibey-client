package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.client.branding.TitleBadge;
import dev.henny.vibeyclient.client.branding.WindowIcons;
import dev.henny.vibeyclient.gametest.mixin.WindowTitleInvoker;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.gui.screens.TitleScreen;

public final class BrandingGameTest implements FabricClientGameTest {
    private static volatile String serverBrand;

    public static void serverSaw(String brand) {
        serverBrand = brand;
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        UiTest t = new UiTest(ctx, "branding");
        boolean before = ctx.computeOnClient(mc -> GlobalSettings.serverBrand.on());
        String mcVersion = ctx.computeOnClient(mc -> SharedConstants.getCurrentVersion().name());
        String prefix = "Vibey Client " + BuildInfo.VERSION + " · Minecraft " + mcVersion;
        t.window(1280, 720);
        t.scale(2);

        ctx.setScreen(TitleScreen::new);
        ctx.waitTicks(40);
        expect(title(ctx), prefix);
        expect(ctx.computeOnClient(mc -> TitleBadge.label()), "Vibey Client " + BuildInfo.VERSION);
        if (WindowIcons.standard() == null) throw new AssertionError("window icons missing from the jar");
        t.shot("branding_title_screen");

        ctx.runOnClient(mc -> GlobalSettings.serverBrand.set(true));
        expect(ClientBrandRetriever.getClientModName(), "vibey,fabric");
        serverBrand = null;
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            String inWorld = title(ctx);
            if (!inWorld.startsWith(prefix + " – ")) throw new AssertionError("window title in a world: " + inWorld);
            expect(serverBrand, "vibey,fabric");
        }
        expect(title(ctx), prefix);

        ctx.runOnClient(mc -> GlobalSettings.serverBrand.set(false));
        expect(ClientBrandRetriever.getClientModName(), "fabric");
        serverBrand = null;
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            expect(serverBrand, "fabric");
        }
        ctx.runOnClient(mc -> GlobalSettings.serverBrand.set(before));
        t.scale(0);
    }

    private static String title(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> ((WindowTitleInvoker) mc).vibeyclient$createTitle());
    }

    private static void expect(String actual, String expected) {
        if (!expected.equals(actual)) throw new AssertionError("expected \"" + expected + "\" but was \"" + actual + "\"");
    }
}
