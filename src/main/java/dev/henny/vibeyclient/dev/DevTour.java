package dev.henny.vibeyclient.dev;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.avatar.AvatarItem;
import dev.henny.vibeyclient.avatar.AvatarItems;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.Wardrobe;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi;
import dev.henny.vibeyclient.module.multiplayer.screen.HostScreen;
import dev.henny.vibeyclient.module.social.SidebarRoute;
import dev.henny.vibeyclient.module.social.SocialModule;
import dev.henny.vibeyclient.module.social.ui.SocialScreen;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.VibeyTabs;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import dev.henny.vibeyclient.ui.title.OnboardingScreen;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Dev-only runtime tour for every version and both loaders. Start a dev client with
 * {@code -Dvibeyclient.tour=<outdir>} (or env {@code VIBEYCLIENT_TOUR=<outdir>}); it walks through the title screen,
 * onboarding, the Vibey menu, a fresh flat singleplayer world, every cosmetic slot on the local player (third person
 * front/back), three emotes mid-animation, the wardrobe, the emote wheel, the social sidebar, the lyrics sky (Fabric),
 * the multiplayer host screen and the pause menu, saves a screenshot of each into {@code <outdir>} and quits.
 * Every step logs one line {@code [vibeyclient-tour] PASS|FAIL <step> ...} (with errors other code logged during the
 * step), the end a {@code SUMMARY} line. Does nothing outside a development environment or without the switch.
 */
public final class DevTour {
    private DevTour() {}

    private static final String OUT = outDir();
    private static final boolean ON = OUT != null && Vibey.platform().isDevelopment();
    private static final String TAG = "[vibeyclient-tour] ";

    private interface Action {
        void run(Minecraft mc) throws Exception;
    }

    /** One operation of a step: run an action, wait ticks, wait for a condition, or take a screenshot. */
    private record Op(String step, Action action, BooleanSupplier until, int ticks, String shot) {}

    private static final List<Op> OPS = new ArrayList<>();
    private static final List<String> RESULTS = new ArrayList<>();
    private static final List<String> LOGGED = new ArrayList<>();
    private static int index = -1;
    private static int wait;
    private static int waited;
    private static String step;
    private static String failure;
    private static boolean failedStep;
    private static int pass, fail;
    private static boolean started, finished;
    private static int startTicks;
    private static int shots;

    public static boolean enabled() {
        return ON;
    }

    private static String outDir() {
        String p = System.getProperty("vibeyclient.tour");
        if (p == null || p.isBlank() || p.equals("false")) p = System.getenv("VIBEYCLIENT_TOUR");
        return p == null || p.isBlank() || p.equals("false") ? null : p;
    }

    // ============================================================================================ driver

    /** End of every client tick (core). */
    public static void tick() {
        if (!ON || finished) return;
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            if (!ready(mc)) return;
            started = true;
            build();
            LogTap.install();
            log("start: Minecraft " + dev.henny.vibeyclient.BuildInfo.MINECRAFT + " on " + Vibey.platform().loader() + ", "
                    + AvatarItems.all().size() + " items, output " + Path.of(OUT).toAbsolutePath());
            next();
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Op op = OPS.get(index);
        try {
            if (op.until() != null) {
                if (!op.until().getAsBoolean()) {
                    if (++waited > op.ticks()) throw new IllegalStateException("timed out after " + op.ticks() + " ticks");
                    return;
                }
            } else if (op.action() != null) {
                op.action().run(mc);
            } else if (op.shot() != null) {
                shot(mc, op.shot());
            } else {
                wait = op.ticks();
            }
        } catch (Throwable t) {
            failStep(t);
        }
        next();
    }

    /** Before the first step: the game (and our modules) are up and a title-like screen is showing. */
    private static boolean ready(Minecraft mc) {
        if (!ModuleManager.get().isStarted() || AvatarItems.all().size() < 20 || Services.get(Wardrobe.class).isEmpty()) return false;
        //? if <26.2 {
        if (mc.getOverlay() != null) return false;
        //?}
        Screen s = ClientCompat.screen();
        if (s == null) return false;
        if (startTicks++ < 40) return false;
        return s instanceof TitleScreen || s instanceof OnboardingScreen
                || s.getClass().getSimpleName().contains("Onboarding");
    }

    private static void next() {
        while (true) {
            index++;
            waited = 0;
            if (index >= OPS.size()) {
                endStep();
                finish();
                return;
            }
            Op op = OPS.get(index);
            if (!op.step().equals(step)) {
                endStep();
                step = op.step();
                failedStep = false;
                failure = null;
                synchronized (LOGGED) {
                    LOGGED.clear();
                }
            } else if (failedStep) {
                continue; // skip the rest of a failed step
            }
            return;
        }
    }

    private static void failStep(Throwable t) {
        failedStep = true;
        failure = t.toString();
        Vibey.LOG.error(TAG + "FAIL {}: exception", step, t);
    }

    private static void endStep() {
        if (step == null) return;
        String extra;
        synchronized (LOGGED) {
            extra = LOGGED.isEmpty() ? "" : " | " + LOGGED.size() + " error(s) logged meanwhile: " + String.join(" || ", LOGGED.subList(0, Math.min(3, LOGGED.size())));
            LOGGED.clear();
        }
        String line = (failedStep ? "FAIL " : "PASS ") + step + (failedStep ? " - " + failure : "") + extra;
        if (failedStep) fail++;
        else pass++;
        RESULTS.add(line);
        log(line);
        step = null;
    }

    private static void finish() {
        finished = true;
        log("SUMMARY " + pass + " PASS, " + fail + " FAIL, " + shots + " screenshots in " + Path.of(OUT).toAbsolutePath());
        for (String r : RESULTS) log("  " + r);
        LogTap.remove();
        Minecraft mc = Minecraft.getInstance();
        dev.henny.vibeyclient.api.Tasks.later(2500, () -> dev.henny.vibeyclient.api.Tasks.onMain(mc::stop));
    }

    // ============================================================================================ builder

    private static String cur;

    private static void step(String name) {
        cur = name;
    }

    private static void run(Action a) {
        OPS.add(new Op(cur, a, null, 0, null));
    }

    private static void waitTicks(int n) {
        OPS.add(new Op(cur, null, null, n, null));
    }

    private static void until(BooleanSupplier c, int timeoutTicks) {
        OPS.add(new Op(cur, null, c, timeoutTicks, null));
    }

    private static void shot(String name) {
        OPS.add(new Op(cur, null, null, 0, name));
    }

    private static void build() {
        step("window");
        run(mc -> {
            resizeWindow(mc, 1280, 720);
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            // a crashed earlier run may have left a loadout behind
            Wardrobe w = wardrobe();
            for (Slot s : Slot.all()) w.unequip(s);
        });
        waitTicks(10);

        step("title");
        run(mc -> {
            Screen s = ClientCompat.screen();
            if (s instanceof OnboardingScreen) {
                // our onboarding: screenshot it, then mark it done (skipped)
                shot(mc, "00_onboarding");
            } else if (s != null && s.getClass().getSimpleName().contains("Onboarding")) {
                mc.options.onboardAccessibility = false;
            }
            GlobalSettings.setOnboarded(true);
            ClientCompat.openScreen(new TitleScreen());
        });
        until(() -> ClientCompat.screen() instanceof TitleScreen, 100);
        waitTicks(50);
        shot("01_title");

        step("menu_modules");
        run(mc -> {
            ModulesPanel.preselect(null);
            ClientCompat.openScreen(new VibeyScreen(ClientCompat.screen()));
        });
        waitTicks(20);
        shot("02_menu_modules");
        run(mc -> {
            ModulesPanel.preselect("cosmetics");
            ClientCompat.openScreen(new VibeyScreen(new TitleScreen()));
        });
        waitTicks(20);
        shot("03_menu_cosmetics");

        step("menu_about");
        run(mc -> ClientCompat.openScreen(new VibeyScreen(new TitleScreen(), VibeyTabs.ABOUT)));
        waitTicks(20);
        shot("04_menu_about");

        step("wardrobe_title");
        run(mc -> WardrobeUi.openWardrobe(new TitleScreen(), Slot.WINGS));
        waitTicks(30);
        shot("05_wardrobe_title_wings");

        step("world_create");
        run(mc -> {
            ClientCompat.openScreen(new TitleScreen());
            openCreateWorld(mc);
        });
        until(() -> ClientCompat.screen() instanceof CreateWorldScreen, 200);
        waitTicks(10);
        run(mc -> configureFlat((CreateWorldScreen) ClientCompat.screen()));
        waitTicks(10);
        shot("06_create_world");
        run(mc -> pressCreate());
        until(() -> Minecraft.getInstance().player != null && Minecraft.getInstance().level != null && ClientCompat.screen() == null, 1200);
        run(mc -> {
            command(mc, "time set noon");
            command(mc, "weather clear");
            command(mc, "gamerule doDaylightCycle false");
            command(mc, "gamerule advance_time false");
            command(mc, "gamemode creative");
            hideHud(mc, false);
            // the "move with WASD" tutorial toast would cover every screenshot's corner
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        });
        waitTicks(100);
        shot("07_world_first_person");

        step("cosmetics_all_slots");
        run(mc -> {
            requireWorld(mc);
            command(mc, "item replace entity @a weapon.offhand with minecraft:shield");
            Wardrobe w = wardrobe();
            for (Slot s : Slot.all()) w.unequip(s);
            for (String id : new String[]{"vibey:vibe_cape", "vibey:crystal_wings", "vibey:ember_sparks", "vibey:seal_buddy",
                    "vibey:seal_crest_shield", "vibey:seal_beanie", "vibey:seal_backpack"}) equip(w, id);
            hideHud(mc, true);
            look(mc, 0f, 10f);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        waitTicks(40);
        shot("10_cosmetics_front");
        run(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        waitTicks(15);
        shot("11_cosmetics_back");

        step("cosmetics_wings_back");
        run(mc -> {
            requireWorld(mc);
            Wardrobe w = wardrobe();
            for (Slot s : Slot.all()) w.unequip(s);
            equip(w, "vibey:crystal_wings");
            equip(w, "vibey:seal_beanie");
        });
        waitTicks(15);
        shot("12_wings_back");
        run(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        waitTicks(15);
        shot("13_wings_front");

        step("cosmetics_cape_backpack");
        run(mc -> {
            requireWorld(mc);
            Wardrobe w = wardrobe();
            for (Slot s : Slot.all()) w.unequip(s);
            equip(w, "vibey:vibe_cape");
            equip(w, "vibey:seal_backpack");
            equip(w, "vibey:seal_crest_shield");
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        waitTicks(15);
        shot("14_cape_backpack_back");

        step("nametag_icons");
        run(mc -> {
            requireWorld(mc);
            hideHud(mc, false);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            look(mc, 0f, 0f);
            NametagProbe.spawn(mc, 3.5);
        });
        waitTicks(20);
        shot("15_nametag_icons");
        run(mc -> mc.options.keyPlayerList.setDown(true));
        waitTicks(10);
        shot("16_tablist_icons");
        run(mc -> {
            mc.options.keyPlayerList.setDown(false);
            NametagProbe.remove(mc);
        });
        waitTicks(5);

        step("emote_wave");
        emote("vibey:wave", 18, "20_emote_wave_front");
        step("emote_guitar");
        emote("vibey:guitar", 12, "21_emote_guitar_front");
        step("emote_seal_summon");
        emote("vibey:seal_summon", 40, "22_emote_seal_summon_front");
        run(mc -> wardrobe().stopEmote());
        waitTicks(10);

        step("wardrobe_world");
        run(mc -> {
            requireWorld(mc);
            hideHud(mc, false);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            equip(wardrobe(), "vibey:seal_buddy");
            WardrobeUi.openWardrobe(null, Slot.CAPE);
        });
        waitTicks(30);
        shot("30_wardrobe_world_capes");
        run(mc -> WardrobeUi.openWardrobeEmotes(null));
        waitTicks(20);
        shot("31_wardrobe_world_emotes");
        run(mc -> ClientCompat.openScreen(null));
        waitTicks(5);

        step("emote_wheel");
        run(mc -> {
            Wardrobe w = wardrobe();
            String[] wheel = {"vibey:wave", "vibey:cheer", "vibey:guitar", "vibey:vibe_dance", "vibey:sit", "vibey:seal_summon", "vibey:clap", "vibey:shrug"};
            for (int i = 0; i < wheel.length; i++) if (w.wheel(i) == null) w.setWheel(i, wheel[i]);
            WardrobeUi.openWheel(() -> true);
        });
        waitTicks(15);
        shot("32_emote_wheel");
        run(mc -> ClientCompat.openScreen(null));
        waitTicks(5);

        step("social_sidebar");
        run(mc -> {
            SocialModule m = ModuleManager.get().get(SocialModule.class);
            if (m == null) throw new IllegalStateException("social module not loaded");
            ClientCompat.openScreen(new SocialScreen(null, m, SidebarRoute.tab(SidebarRoute.Tab.FRIENDS)));
        });
        waitTicks(30);
        shot("33_social_sidebar");
        run(mc -> ClientCompat.openScreen(null));
        waitTicks(5);

        //? if fabric {
        step("lyrics_sky");
        run(mc -> {
            requireWorld(mc);
            if (!dev.henny.vibeyclient.module.lyrics.LyricsModule.running()) throw new IllegalStateException("lyrics module not running");
            dev.henny.vibeyclient.module.lyrics.library.SongLibrary.rescan();
            var demo = dev.henny.vibeyclient.module.lyrics.library.SongLibrary.find(
                    dev.henny.vibeyclient.module.lyrics.library.SongLibrary.DEMO.toLowerCase(java.util.Locale.ROOT));
            if (demo == null) throw new IllegalStateException("demo song missing");
            hideHud(mc, true);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            look(mc, -45f, -14f);
            var c = dev.henny.vibeyclient.module.lyrics.LyricsConfig.get();
            c.position = dev.henny.vibeyclient.module.lyrics.LyricsConfig.Position.SKY;
            c.applyPreset(dev.henny.vibeyclient.module.lyrics.LyricsConfig.Preset.NEON);
            c.reveal = dev.henny.vibeyclient.module.lyrics.LyricsConfig.Reveal.LINE;
            c.skyMode = dev.henny.vibeyclient.module.lyrics.LyricsConfig.SkyMode.FIXED;
            c.skyYaw = -45;
            c.sky = "vanilla";
            dev.henny.vibeyclient.module.lyrics.Playback.play(demo);
        });
        waitTicks(20);
        run(mc -> {
            dev.henny.vibeyclient.module.lyrics.Playback.seek(25_350);
            dev.henny.vibeyclient.module.lyrics.Playback.pause();
        });
        waitTicks(15);
        shot("34_lyrics_sky");
        run(mc -> {
            dev.henny.vibeyclient.module.lyrics.LyricsConfig.get().sky = "galaxy";
            dev.henny.vibeyclient.module.lyrics.Playback.seek(16_700);
            dev.henny.vibeyclient.module.lyrics.Playback.pause();
        });
        waitTicks(15);
        shot("35_lyrics_galaxy");
        run(mc -> {
            long drawn = dev.henny.vibeyclient.module.lyrics.render.WorldLyrics.drawnFrames;
            dev.henny.vibeyclient.module.lyrics.LyricsConfig.get().sky = "vanilla";
            dev.henny.vibeyclient.module.lyrics.Playback.stop();
            hideHud(mc, false);
            if (drawn <= 0) throw new IllegalStateException("world lyrics were never drawn");
        });
        //?}

        step("host_screen");
        run(mc -> {
            requireWorld(mc);
            hideHud(mc, false);
            look(mc, 0f, 10f);
            ClientCompat.openScreen(new HostScreen(null));
        });
        waitTicks(25);
        shot("40_host_screen");

        step("pause_menu");
        run(mc -> {
            requireWorld(mc);
            ClientCompat.openScreen(new PauseScreen(true));
        });
        waitTicks(20);
        shot("41_pause_menu");
        run(mc -> {
            ClientCompat.openScreen(null);
            Wardrobe w = wardrobe();
            for (Slot s : Slot.all()) w.unequip(s);
        });
        waitTicks(5);
    }

    private static void emote(String id, int ticks, String name) {
        run(mc -> {
            requireWorld(mc);
            Wardrobe w = wardrobe();
            for (Slot s : Slot.all()) w.unequip(s);
            hideHud(mc, true);
            look(mc, 0f, 10f);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            if (AvatarItems.get(id) == null) throw new IllegalStateException("emote missing: " + id);
            w.playEmote(id);
        });
        waitTicks(ticks);
        run(mc -> {
            String playing = wardrobe().playingEmote();
            if (!id.equals(playing)) throw new IllegalStateException("emote not playing at screenshot time: " + playing);
        });
        shot(name);
        run(mc -> wardrobe().stopEmote());
        waitTicks(8);
    }

    // ============================================================================================ helpers

    private static Wardrobe wardrobe() {
        return Services.get(Wardrobe.class).orElseThrow(() -> new IllegalStateException("no Wardrobe service"));
    }

    private static void equip(Wardrobe w, String id) {
        AvatarItem i = AvatarItems.get(id);
        if (i == null) throw new IllegalStateException("item missing: " + id);
        w.equip(i);
        Slot slot = i.slot();
        if (slot != null && !id.equals(w.equipped(slot))) throw new IllegalStateException("equip failed: " + id + " (" + w.lockedReason(i) + ")");
    }

    /** F1 on/off (26.2 moved it from the options to the HUD, which only has a toggle). */
    private static void hideHud(Minecraft mc, boolean hidden) {
        if (ClientCompat.hudHidden() == hidden) return;
        //? if >=26.2 {
        /*mc.gui.hud.toggle();
        *///?} else
        mc.options.hideGui = hidden;
    }

    private static void requireWorld(Minecraft mc) {
        if (mc.player == null || mc.level == null) throw new IllegalStateException("no world");
    }

    private static void look(Minecraft mc, float yaw, float pitch) {
        var p = mc.player;
        p.setYRot(yaw);
        p.yRotO = yaw;
        p.setXRot(pitch);
        p.xRotO = pitch;
        p.yHeadRot = yaw;
        p.yHeadRotO = yaw;
        p.yBodyRot = yaw;
        p.yBodyRotO = yaw;
    }

    /** Runs a command as the integrated server (permission level 4). */
    private static void command(Minecraft mc, String cmd) {
        var server = mc.getSingleplayerServer();
        if (server == null) throw new IllegalStateException("no integrated server");
        server.execute(() -> {
            try {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), cmd);
            } catch (Throwable t) {
                log("command '" + cmd + "' failed: " + t);
            }
        });
    }

    private static void openCreateWorld(Minecraft mc) throws ReflectiveOperationException {
        for (Method m : CreateWorldScreen.class.getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            // only the simple overload (Minecraft, Screen | Runnable); 1.21.2+ also has one with a CreateWorldCallback
            if (m.getName().equals("openFresh") && p.length >= 1 && p[0] == Minecraft.class
                    && java.util.Arrays.stream(p).skip(1).allMatch(c -> c == Screen.class || c == Runnable.class)) {
                Object[] args = new Object[p.length];
                args[0] = mc;
                for (int i = 1; i < p.length; i++) {
                    if (p[i] == Screen.class) args[i] = new TitleScreen();
                    else if (p[i] == Runnable.class) args[i] = (Runnable) () -> ClientCompat.openScreen(new TitleScreen());
                }
                m.invoke(null, args);
                return;
            }
        }
        throw new NoSuchMethodException("CreateWorldScreen.openFresh");
    }

    private static void configureFlat(CreateWorldScreen screen) {
        WorldCreationUiState s = screen.getUiState();
        s.setName("Vibey Tour");
        s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
        s.setAllowCommands(true);
        for (WorldCreationUiState.WorldTypeEntry e : s.getNormalPresetList()) {
            if (e.preset() != null && e.preset().is(WorldPresets.FLAT)) {
                s.setWorldType(e);
                return;
            }
        }
        for (WorldCreationUiState.WorldTypeEntry e : s.getAltPresetList()) {
            if (e.preset() != null && e.preset().is(WorldPresets.FLAT)) {
                s.setWorldType(e);
                return;
            }
        }
        throw new IllegalStateException("no flat preset");
    }

    private static void pressCreate() throws ReflectiveOperationException {
        Screen s = ClientCompat.screen();
        if (s == null) throw new IllegalStateException("no create world screen");
        for (var l : s.children()) {
            if (l instanceof Button b && b.getMessage().getContents() instanceof TranslatableContents tc && tc.getKey().equals("selectWorld.create")) {
                for (Class<?> k = b.getClass(); k != null; k = k.getSuperclass()) {
                    for (Method m : k.getDeclaredMethods()) {
                        if (!m.getName().equals("onPress")) continue;
                        m.setAccessible(true);
                        if (m.getParameterCount() == 0) m.invoke(b);
                        else {
                            Object key = Class.forName("net.minecraft.client.input.KeyEvent")
                                    .getConstructor(int.class, int.class, int.class).newInstance(257, 0, 0);
                            m.invoke(b, key);
                        }
                        return;
                    }
                }
            }
        }
        throw new IllegalStateException("no Create button on " + s.getClass().getName());
    }

    /** GLFW window resize through reflection (the window handle getter and LWJGL differ per version). */
    private static void resizeWindow(Minecraft mc, int w, int h) {
        try {
            Object window = mc.getWindow();
            long handle = 0;
            for (String name : new String[]{"handle", "getWindow"}) {
                try {
                    Method m = window.getClass().getMethod(name);
                    if (m.getReturnType() == long.class) {
                        handle = (long) m.invoke(window);
                        break;
                    }
                } catch (NoSuchMethodException ignored) {
                }
            }
            if (handle == 0) throw new NoSuchMethodException("window handle");
            Class.forName("org.lwjgl.glfw.GLFW").getMethod("glfwSetWindowSize", long.class, int.class, int.class).invoke(null, handle, w, h);
        } catch (Throwable t) {
            log("window resize failed (screenshots keep the default size): " + t);
        }
    }

    // ============================================================================================ screenshots

    /** Saves the last frame to {@code <out>/<name>.png}. */
    private static void shot(Minecraft mc, String name) throws Exception {
        Path dir = Path.of(OUT).toAbsolutePath();
        Files.createDirectories(dir);
        Path file = dir.resolve(name + ".png");
        Method take = null;
        for (Method m : net.minecraft.client.Screenshot.class.getMethods()) {
            if (m.getName().equals("takeScreenshot") && Modifier.isStatic(m.getModifiers())) take = m;
        }
        if (take == null) throw new NoSuchMethodException("Screenshot.takeScreenshot");
        Class<?>[] p = take.getParameterTypes();
        Object[] args = new Object[p.length];
        Consumer<Object> write = img -> writeImage(img, file);
        for (int i = 0; i < p.length; i++) {
            if (p[i] == int.class) args[i] = 1;
            else if (Consumer.class.isAssignableFrom(p[i])) args[i] = write;
            else args[i] = renderTarget(mc, p[i]);
        }
        Object r = take.invoke(null, args);
        if (r != null) writeImage(r, file);
        shots++;
        log("screenshot " + file);
    }

    private static void writeImage(Object img, Path file) {
        try {
            img.getClass().getMethod("writeToFile", Path.class).invoke(img, file);
        } catch (ReflectiveOperationException e) {
            try {
                img.getClass().getMethod("writeToFile", File.class).invoke(img, file.toFile());
            } catch (ReflectiveOperationException e2) {
                Vibey.LOG.error(TAG + "writing {} failed", file, e2);
            }
        } finally {
            if (img instanceof AutoCloseable c) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static Object renderTarget(Minecraft mc, Class<?> type) throws ReflectiveOperationException {
        for (String name : new String[]{"getMainRenderTarget", "mainRenderTarget"}) {
            for (Object owner : new Object[]{mc, mc.gameRenderer}) {
                try {
                    Method m = owner.getClass().getMethod(name);
                    if (type.isAssignableFrom(m.getReturnType())) return m.invoke(owner);
                } catch (NoSuchMethodException ignored) {
                }
            }
        }
        throw new NoSuchMethodException("main render target of type " + type.getName());
    }

    private static void log(String s) {
        Vibey.LOG.info(TAG + "{}", s);
    }

    // ============================================================================================ log tap

    /** Collects ERROR lines (and WARN lines with a stack trace) other code logs during a step. */
    private static final class LogTap {
        private static Object appender;

        static void install() {
            try {
                var root = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger();
                var a = new org.apache.logging.log4j.core.appender.AbstractAppender("VibeyTour", null, null, true,
                        org.apache.logging.log4j.core.config.Property.EMPTY_ARRAY) {
                    @Override
                    public void append(org.apache.logging.log4j.core.LogEvent e) {
                        var level = e.getLevel();
                        boolean error = level.isMoreSpecificThan(org.apache.logging.log4j.Level.ERROR);
                        boolean warnTrace = level == org.apache.logging.log4j.Level.WARN && e.getThrown() != null;
                        if (!error && !warnTrace) return;
                        String msg = e.getMessage() == null ? "" : e.getMessage().getFormattedMessage();
                        if (msg.contains(TAG)) return;
                        String line = e.getLoggerName() + ": " + msg + (e.getThrown() != null ? " (" + e.getThrown() + ")" : "");
                        if (line.length() > 300) line = line.substring(0, 300) + "…";
                        synchronized (LOGGED) {
                            if (LOGGED.size() < 50) LOGGED.add(line);
                        }
                    }
                };
                a.start();
                root.addAppender(a);
                appender = a;
            } catch (Throwable t) {
                log("log tap not available: " + t);
            }
        }

        static void remove() {
            try {
                if (appender != null) {
                    var root = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger();
                    root.removeAppender((org.apache.logging.log4j.core.Appender) appender);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
