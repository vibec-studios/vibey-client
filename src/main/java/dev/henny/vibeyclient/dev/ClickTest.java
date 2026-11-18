package dev.henny.vibeyclient.dev;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.mixin.client.MouseHandlerAccessor;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.VibeyTabs;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import dev.henny.vibeyclient.ui.title.OnboardingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonInfo;
//?}

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Dev-only click self-test for every version and both loaders: {@code -Dvibeyclient.clicktest=<outdir>} (or env
 * {@code VIBEYCLIENT_CLICKTEST=<outdir>}). It feeds real mouse presses/releases into the game's {@code MouseHandler}
 * (the path the window callback takes, so GUI scaling, the native button numbers and the screen dispatch are all
 * included) and checks that Vibey widgets react: an immediate-mode button, a right-click area and a
 * {@code VibeyButton} on {@link ClickTestScreen}, then a tab of the real Vibey menu. Logs
 * {@code [vibeyclient-clicktest] PASS|FAIL <check>} lines and a {@code SUMMARY}, writes {@code <outdir>/clicktest.txt}
 * and screenshots, and quits.
 */
public final class ClickTest {
    private ClickTest() {}

    private static final String OUT = outDir();
    private static final boolean ON = OUT != null && Vibey.platform().isDevelopment();
    private static final String TAG = "[vibeyclient-clicktest] ";

    /** One step; returns the ticks to wait before the next one. */
    private interface Step {
        int run(Minecraft mc) throws Exception;
    }

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static final List<String> RESULTS = new ArrayList<>();
    private static int wait, startTicks, pass, fail;
    private static boolean started, finished;
    private static ClickTestScreen screen;

    private static String outDir() {
        String p = System.getProperty("vibeyclient.clicktest");
        if (p == null || p.isBlank() || p.equals("false")) p = System.getenv("VIBEYCLIENT_CLICKTEST");
        return p == null || p.isBlank() || p.equals("false") ? null : p;
    }

    public static boolean enabled() {
        return ON;
    }

    /** End of every client tick (core). */
    public static void tick() {
        if (!ON || finished) return;
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            if (!ready(mc)) return;
            started = true;
            log("start: Minecraft " + dev.henny.vibeyclient.BuildInfo.MINECRAFT + " on " + Vibey.platform().loader()
                    + ", output " + Path.of(OUT).toAbsolutePath());
            build();
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Step s = STEPS.poll();
        if (s == null) {
            finish(mc);
            return;
        }
        try {
            wait = s.run(mc);
        } catch (Throwable t) {
            check("step", false, "exception " + t);
            Vibey.LOG.error(TAG + "step failed", t);
            wait = 0;
        }
    }

    private static boolean ready(Minecraft mc) {
        if (!ModuleManager.get().isStarted() || overlay(mc)) {
            startTicks = 0;
            return false;
        }
        Screen s = ClientCompat.screen();
        if (s == null) return false;
        if (startTicks++ < 40) return false;
        return s instanceof TitleScreen || s instanceof OnboardingScreen
                || s.getClass().getSimpleName().contains("Onboarding");
    }

    /** The loading overlay is up (the mouse handler then drops every click). */
    private static boolean overlay(Minecraft mc) {
        //? if >=26.2 {
        /*return mc.gui.overlay() != null;
        *///?} else
        return mc.getOverlay() != null;
    }

    // ============================================================================================ the test

    private static void build() {
        step(mc -> {
            mc.options.onboardAccessibility = false;
            GlobalSettings.setOnboarded(true);
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            screen = new ClickTestScreen();
            ClientCompat.openScreen(screen);
            return 20;
        });

        // immediate-mode button (BaseScreen glue -> Ui.queueClick -> Ui.clicked in the next frame)
        clickAt(() -> screen.col() + ClickTestScreen.BW / 2.0, () -> screen.uiButtonY() + ClickTestScreen.BH / 2.0, Keys.MOUSE_LEFT);
        step(mc -> {
            check("ui_left_click", screen.leftClicks == 1 && screen.rightClicks == 0,
                    "left=" + screen.leftClicks + " right=" + screen.rightClicks);
            return 0;
        });
        // a right-click area must take a right click ...
        clickAt(() -> screen.col() + ClickTestScreen.BW / 2.0, () -> screen.rightAreaY() + ClickTestScreen.BH / 2.0, Keys.MOUSE_RIGHT);
        step(mc -> {
            check("ui_right_click", screen.rightClicks == 1, "right=" + screen.rightClicks);
            return 0;
        });
        // ... and not a left click
        int[] rightBefore = new int[1];
        step(mc -> {
            rightBefore[0] = screen.rightClicks;
            return 0;
        });
        clickAt(() -> screen.col() + ClickTestScreen.BW / 2.0, () -> screen.rightAreaY() + ClickTestScreen.BH / 2.0, Keys.MOUSE_LEFT);
        step(mc -> {
            check("ui_left_not_right", screen.rightClicks == rightBefore[0],
                    "right-click area fired " + (screen.rightClicks - rightBefore[0]) + "x on a left click");
            return 0;
        });
        // a real widget (VibeyButton, used on the title and pause screens)
        clickAt(() -> screen.col() + ClickTestScreen.BW / 2.0, () -> screen.vibeyButtonY() + ClickTestScreen.BH / 2.0, Keys.MOUSE_LEFT);
        step(mc -> {
            check("vibey_button", screen.vibeyClicks == 1, "vibey=" + screen.vibeyClicks);
            return 2;
        });
        step(mc -> {
            shot(mc, "clicktest-screen");
            return 0;
        });

        // the real Vibey menu: click along the header until the tab changes
        step(mc -> {
            ModulesPanel.preselect(null);
            ClientCompat.openScreen(new VibeyScreen(new TitleScreen(), VibeyTabs.MODULES));
            return 30;
        });
        step(mc -> {
            shot(mc, "menu-before");
            return 0;
        });
        STEPS.add(new TabScan());

        // the corner button on the vanilla title screen opens the Vibey menu
        step(mc -> {
            ClientCompat.openScreen(new TitleScreen());
            return 30;
        });
        clickAt(() -> titleButton()[0], () -> titleButton()[1], Keys.MOUSE_LEFT);
        step(mc -> {
            check("title_button", ClientCompat.screen() instanceof VibeyScreen, "screen=" + ClientCompat.screen());
            shot(mc, "title-button-after");
            return 0;
        });

        // a module tile opens its page
        step(mc -> {
            ModulesPanel.preselect(null);
            ClientCompat.openScreen(new VibeyScreen(new TitleScreen(), VibeyTabs.MODULES));
            return 30;
        });
        clickAt(() -> tile("emotes")[0], () -> tile("emotes")[1], Keys.MOUSE_LEFT);
        step(mc -> {
            check("module_tile", "emotes".equals(ModulesPanel.openPage()), "page=" + ModulesPanel.openPage());
            shot(mc, "module-page");
            return 0;
        });

        // screens get GLFW key codes on every version (26.3 reports SDL scancodes)
        step(mc -> {
            int[] keys = {Keys.ESCAPE, Keys.ENTER, Keys.TAB, Keys.BACKSPACE, Keys.UP, Keys.DOWN, Keys.LEFT, Keys.RIGHT, Keys.A, Keys.Z,
                    Keys.F1, Keys.RIGHT_SHIFT, Keys.SPACE};
            StringBuilder bad = new StringBuilder();
            for (int k : keys) if (Keys.fromNative(Keys.toNative(k)) != k) bad.append(k).append(' ');
            check("key_codes", bad.length() == 0, bad.length() == 0 ? "round trip ok" : "wrong: " + bad);
            return 0;
        });
    }

    /** Center (GUI px) of the "Vibey menu" corner button on the title screen, or the screen center. */
    private static double[] titleButton() {
        var s = ClientCompat.screen();
        String label = dev.henny.vibeyclient.api.Lang.tr("core.title.vibey");
        if (s != null) {
            for (var l : s.children()) {
                if (l instanceof dev.henny.vibeyclient.ui.VibeyButton b && b.getMessage().getString().equals(label)) {
                    return new double[] {b.getX() + b.getWidth() / 2.0, b.getY() + b.getHeight() / 2.0};
                }
            }
        }
        if (s instanceof TitleScreen) log("no Vibey corner button on the title screen");
        return new double[] {s == null ? 0 : s.width / 2.0, s == null ? 0 : s.height / 2.0};
    }

    /** A point inside the tile of module {@code id} (bottom left, away from the switch). */
    private static double[] tile(String id) {
        int[] b = ModulesPanel.tileBounds(id);
        if (b == null) return new double[] {0, 0};
        return new double[] {b[0] + 50, b[1] + b[3] - 8};
    }

    /** Clicks across the menu header (left of the close button) until {@link VibeyScreen#tab()} changes. */
    private static final class TabScan implements Step {
        private int x = -1;
        private int phase;
        private String before;

        @Override
        public int run(Minecraft mc) throws Exception {
            if (!(ClientCompat.screen() instanceof VibeyScreen vs)) {
                check("menu_tab_click", false, "the menu closed during the scan: " + ClientCompat.screen());
                return 0;
            }
            int w = vs.windowWidth(), h = vs.windowHeight();
            int left = (vs.width - w) / 2, top = (vs.height - h) / 2;
            double y = top + VibeyScreen.HEADER / 2.0;
            if (x < 0) {
                before = vs.tab();
                x = left + 90;
            }
            if (phase == 0) {
                press(mc, x, y, Keys.MOUSE_LEFT, true);
                phase = 1;
                STEPS.addFirst(this);
                return 2;
            }
            press(mc, x, y, Keys.MOUSE_LEFT, false);
            phase = 0;
            STEPS.addFirst(mc2 -> {
                if (!(ClientCompat.screen() instanceof VibeyScreen now)) {
                    check("menu_tab_click", false, "the menu closed after a click at x=" + x + ": " + ClientCompat.screen());
                    return 0;
                }
                if (!now.tab().equals(before)) {
                    check("menu_tab_click", true, "tab " + before + " -> " + now.tab() + " (click at " + x + "," + (int) y + ")");
                    STEPS.addFirst(mc3 -> {
                        shot(mc3, "menu-after");
                        return 0;
                    });
                    return 15;
                }
                x += 16;
                if (x >= left + w - 34) { // the close button starts at w - 26
                    check("menu_tab_click", false, "no header click changed the tab (still " + now.tab() + ")");
                    STEPS.addFirst(mc3 -> {
                        shot(mc3, "menu-after");
                        return 0;
                    });
                    return 2;
                }
                STEPS.addFirst(this);
                return 0;
            });
            return 4;
        }
    }

    // ============================================================================================ input injection

    private interface Coord {
        double get();
    }

    /** Press, two ticks, release, then a few ticks so the screen draws (immediate-mode widgets consume clicks there). */
    private static void clickAt(Coord x, Coord y, int button) {
        step(mc -> {
            press(mc, x.get(), y.get(), button, true);
            return 2;
        });
        step(mc -> {
            press(mc, x.get(), y.get(), button, false);
            return 4;
        });
    }

    /**
     * Moves the cursor to GUI position (x, y) and sends a press or release of {@code button} (GLFW numbering, see
     * {@link Keys#mouseToNative}) through the game's mouse handler, as the window callback does.
     */
    static void press(Minecraft mc, double x, double y, int button, boolean down) {
        var window = mc.getWindow();
        MouseHandlerAccessor mouse = (MouseHandlerAccessor) (Object) mc.mouseHandler;
        mouse.vibeyclient$setXpos(x * window.getScreenWidth() / (double) window.getGuiScaledWidth());
        mouse.vibeyclient$setYpos(y * window.getScreenHeight() / (double) window.getGuiScaledHeight());
        int nativeButton = Keys.mouseToNative(button);
        //? if >=1.21.9 {
        mouse.vibeyclient$onButton(windowHandle(mc), new MouseButtonInfo(nativeButton, 0), down ? 1 : 0);
        //?} else
        /*mouse.vibeyclient$onPress(windowHandle(mc), nativeButton, down ? 1 : 0, 0);*/
        if (down) {
            log("click at " + (int) x + "," + (int) y + " (GUI px; window " + window.getScreenWidth() + "x" + window.getScreenHeight()
                    + ", GUI " + window.getGuiScaledWidth() + "x" + window.getGuiScaledHeight() + "), native button " + nativeButton
                    + (overlay(mc) ? ", LOADING OVERLAY UP: dropped" : "") + " on " + (ClientCompat.screen() == null ? "no screen" : ClientCompat.screen().getClass().getSimpleName()));
        }
    }

    private static long windowHandle(Minecraft mc) {
        Object window = mc.getWindow();
        for (String name : new String[]{"handle", "getWindow"}) {
            try {
                Method m = window.getClass().getMethod(name);
                if (m.getReturnType() == long.class) return (long) m.invoke(window);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        throw new IllegalStateException("no window handle getter");
    }

    // ============================================================================================ helpers

    private static void step(Step s) {
        STEPS.add(s);
    }

    private static void check(String name, boolean ok, String detail) {
        String line = (ok ? "PASS " : "FAIL ") + name + " " + detail;
        if (ok) pass++;
        else fail++;
        RESULTS.add(line);
        log(line);
    }

    private static void shot(Minecraft mc, String name) {
        try {
            DevTour.screenshot(mc, Path.of(OUT), name);
        } catch (Throwable t) {
            log("screenshot " + name + " failed: " + t);
        }
    }

    private static void finish(Minecraft mc) {
        finished = true;
        String summary = "SUMMARY " + (fail == 0 && pass > 0 ? "PASS" : "FAIL") + " " + pass + " passed, " + fail + " failed (Minecraft "
                + dev.henny.vibeyclient.BuildInfo.MINECRAFT + ", " + Vibey.platform().loader() + ")";
        RESULTS.add(summary);
        log(summary);
        try {
            Path dir = Path.of(OUT).toAbsolutePath();
            Files.createDirectories(dir);
            Files.write(dir.resolve("clicktest.txt"), RESULTS);
        } catch (Exception e) {
            log("writing clicktest.txt failed: " + e);
        }
        mc.execute(mc::stop);
    }

    private static void log(String s) {
        Vibey.LOG.info(TAG + "{}", s);
    }
}
