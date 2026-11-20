package dev.henny.vibeyclient.dev;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.Services;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.model.Slot;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi;
import dev.henny.vibeyclient.module.multiplayer.screen.HostScreen;
import dev.henny.vibeyclient.module.social.SocialModule;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.VibeyTabs;
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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Dev-only runtime tour for every version and both loaders (development environment AND
 * {@code -Dvibeyclient.tourB=<outdir>} or env {@code VIBEYCLIENT_TOUR_B=<outdir>}; separate from
 * {@link DevTour}'s switch so the two never run at once; relative paths are resolved against
 * the game directory). Walks through title screen → onboarding skipped → Vibey menu (modules, about) → a flat
 * creative world ({@code saves/vibeytour}, created on the first run) → one cosmetic per slot in third person
 * front/back → three emotes mid-animation → wardrobe, emote wheel, social sidebar, lyrics demo in the sky (Fabric),
 * pause menu, host screen → quit. Every step logs {@code [vibeyclient-tourb] PASS|FAIL <step>}; warnings/errors logged
 * during a step are attached to it. Screenshots ({@code NN_name.png}) and {@code tour.txt} go to the out dir.
 */
public final class DevTourB {
    private DevTourB() {}

    private static final String OUT = outProperty();
    private static final boolean ON = OUT != null && !OUT.isBlank() && Vibey.platform().isDevelopment();
    private static final String WORLD = "vibeytour";
    private static final String[] ITEMS = {
            "vibey:vibe_cape", "vibey:crystal_wings", "vibey:snowflake_orbit", "vibey:seal_buddy",
            "vibey:seal_crest_shield", "vibey:seal_beanie", "vibey:seal_backpack"};

    private static List<Step> steps;
    private static int index;
    private static int wait;
    private static int stepTicks;
    private static int shotNo;
    private static Path out;
    private static final List<String> results = new ArrayList<>();
    private static final List<String> stepLog = new ArrayList<>();
    private static int pass, fail;
    private static boolean finished;

    public static boolean enabled() {
        return ON;
    }

    private static String outProperty() {
        String p = System.getProperty("vibeyclient.tourB");
        if (p == null || p.isBlank()) p = System.getenv("VIBEYCLIENT_TOUR_B");
        return p;
    }

    // ============================================================================================ driver

    /** End of every client tick (VibeyCore). */
    public static void tick() {
        if (!ON || finished) return;
        if (steps == null) start();
        if (wait > 0) {
            wait--;
            return;
        }
        if (index >= steps.size()) {
            finish();
            return;
        }
        Step s = steps.get(index);
        stepTicks++;
        boolean done;
        try {
            done = s.act.run();
        } catch (Throwable t) {
            Vibey.LOG.error("[vibeyclient-tourb] step {} threw", s.name, t);
            report(s, false, t.toString());
            next(s);
            return;
        }
        if (done) {
            if (s.check) report(s, true, null);
            next(s);
        } else if (stepTicks > s.timeout) {
            report(s, false, "timed out after " + s.timeout + " ticks");
            next(s);
            if (s.fatal) skipRest("needs " + s.name);
        }
    }

    private static void next(Step s) {
        index++;
        stepTicks = 0;
        wait = s.after;
    }

    /** After a fatal failure: report the remaining checks as skipped and go straight to the final (quit) step. */
    private static void skipRest(String why) {
        while (index < steps.size() && !steps.get(index).always) {
            Step s = steps.get(index++);
            if (s.check) report(s, false, "skipped (" + why + ")");
        }
        wait = 0;
    }

    private static void report(Step s, boolean ok, String detail) {
        List<String> errs;
        synchronized (stepLog) {
            errs = new ArrayList<>(stepLog);
            stepLog.clear();
        }
        // a step that logged errors is a failure (module hooks catch and log their exceptions)
        boolean hadErrors = errs.stream().anyMatch(e -> e.startsWith("ERROR"));
        if (ok && hadErrors) {
            ok = false;
            detail = "errors logged";
        }
        String line = (ok ? "PASS " : "FAIL ") + s.name + (detail == null ? "" : " - " + detail);
        if (ok) pass++;
        else fail++;
        results.add(line);
        for (String e : errs) results.add("    " + e);
        Vibey.LOG.info("[vibeyclient-tourb] {}", line);
    }

    private static void start() {
        Minecraft mc = Minecraft.getInstance();
        out = Path.of(OUT);
        if (!out.isAbsolute()) out = mc.gameDirectory.toPath().resolve(OUT);
        try {
            Files.createDirectories(out);
        } catch (IOException e) {
            Vibey.LOG.error("[vibeyclient-tourb] cannot create {}", out, e);
        }
        installAppender();
        Vibey.LOG.info("[vibeyclient-tourb] start on {} {}, screenshots to {}", Vibey.platform().loader(), dev.henny.vibeyclient.BuildInfo.MINECRAFT, out);
        steps = build();
    }

    // ============================================================================================ the tour

    private static List<Step> build() {
        List<Step> l = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        final boolean[] sawOnboarding = {false};
        final String[] loadWarning = {null};

        // --- start: wait for the first real screen (vanilla accessibility onboarding, Vibey onboarding or title)
        l.add(check("game started", 20 * 180, () -> {
            Screen s = ClientCompat.screen();
            if (stepTicks % 200 == 0) {
                log("waiting: screen " + (s == null ? "none" : s.getClass().getName()) + ", loading " + loading(mc));
                shot("waiting");
            }
            if (loading(mc)) return false;
            if (s == null) return false;
            if (s instanceof OnboardingScreen) {
                sawOnboarding[0] = true;
                return true;
            }
            if (s instanceof TitleScreen) {
                if (loadWarning[0] != null) throw new IllegalStateException("mod loading warning screen: " + loadWarning[0]);
                return true;
            }
            if (s.getClass().getSimpleName().equals("LoadingErrorScreen") && loadWarning[0] == null) {
                // NeoForge's "Warning while loading mods" screen: note it, go on to the title screen
                shot("loading_warning");
                loadWarning[0] = s.getTitle().getString();
                ClientCompat.openScreen(new TitleScreen());
                return false;
            }
            if (s.getClass().getSimpleName().contains("Onboarding") && stepTicks > 40) {
                // a fresh game directory opens vanilla's accessibility onboarding first
                mc.options.onboardAccessibility = false;
                mc.options.save();
                ClientCompat.openScreen(new TitleScreen());
            }
            return false;
        }).fatal().after(40));
        l.add(act("english", () -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH)).after(10));
        l.add(check("onboarding", 100, () -> {
            if (ClientCompat.screen() instanceof OnboardingScreen) shot("onboarding");
            else if (!GlobalSettings.onboarded()) throw new IllegalStateException("not onboarded but no onboarding screen");
            return true;
        }).after(5));
        l.add(check("onboarding skipped", 100, () -> {
            GlobalSettings.setOnboarded(true);
            if (!(ClientCompat.screen() instanceof TitleScreen)) ClientCompat.openScreen(new TitleScreen());
            return true;
        }).after(60));
        l.add(check("title screen", 100, () -> expect(TitleScreen.class) && shot("title")).after(5));
        l.add(act("open Vibey menu", () -> ClientCompat.openScreen(new VibeyScreen(ClientCompat.screen(), VibeyTabs.MODULES))).after(30));
        l.add(check("Vibey menu: modules", 100, () -> expect(VibeyScreen.class) && "modules".equals(((VibeyScreen) ClientCompat.screen()).tab()) && shot("menu_modules")).after(5));
        l.add(act("about tab", () -> ((VibeyScreen) ClientCompat.screen()).showTab(VibeyTabs.ABOUT)).after(25));
        l.add(check("Vibey menu: about", 100, () -> expect(VibeyScreen.class) && VibeyTabs.ABOUT.equals(((VibeyScreen) ClientCompat.screen()).tab()) && shot("menu_about")).after(5));

        // --- world
        l.add(act("open world", () -> {
            ClientCompat.openScreen(new TitleScreen());
            if (Files.isDirectory(mc.gameDirectory.toPath().resolve("saves").resolve(WORLD))) {
                log("loading existing world " + WORLD);
                mc.createWorldOpenFlows().openWorld(WORLD, () -> ClientCompat.openScreen(new TitleScreen()));
            } else {
                log("creating flat world " + WORLD);
                openCreateWorld(mc);
            }
        }).after(20));
        l.add(new Step("create world", 20 * 30, false, () -> {
            Screen s = ClientCompat.screen();
            if (!(s instanceof CreateWorldScreen c)) return true; // existing world: nothing to create
            WorldCreationUiState ui = c.getUiState();
            ui.setName(WORLD);
            ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            ui.setAllowCommands(true);
            for (WorldCreationUiState.WorldTypeEntry e : ui.getNormalPresetList()) if (e.preset() != null && e.preset().is(WorldPresets.FLAT)) ui.setWorldType(e);
            for (WorldCreationUiState.WorldTypeEntry e : ui.getAltPresetList()) if (e.preset() != null && e.preset().is(WorldPresets.FLAT)) ui.setWorldType(e);
            pressButton(s, "selectWorld.create");
            return true;
        }));
        l.add(check("world joined", 20 * 120, () -> {
            pressConfirm();
            return ClientCompat.inWorld() && ClientCompat.screen() == null;
        }).fatal().after(100));
        l.add(check("world setup", 100, () -> {
            var c = mc.player.connection;
            c.sendCommand("time set 6000");
            c.sendCommand("weather clear 999999");
            c.sendCommand("item replace entity @s weapon.offhand with shield");
            c.sendCommand("item replace entity @s weapon.mainhand with diamond_sword");
            face(mc, 0f, 0f);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            return true;
        }).after(40));
        l.add(act("clear chat", () -> clearChat(mc)).after(5));
        l.add(check("in world (first person)", 100, () -> shot("world_first_person")).after(5));

        // --- cosmetics
        l.add(check("equip one item per slot", 100, () -> {
            Wardrobe w = Services.get(Wardrobe.class).orElseThrow(() -> new IllegalStateException("no Wardrobe service"));
            List<String> missing = new ArrayList<>();
            for (String id : ITEMS) {
                Item item = Cosmetico.items().get(id);
                if (item == null) {
                    missing.add(id);
                    continue;
                }
                w.equip(item);
            }
            for (Slot s : Slot.all()) if (w.equipped(s) == null) missing.add("slot " + s.id());
            if (!missing.isEmpty()) throw new IllegalStateException("missing: " + missing);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            face(mc, 0f, 0f);
            hud(mc, false);
            return true;
        }).after(40));
        l.add(check("cosmetics front", 100, () -> shot("cosmetics_front")).after(5));
        l.add(act("camera back", () -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK)).after(20));
        l.add(check("cosmetics back", 100, () -> shot("cosmetics_back")).after(5));
        l.add(act("camera side", () -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            face(mc, 0f, 25f);
        }).after(20));
        l.add(check("cosmetics back from above", 100, () -> shot("cosmetics_back_above")).after(5));
        l.add(act("first person shield", () -> {
            hud(mc, true);
            clearChat(mc);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            face(mc, 0f, 0f);
        }).after(20));
        l.add(check("shield first person", 100, () -> shot("shield_first_person")).after(5));

        // --- nametag icons on two fake players (one with Vibey + badges) and in the tab list
        l.add(act("nametag players", () -> {
            try {
                NametagProbe.spawn(mc, 3.5);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            face(mc, 0f, 0f);
        }).after(5));
        l.add(check("nametag icons", 100, () -> shot("nametag_icons")).after(20));
        l.add(act("tab list down", () -> mc.options.keyPlayerList.setDown(true)).after(5));
        l.add(check("tab list icons", 100, () -> shot("tablist_icons")).after(10));
        l.add(act("nametag players gone", () -> {
            mc.options.keyPlayerList.setDown(false);
            NametagProbe.remove(mc);
        }).after(5));

        // --- emotes
        emote(l, mc, "wave", 18);
        emote(l, mc, "guitar", 24);
        emote(l, mc, "seal_summon", 30);

        // --- screens in the world
        l.add(act("first person for the screens", () -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            hud(mc, true);
            clearChat(mc);
        }).after(5));
        l.add(act("open wardrobe", () -> WardrobeUi.openWardrobe(null)).after(40));
        l.add(check("wardrobe", 100, () -> expectName("WardrobeScreen") && shot("wardrobe")).after(5));
        l.add(act("close wardrobe", () -> ClientCompat.openScreen(null)).after(10));
        final boolean[] wheelHeld = {true};
        l.add(act("open emote wheel", () -> WardrobeUi.openWheel(() -> wheelHeld[0])).after(30));
        l.add(check("emote wheel", 100, () -> expectName("EmoteWheelScreen") && shot("emote_wheel")).after(5));
        l.add(act("close emote wheel", () -> ClientCompat.openScreen(null)).after(10));
        l.add(act("open social sidebar", () -> {
            SocialModule m = ModuleManager.get().get(SocialModule.class);
            if (m == null) throw new IllegalStateException("no social module");
            if (!m.isEnabled()) throw new IllegalStateException("social module disabled: " + m.status());
            m.openSidebar(null);
        }).after(40));
        l.add(check("social sidebar", 100, () -> expectName("SocialScreen") && shot("social_sidebar")).after(5));
        l.add(act("close social", () -> ClientCompat.openScreen(null)).after(10));
        lyrics(l, mc);
        l.add(act("open pause menu", () -> ClientCompat.openScreen(new PauseScreen(true))).after(30));
        l.add(check("pause menu", 100, () -> expect(PauseScreen.class) && shot("pause_menu")).after(5));
        l.add(act("open host screen", () -> ClientCompat.openScreen(new HostScreen(null))).after(30));
        l.add(check("host screen", 100, () -> expect(HostScreen.class) && shot("host_screen")).after(5));
        l.add(new Step("quit", 100, true, () -> {
            ClientCompat.openScreen(null);
            return true;
        }).always());
        return l;
    }

    private static void emote(List<Step> l, Minecraft mc, String name, int midTicks) {
        String id = "vibey:" + name;
        l.add(act("emote " + name, () -> {
            Wardrobe w = Services.get(Wardrobe.class).orElseThrow();
            if (Cosmetico.items().get(id) == null) throw new IllegalStateException("no emote " + id);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            face(mc, 0f, 0f);
            hud(mc, false);
            w.playEmote(id);
        }).after(midTicks));
        l.add(check("emote " + name + " playing", 100, () -> {
            String playing = Services.get(Wardrobe.class).orElseThrow().playingEmote();
            if (!id.equals(playing)) throw new IllegalStateException("playing " + playing);
            return shot("emote_" + name);
        }).after(12));
        l.add(check("emote " + name + " later", 100, () -> shot("emote_" + name + "_2")).after(5));
        l.add(act("stop " + name, () -> Services.get(Wardrobe.class).orElseThrow().stopEmote()).after(15));
    }

    private static void lyrics(List<Step> l, Minecraft mc) {
        //? if fabric {
        l.add(act("lyrics demo", () -> {
            var m = dev.henny.vibeyclient.module.media.lyrics.LyricsFeature.instance();
            if (m == null || !m.isEnabled()) throw new IllegalStateException("lyrics module not running");
            dev.henny.vibeyclient.module.media.lyrics.library.SongLibrary.ensureFolder();
            var songs = dev.henny.vibeyclient.module.media.lyrics.library.SongLibrary.rescan();
            var demo = songs.stream().filter(s -> s.id().equals(dev.henny.vibeyclient.module.media.lyrics.library.SongLibrary.DEMO.toLowerCase(java.util.Locale.ROOT)))
                    .findFirst().orElse(songs.isEmpty() ? null : songs.get(0));
            if (demo == null) throw new IllegalStateException("no demo song");
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            face(mc, 0f, -35f);
            dev.henny.vibeyclient.module.media.lyrics.Playback.play(demo);
        }).after(20 * 9));
        l.add(check("lyrics sky", 100, () -> {
            if (!dev.henny.vibeyclient.module.media.lyrics.Playback.isPlaying()) throw new IllegalStateException("not playing: " + dev.henny.vibeyclient.module.media.lyrics.Playback.error());
            return shot("lyrics_sky");
        }).after(60));
        l.add(check("lyrics sky later", 100, () -> shot("lyrics_sky_2")).after(5));
        l.add(act("stop lyrics", () -> {
            dev.henny.vibeyclient.module.media.lyrics.Playback.stop();
            face(mc, 0f, 0f);
        }).after(10));
        //?}
    }

    // ============================================================================================ helpers

    private static Step act(String name, ThrowingRunnable r) {
        return new Step(name, 100, false, () -> {
            r.run();
            return true;
        });
    }

    private static Step check(String name, int timeout, Act a) {
        return new Step(name, timeout, true, a);
    }

    private static boolean expect(Class<? extends Screen> type) {
        Screen s = ClientCompat.screen();
        if (type.isInstance(s)) return true;
        throw new IllegalStateException("expected " + type.getSimpleName() + ", screen is " + (s == null ? "none" : s.getClass().getName()));
    }

    private static boolean expectName(String simpleName) {
        Screen s = ClientCompat.screen();
        if (s != null && s.getClass().getSimpleName().equals(simpleName)) return true;
        throw new IllegalStateException("expected " + simpleName + ", screen is " + (s == null ? "none" : s.getClass().getName()));
    }

    private static void face(Minecraft mc, float yaw, float pitch) {
        var p = mc.player;
        if (p == null) return;
        p.setYRot(yaw);
        p.setXRot(pitch);
        p.yRotO = yaw;
        p.xRotO = pitch;
        p.setYHeadRot(yaw);
        p.yBodyRot = yaw;
        p.yBodyRotO = yaw;
    }

    /** F1 on/off. */
    private static void hud(Minecraft mc, boolean visible) {
        //? if >=26.2 {
        /*if (mc.gui.hud.isHidden() == visible) mc.gui.hud.toggle();
        *///?} else
        mc.options.hideGui = !visible;
    }

    private static void clearChat(Minecraft mc) {
        //? if >=26.2 {
        /*mc.gui.hud.getChat().clearMessages(false);
        *///?} else
        mc.gui.getChat().clearMessages(false);
    }

    private static boolean loading(Minecraft mc) {
        //? if >=26.2 {
        /*return mc.gui.overlay() != null;
        *///?} else
        return mc.getOverlay() != null;
    }

    private static void openCreateWorld(Minecraft mc) {
        //? if >=1.21.9 {
        CreateWorldScreen.openFresh(mc, () -> ClientCompat.openScreen(new TitleScreen()));
        //?} else
        /*CreateWorldScreen.openFresh(mc, new TitleScreen());*/
    }

    /** Presses the button whose label is the translation {@code key}. */
    private static void pressButton(Screen s, String key) {
        for (var c : s.children()) {
            if (c instanceof Button b && b.getMessage().getContents() instanceof TranslatableContents tc && tc.getKey().equals(key)) {
                press(b);
                return;
            }
        }
        throw new IllegalStateException("no button " + key + " on " + s.getClass().getName());
    }

    private static void press(Button b) {
        //? if >=1.21.9 {
        b.onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
        //?} else
        /*b.onPress();*/
    }

    /** Upgrade/backup or experimental-features prompts on world load: continue without backup. */
    private static void pressConfirm() {
        Screen s = ClientCompat.screen();
        if (s == null) return;
        String n = s.getClass().getSimpleName();
        if (!n.contains("Backup") && !n.contains("Confirm")) return;
        for (var c : s.children()) {
            if (c instanceof Button b && b.getMessage().getContents() instanceof TranslatableContents tc
                    && (tc.getKey().equals("selectWorld.backupJoinSkipButton") || tc.getKey().equals("gui.yes") || tc.getKey().equals("gui.proceed"))) {
                log("pressed " + tc.getKey() + " on " + n);
                press(b);
                return;
            }
        }
    }

    private static boolean shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        String file = String.format("%02d_%s.png", ++shotNo, name);
        Path target = out.resolve(file);
        //? if >=26.2 {
        /*var rt = mc.gameRenderer.mainRenderTarget();
        *///?} else
        var rt = mc.getMainRenderTarget();
        //? if >=1.21.5 {
        net.minecraft.client.Screenshot.takeScreenshot(rt, img -> save(img, target));
        //?} else
        /*save(net.minecraft.client.Screenshot.takeScreenshot(rt), target);*/
        return true;
    }

    private static void save(com.mojang.blaze3d.platform.NativeImage img, Path target) {
        try (img) {
            img.writeToFile(target);
            log("screenshot " + target);
        } catch (Exception e) {
            Vibey.LOG.error("[vibeyclient-tourb] screenshot {} failed", target, e);
        }
    }

    private static void finish() {
        finished = true;
        String summary = "tour done on " + Vibey.platform().loader() + " " + dev.henny.vibeyclient.BuildInfo.MINECRAFT + ": " + pass + " passed, " + fail + " failed";
        results.add(0, summary);
        Vibey.LOG.info("[vibeyclient-tourb] {}", summary);
        try {
            Files.writeString(out.resolve("tour.txt"), String.join("\n", results) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            Vibey.LOG.error("[vibeyclient-tourb] cannot write tour.txt", e);
        }
        Wardrobe w = Services.get(Wardrobe.class).orElse(null);
        if (w != null) for (Slot s : Slot.all()) w.unequip(s);
        try {
            hud(Minecraft.getInstance(), true);
        } catch (RuntimeException ignored) {
        }
        dev.henny.vibeyclient.api.Tasks.later(1500, () -> dev.henny.vibeyclient.api.Tasks.onMain(() -> Minecraft.getInstance().stop()));
    }

    private static void log(String s) {
        Vibey.LOG.info("[vibeyclient-tourb] {}", s);
    }

    /** Vanilla noise of a dev (offline) account, not ours. */
    private static final java.util.Set<String> IGNORED_LOGGERS = java.util.Set.of(
            "net.minecraft.client.multiplayer.AccountProfileKeyPairManager", "net.minecraft.client.multiplayer.ProfileKeyPairManager",
            "com.mojang.realmsclient.client.RealmsClient");

    /** Collects WARN/ERROR lines (other than the tour's own) so a step that logged errors is reported. */
    private static void installAppender() {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            AbstractAppender app = new AbstractAppender("VibeyTourB", null, null, true, null) {
                @Override
                public void append(LogEvent e) {
                    if (e.getLevel().isMoreSpecificThan(Level.WARN)) {
                        String msg = e.getMessage() == null ? "" : e.getMessage().getFormattedMessage();
                        if (msg.contains("[vibeyclient-tour") || IGNORED_LOGGERS.contains(e.getLoggerName())) return;
                        Throwable t = e.getThrown();
                        String line = e.getLevel() + " " + e.getLoggerName() + ": " + msg + (t == null ? "" : " (" + t + ")");
                        if (line.length() > 400) line = line.substring(0, 400) + "…";
                        synchronized (stepLog) {
                            if (stepLog.size() < 20) stepLog.add(line);
                        }
                    }
                }
            };
            app.start();
            ctx.getConfiguration().getRootLogger().addAppender(app, Level.WARN, null);
            ctx.updateLoggers();
        } catch (Throwable t) {
            Vibey.LOG.warn("[vibeyclient-tourb] no log capture: {}", t.toString());
        }
    }

    // ============================================================================================ types

    @FunctionalInterface
    private interface Act {
        /** True when the step is done, false to run it again next tick. */
        boolean run() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class Step {
        final String name;
        final int timeout;
        final boolean check;
        final Act act;
        int after;
        boolean fatal, always;

        Step(String name, int timeout, boolean check, Act act) {
            this.name = name;
            this.timeout = timeout;
            this.check = check;
            this.act = act;
        }

        Step after(int ticks) {
            after = ticks;
            return this;
        }

        Step fatal() {
            fatal = true;
            return this;
        }

        Step always() {
            always = true;
            return this;
        }
    }
}
