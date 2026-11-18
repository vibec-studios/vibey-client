package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.VibeyButton;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.VibeyTabs;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import dev.henny.vibeyclient.ui.screen.SettingsView;
import dev.henny.vibeyclient.ui.title.OnboardingScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Screenshots of the client UI at GUI scales 2 and 3, window sizes 1280×720 / 1920×1080, English and German: the
 * vanilla title screen with the Vibey corner buttons (+ hover tooltip), the module grid, a module page, its advanced
 * area collapsed and open, the General and Account tabs, search, onboarding, About and the pause row. Clicks go
 * through the real mouse input (title button → Vibey menu, tile → module page, back). Files:
 * versions/fabric-1.21.11/build/run/clientGameTest/screenshots/ui_*.png. Run: ./gradlew :fabric-1.21.11:runClientGameTest
 */
public final class VibeyUiGameTest implements FabricClientGameTest {
    private static final int[][] SIZES = {{1280, 720}, {1920, 1080}};
    private static final int[] SCALES = {2, 3};

    @Override
    public void runTest(ClientGameTestContext ctx) {
        for (int[] size : SIZES) {
            ctx.getInput().resizeWindow(size[0], size[1]);
            for (int scale : SCALES) {
                setScale(ctx, scale);
                String tag = size[0] + "_s" + scale;
                lang(ctx, GlobalSettings.Language.ENGLISH);
                screens(ctx, tag, size[0] == 1280);
                lang(ctx, GlobalSettings.Language.GERMAN);
                screens(ctx, tag + "_de", false);
                lang(ctx, GlobalSettings.Language.ENGLISH);
            }
        }

        // real clicks: title corner button → Vibey menu, tile → module page, back → grid
        ctx.getInput().resizeWindow(1280, 720);
        setScale(ctx, 2);
        clicks(ctx);

        // the Vibey UI scale override (2x on a 1920x1080 window with game scale auto = 4)
        ctx.getInput().resizeWindow(1920, 1080);
        setScale(ctx, 0);
        ctx.runOnClient(mc -> GlobalSettings.uiScale.set(GlobalSettings.UiScale.X2));
        ModulesPanel.preselect(null);
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(6);
        ctx.takeScreenshot("ui_menu_1920_uiscale2");
        ctx.setScreen(() -> null);
        int restored = ctx.computeOnClient(mc -> (int) mc.getWindow().getGuiScale());
        if (restored != 4) throw new AssertionError("GUI scale not restored after closing the Vibey menu: " + restored);
        ctx.runOnClient(mc -> GlobalSettings.uiScale.set(GlobalSettings.UiScale.GAME));

        // in a world: pause menu with the Vibey row, at the small and the big scales
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            for (int scale : new int[] {2, 3, 4}) {
                setScale(ctx, scale);
                ctx.setScreen(() -> new PauseScreen(true));
                ctx.waitTicks(4);
                ctx.takeScreenshot("ui_pause_1280_s" + scale);
            }
            // tiny GUI (320 x 180): the row has to move out of the way
            ctx.getInput().resizeWindow(960, 540);
            setScale(ctx, 3);
            ctx.setScreen(() -> new PauseScreen(true));
            ctx.waitTicks(4);
            ctx.takeScreenshot("ui_pause_960_s3");
            ctx.getInput().resizeWindow(1280, 720);
            setScale(ctx, 3);
            lang(ctx, GlobalSettings.Language.GERMAN);
            ctx.setScreen(() -> new PauseScreen(true));
            ctx.waitTicks(4);
            ctx.takeScreenshot("ui_pause_1280_s3_de");
            lang(ctx, GlobalSettings.Language.ENGLISH);
            ctx.setScreen(() -> null);
        }
        lang(ctx, GlobalSettings.Language.AUTO);
        setScale(ctx, 0);
    }

    private static void screens(ClientGameTestContext ctx, String tag, boolean extra) {
        // vanilla title + corner buttons, then hover the first one for its tooltip
        ctx.setScreen(TitleScreen::new);
        ctx.waitTicks(6);
        ctx.takeScreenshot("ui_title_" + tag);
        int[] b = ctx.computeOnClient(mc -> buttonCenter("wardrobe", 0));
        hover(ctx, b[0], b[1]);
        ctx.waitTicks(4);
        ctx.takeScreenshot("ui_title_hover_" + tag);
        ctx.getInput().setCursorPos(0, 0);

        // module grid
        ModulesPanel.preselect(null);
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(8);
        ctx.takeScreenshot("ui_menu_grid_" + tag);

        // module page: cosmetics (sections, two-column switches, keys)
        ModulesPanel.preselect("cosmetics");
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(8);
        ctx.takeScreenshot("ui_menu_module_" + tag);

        // module page with an advanced area: multiplayer, collapsed and open (scrolled to the bottom)
        ModulesPanel.preselect("multiplayer");
        ctx.runOnClient(mc -> SettingsView.setAdvancedOpen(ModuleManager.get().get("multiplayer").settings(), false));
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(8);
        ctx.takeScreenshot("ui_menu_advanced_closed_" + tag);
        ctx.runOnClient(mc -> SettingsView.setAdvancedOpen(ModuleManager.get().get("multiplayer").settings(), true));
        scrollMiddle(ctx, -40);
        ctx.waitTicks(10);
        ctx.takeScreenshot("ui_menu_advanced_open_" + tag);
        ctx.runOnClient(mc -> SettingsView.setAdvancedOpen(ModuleManager.get().get("multiplayer").settings(), false));

        // General tab
        ctx.runOnClient(mc -> SettingsView.setAdvancedOpen(GlobalSettings.GROUP, false));
        ctx.setScreen(() -> new VibeyScreen(null, VibeyTabs.GENERAL));
        ctx.waitTicks(8);
        ctx.takeScreenshot("ui_menu_general_" + tag);

        // Account tab (offline profile in the test: not signed in)
        ctx.setScreen(() -> new VibeyScreen(null, "account"));
        ctx.waitTicks(8);
        ctx.takeScreenshot("ui_menu_account_" + tag);

        if (!extra) return;
        ctx.runOnClient(mc -> SettingsView.setAdvancedOpen(GlobalSettings.GROUP, true));
        ctx.setScreen(() -> new VibeyScreen(null, VibeyTabs.GENERAL));
        ctx.waitTicks(4);
        scrollMiddle(ctx, -40);
        ctx.waitTicks(10);
        ctx.takeScreenshot("ui_menu_general_advanced_" + tag);
        ctx.runOnClient(mc -> SettingsView.setAdvancedOpen(GlobalSettings.GROUP, false));

        // search across modules and settings
        ModulesPanel.preselect(null);
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(4);
        ctx.getInput().typeChars("vol");
        ctx.waitTicks(6);
        ctx.takeScreenshot("ui_menu_search_" + tag);

        ctx.setScreen(() -> new VibeyScreen(null, VibeyTabs.ABOUT));
        ctx.waitTicks(8);
        ctx.takeScreenshot("ui_menu_about_" + tag);

        ctx.setScreen(() -> new OnboardingScreen(new TitleScreen()));
        ctx.waitTicks(6);
        ctx.takeScreenshot("ui_onboarding1_" + tag);
        ctx.getInput().pressKey(Keys.RIGHT);
        ctx.waitTicks(6);
        ctx.takeScreenshot("ui_onboarding2_" + tag);
        ctx.getInput().pressKey(Keys.RIGHT);
        ctx.waitTicks(6);
        ctx.takeScreenshot("ui_onboarding3_" + tag);
    }

    private static void clicks(ClientGameTestContext ctx) {
        ctx.setScreen(TitleScreen::new);
        ctx.waitTicks(6);
        int[] v = ctx.computeOnClient(mc -> buttonCenter("vibey", -1));
        click(ctx, v[0], v[1]);
        ctx.waitTicks(6);
        if (!ctx.computeOnClient(mc -> ClientCompat.screen() instanceof VibeyScreen)) {
            throw new AssertionError("clicking the Vibey corner button did not open the Vibey menu: " + ctx.computeOnClient(mc -> String.valueOf(ClientCompat.screen())));
        }
        ctx.runOnClient(mc -> {
            if (ClientCompat.screen() instanceof VibeyScreen s) s.showTab(VibeyTabs.MODULES);
        });
        ModulesPanel.preselect(null);
        ctx.setScreen(() -> new VibeyScreen(new TitleScreen()));
        ctx.waitTicks(8);
        int[] t = ctx.computeOnClient(mc -> ModulesPanel.tileBounds("emotes"));
        if (t == null) throw new AssertionError("no emotes tile drawn");
        click(ctx, t[0] + 60, t[1] + t[3] - 8);
        ctx.waitTicks(6);
        String page = ctx.computeOnClient(mc -> ModulesPanel.openPage());
        if (!"emotes".equals(page)) throw new AssertionError("clicking the emotes tile opened " + page);
        ctx.takeScreenshot("ui_click_module_page");
        ctx.getInput().pressKey(Keys.ESCAPE);
        ctx.waitTicks(4);
        if (ctx.computeOnClient(mc -> ModulesPanel.openPage()) != null) throw new AssertionError("Esc did not go back to the grid");
        ctx.setScreen(() -> null);
    }

    /** Center (GUI px) of the title corner button with that MenuEntries id ({@code index}: fallback, -1 = last). */
    private static int[] buttonCenter(String id, int index) {
        var screen = ClientCompat.screen();
        String label = dev.henny.vibeyclient.api.Lang.tr(switch (id) {
            case "wardrobe" -> "core.title.wardrobe";
            case "friends" -> "core.title.friends";
            default -> "core.title.vibey";
        });
        java.util.List<VibeyButton> buttons = new java.util.ArrayList<>();
        for (GuiEventListener l : screen.children()) {
            if (l instanceof VibeyButton vb) {
                if (vb.getMessage().getString().equals(label)) return new int[] {vb.getX() + vb.getWidth() / 2, vb.getY() + vb.getHeight() / 2};
                buttons.add(vb);
            }
        }
        if (buttons.isEmpty()) throw new AssertionError("no Vibey buttons on the title screen");
        VibeyButton vb = buttons.get(index < 0 ? buttons.size() - 1 : Math.min(index, buttons.size() - 1));
        return new int[] {vb.getX() + vb.getWidth() / 2, vb.getY() + vb.getHeight() / 2};
    }

    private static void hover(ClientGameTestContext ctx, int guiX, int guiY) {
        double s = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
        ctx.getInput().setCursorPos(guiX * s + s / 2, guiY * s + s / 2);
    }

    private static void click(ClientGameTestContext ctx, int guiX, int guiY) {
        hover(ctx, guiX, guiY);
        ctx.waitTicks(2);
        ctx.getInput().pressMouse(0);
    }

    /** Wheel over the middle of the window (negative = down). */
    private static void scrollMiddle(ClientGameTestContext ctx, double amount) {
        int[] wh = ctx.computeOnClient(mc -> new int[] {mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight()});
        ctx.getInput().setCursorPos(wh[0] / 2.0, wh[1] / 2.0);
        ctx.waitTicks(1);
        ctx.getInput().scroll(amount);
        ctx.waitTicks(3); // the next frames take the wheel while the mouse is over the page
        ctx.getInput().setCursorPos(0, 0);
    }

    private static void setScale(ClientGameTestContext ctx, int scale) {
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
        });
        ctx.waitTicks(2);
    }

    private static void lang(ClientGameTestContext ctx, GlobalSettings.Language l) {
        ctx.runOnClient(mc -> {
            GlobalSettings.language.set(l);
            dev.henny.vibeyclient.api.Lang.tick();
        });
    }
}
