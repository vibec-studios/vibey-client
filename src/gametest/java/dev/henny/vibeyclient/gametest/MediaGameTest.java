package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.media.MediaModule;
import dev.henny.vibeyclient.module.media.lyrics.LyricsConfig;
import dev.henny.vibeyclient.module.media.lyrics.LyricsFeature;
import dev.henny.vibeyclient.module.media.lyrics.Playback;
import dev.henny.vibeyclient.module.media.lyrics.fx.MusicAnalyzer;
import dev.henny.vibeyclient.module.media.lyrics.fx.SkyFx;
import dev.henny.vibeyclient.module.media.lyrics.fx.TestTrack;
import dev.henny.vibeyclient.module.media.lyrics.library.Song;
import dev.henny.vibeyclient.module.media.lyrics.library.SongLibrary;
import dev.henny.vibeyclient.module.media.lyrics.world.WorldText;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.hud.editor.HudEditorScreen;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * The media module in the real game: the settings page (en/de), sky lyrics "where you look" at several yaw/pitch
 * angles (rest, delayed glide, settle), the sky reacting to a synthetic test song (before and right after its drop,
 * with and without the soft flash), the analysis cost, and the HUD editor driven with the mouse (drag with snapping,
 * scroll to scale, saved). Screenshots: versions/fabric-1.21.11/build/run/clientGameTest/screenshots (media_*).
 */
public final class MediaGameTest implements FabricClientGameTest {
    private static final String TEST_SONG = "Vibey Media - Sky Test";

    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        if (FabricLoader.getInstance().isModLoaded("vibeylyrics")) return; // see MultiplayerLyricsGameTest.conflict
        guiScale(context, 2);
        context.runOnClient(mc -> {
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            ModuleManager mm = ModuleManager.get();
            if (!mm.isEnabled("media")) throw new AssertionError("media should be on: " + mm.get("media").status());
            if (mm.get("lyrics") != null) throw new AssertionError("old module id lyrics still registered");
            System.out.println("[vibeyclient-media-test] parts: " + MediaModule.instance().describe());
            LyricsFeature f = LyricsFeature.instance();
            System.out.println("[vibeyclient-media-test] state: offset=" + f.offset.get() + " volume=" + f.volume.get() + " preset=" + f.preset.get()
                    + " skyMode=" + LyricsConfig.get().skyMode + " sky=" + LyricsConfig.get().sky + " size=" + LyricsConfig.get().size
                    + " openKey=" + f.open.mapping().saveString());
            if (!LyricsFeature.running()) throw new AssertionError("lyrics part not running");
            // the lyrics part can be switched off alone and back on
            LyricsFeature ly = LyricsFeature.instance();
            ly.enabledSetting.set(false);
            if (LyricsFeature.running() || !mm.isEnabled("media")) throw new AssertionError("lyrics switch off failed");
            ly.enabledSetting.set(true);
            if (!LyricsFeature.running()) throw new AssertionError("lyrics switch on failed");
            HudLayout.resetAll();
            HudLayout.setFlag("intro", true); // the first-run intro is HudGameTest's business
            HudLayout.save();
        });

        ModulesPanel.preselect("media");
        context.setScreen(() -> new VibeyScreen(null));
        context.waitTicks(12);
        context.takeScreenshot("media_1_settings_en");
        context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.GERMAN));
        context.setScreen(() -> new VibeyScreen(null));
        context.waitTicks(12);
        context.takeScreenshot("media_2_settings_de");
        context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
        context.setScreen(() -> new HudEditorScreen(null));
        context.waitTicks(8);
        context.takeScreenshot("media_3_hud_editor_title");
        context.setScreen(() -> null);

        try (TestSingleplayerContext world = context.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeymedia"))
                .create()) {
            world.getServer().runCommand("time set 18000");
            world.getServer().runCommand("weather clear");
            world.getClientWorld().waitForChunksRender();
            int[] p = context.computeOnClient(mc -> new int[]{
                    (int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ())});
            int top = p[1] + 14;
            world.getServer().runCommand(String.format("fill %d %d %d %d %d %d minecraft:glass", p[0] - 1, top, p[2] - 1, p[0] + 1, top, p[2] + 1));
            world.getServer().runCommand(String.format("tp @a %d.5 %d %d.5", p[0], top + 1, p[2]));
            context.waitTicks(60);

            viewFollow(context);
            dynamicSky(context);
            loopback(context);
            hudEditor(context);
            context.setScreen(() -> new net.minecraft.client.gui.screens.PauseScreen(true));
            context.waitTicks(5);
            boolean entry = context.computeOnClient(mc -> dev.henny.vibeyclient.ui.MenuEntries.visible(dev.henny.vibeyclient.ui.MenuEntries.Place.PAUSE)
                    .stream().anyMatch(e -> e.id().equals("media")));
            if (!entry) throw new AssertionError("no Media entry in the pause menu");
            context.takeScreenshot("media_13_pause_menu");
            context.setScreen(() -> null);
            context.runOnClient(mc -> {
                Playback.stop();
                mc.options.hideGui = false;
            });
        }
        guiScale(context, 0);
        context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
        System.out.println("[vibeyclient-media-test] done");
    }

    // ============================================================================================ sky lyrics

    private static void viewFollow(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            mc.options.hideGui = true;
            LyricsConfig c = LyricsConfig.get();
            c.position = LyricsConfig.Position.SKY;
            c.applyPreset(LyricsConfig.Preset.NEON);
            c.reveal = LyricsConfig.Reveal.LINE;
            c.skyMode = LyricsConfig.SkyMode.VIEW;
            c.sky = "vanilla";
            c.volume = 0;
            c.skyFx = true;
            c.save();
            Playback.play(demo());
            Playback.seek(25_350);
            Playback.pause();
            mc.player.setYRot(0);
            mc.player.setXRot(-5);
        });
        context.waitTicks(20);
        float[][] angles = {{0, -5}, {95, -30}, {-150, -55}, {35, 25}};
        for (int i = 0; i < angles.length; i++) {
            float yaw = angles[i][0];
            float pitch = angles[i][1];
            look(context, yaw, pitch);
            context.waitFor(mc -> settled(yaw), 160);
            context.waitTicks(4);
            float[] v = context.computeOnClient(mc -> WorldText.viewState());
            System.out.printf(Locale.ROOT, "[vibeyclient-media-test] view yaw %.0f pitch %.0f -> text at yaw %.1f elevation %.1f%n", yaw, pitch, v[0], v[1]);
            if (Math.abs(wrap(v[0] - yaw)) > 3) throw new AssertionError("sky lyrics did not follow to yaw " + yaw + ": " + v[0]);
            float wantElev = Math.max(9f, Math.min(58f, -pitch + 9f));
            if (Math.abs(v[1] - wantElev) > 3) throw new AssertionError("elevation " + v[1] + ", want " + wantElev);
            context.takeScreenshot("media_4_view_" + (i + 1) + "_yaw" + Math.round(yaw) + "_pitch" + Math.round(pitch));
        }
        // a small look-around keeps the text where it is
        look(context, 45, 20);
        context.waitTicks(4);
        float restYaw = context.computeOnClient(mc -> WorldText.viewState()[0]);
        look(context, 52, 15);
        context.waitTicks(20);
        float[] rest = context.computeOnClient(mc -> WorldText.viewState());
        if (Math.abs(wrap(rest[0] - restYaw)) > 0.5f || rest[2] != 0f) throw new AssertionError("text moved for a small look-around: " + rest[0] + " vs " + restYaw);
        // a fast turn: the text waits a moment, then glides (screenshot mid-glide), then settles
        look(context, 170, -10);
        context.waitTicks(2);
        float[] waiting = context.computeOnClient(mc -> WorldText.viewState());
        context.waitFor(mc -> WorldText.viewState()[2] == 1f && Math.abs(wrap(WorldText.viewState()[0] - 170)) < 70, 60);
        context.takeScreenshot("media_4_view_5_gliding");
        float[] gliding = context.computeOnClient(mc -> WorldText.viewState());
        context.waitFor(mc -> settled(170), 160);
        System.out.printf(Locale.ROOT, "[vibeyclient-media-test] turn 45->170: after 2 ticks %.1f, mid-glide %.1f (gliding %s)%n", waiting[0], gliding[0], gliding[2] == 1f);
        context.runOnClient(mc -> Playback.stop());
    }

    private static boolean settled(float yaw) {
        float[] v = WorldText.viewState();
        return v[2] == 0f && Math.abs(wrap(v[0] - yaw)) < 2f;
    }

    private static void look(ClientGameTestContext context, float yaw, float pitch) {
        context.runOnClient(mc -> {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
            mc.player.yRotO = yaw;
            mc.player.xRotO = pitch;
        });
    }

    // ============================================================================================ dynamic sky

    private static void dynamicSky(ClientGameTestContext context) {
        Song test = context.computeOnClient(mc -> {
            try {
                TestTrack.write(LyricsFeature.songsDir(), TEST_SONG);
            } catch (Exception e) {
                throw new AssertionError("could not write the test song", e);
            }
            SongLibrary.rescan();
            Song s = SongLibrary.find(TEST_SONG.toLowerCase(Locale.ROOT));
            if (s == null || !s.hasAudio()) throw new AssertionError("test song missing: " + SongLibrary.songs());
            return s;
        });
        String[] skies = {"aurora", "galaxy"};
        for (int run = 0; run < skies.length; run++) {
            String sky = skies[run];
            boolean flashes = run == 1;
            context.runOnClient(mc -> {
                mc.options.hideGui = true;
                LyricsConfig c = LyricsConfig.get();
                c.position = LyricsConfig.Position.SKY;
                c.applyPreset(LyricsConfig.Preset.NEON);
                c.sky = sky;
                c.skyFx = true;
                c.fxStrength = 70;
                c.fxFlashes = flashes;
                c.volume = 0;
                c.save();
                mc.player.setYRot(20);
                mc.player.setXRot(-32);
                Playback.play(test);
                Playback.seek(4_500);
            });
            context.waitFor(mc -> Playback.positionMs() >= 7_300, 400);
            float[] before = context.computeOnClient(mc -> SkyFx.snapshot());
            int drops = context.computeOnClient(mc -> MusicAnalyzer.drops);
            context.takeScreenshot("media_5_" + sky + "_before_drop");
            context.waitFor(mc -> MusicAnalyzer.drops > drops, 200);
            context.waitTicks(3);
            float[] after = context.computeOnClient(mc -> SkyFx.snapshot());
            context.takeScreenshot("media_6_" + sky + "_drop" + (flashes ? "_flash" : ""));
            context.waitFor(mc -> Playback.positionMs() >= 12_500, 400);
            context.takeScreenshot("media_7_" + sky + "_groove");
            String stats = context.computeOnClient(mc -> MusicAnalyzer.stats() + " | render " + SkyFx.stats());
            System.out.printf(Locale.ROOT, "[vibeyclient-media-test] %s: before drop %s, after drop %s%n[vibeyclient-media-test] %s%n",
                    sky, java.util.Arrays.toString(before), java.util.Arrays.toString(after), stats);
            if (after[1] <= before[1] + 0.05f) throw new AssertionError("horizon glow did not react to the drop: " + before[1] + " -> " + after[1]);
            if (flashes && after[2] <= 0.01f) throw new AssertionError("no soft flash with flashes on");
            if (!flashes && after[2] > 0f) throw new AssertionError("flash although flashes are off");
            context.runOnClient(mc -> Playback.stop());
        }
        context.runOnClient(mc -> {
            if (MusicAnalyzer.hops < 100) throw new AssertionError("analysis did not run: " + MusicAnalyzer.stats());
            if (MusicAnalyzer.hopMicrosAvg > 2000) throw new AssertionError("analysis too slow: " + MusicAnalyzer.stats());
            long perHop = MusicAnalyzer.allocatedBytes / Math.max(1, MusicAnalyzer.hops);
            System.out.println("[vibeyclient-media-test] analysis allocated " + MusicAnalyzer.allocatedBytes + " bytes in " + MusicAnalyzer.hops + " hops (" + perHop + " per hop)");
            if (perHop > 64) throw new AssertionError("analysis allocates per hop: " + MusicAnalyzer.stats());
            LyricsConfig c = LyricsConfig.get();
            c.sky = "vanilla";
            c.fxFlashes = false;
            c.fxStrength = 50;
            c.save();
            try {
                Files.deleteIfExists(LyricsFeature.songsDir().resolve(TEST_SONG + ".wav"));
                Files.deleteIfExists(LyricsFeature.songsDir().resolve(TEST_SONG + ".lrc"));
            } catch (Exception ignored) {
            }
            SongLibrary.rescan();
        });
    }

    // ============================================================================================ PC sound

    /** Windows: the WASAPI loopback helper starts and streams (a silent Java Sound stream keeps the mixer running). */
    private static void loopback(ClientGameTestContext context) {
        if (!dev.henny.vibeyclient.module.media.lyrics.fx.MusicAnalyzer.systemAudioSupported()) return;
        Song test = context.computeOnClient(mc -> {
            try {
                TestTrack.write(LyricsFeature.songsDir(), TEST_SONG);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            SongLibrary.rescan();
            return SongLibrary.find(TEST_SONG.toLowerCase(Locale.ROOT));
        });
        context.runOnClient(mc -> {
            Playback.play(test);
            dev.henny.vibeyclient.module.media.lyrics.fx.LoopbackCapture.pin(true);
        });
        long start = System.currentTimeMillis();
        // the helper compiles its C# part first (a few seconds, longer on a busy machine); environment dependent, so
        // this reports instead of failing
        for (int i = 0; i < 800; i++) {
            boolean ok = context.computeOnClient(mc -> dev.henny.vibeyclient.module.media.lyrics.fx.LoopbackCapture.state()
                    == dev.henny.vibeyclient.module.media.lyrics.fx.LoopbackCapture.State.RUNNING
                    && dev.henny.vibeyclient.module.media.lyrics.fx.LoopbackCapture.ring().end() > 3000);
            if (ok) break;
            context.waitTick();
        }
        long end = context.computeOnClient(mc -> dev.henny.vibeyclient.module.media.lyrics.fx.LoopbackCapture.ring().end());
        int rate = context.computeOnClient(mc -> dev.henny.vibeyclient.module.media.lyrics.fx.LoopbackCapture.ring().rate());
        String state = context.computeOnClient(mc -> String.valueOf(dev.henny.vibeyclient.module.media.lyrics.fx.LoopbackCapture.state()));
        System.out.println("[vibeyclient-media-test] PC sound helper " + state + " after " + (System.currentTimeMillis() - start) + " ms, "
                + end + " samples at " + rate + " Hz");
        context.runOnClient(mc -> {
            Playback.stop();
            dev.henny.vibeyclient.module.media.lyrics.fx.LoopbackCapture.pin(false);
            dev.henny.vibeyclient.module.media.lyrics.fx.LoopbackCapture.stopNow();
            try {
                Files.deleteIfExists(LyricsFeature.songsDir().resolve(TEST_SONG + ".wav"));
                Files.deleteIfExists(LyricsFeature.songsDir().resolve(TEST_SONG + ".lrc"));
            } catch (Exception ignored) {
            }
            SongLibrary.rescan();
        });
    }

    // ============================================================================================ HUD editor

    private static void hudEditor(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            mc.options.hideGui = false;
            mc.player.setXRot(-10);
            LyricsConfig c = LyricsConfig.get();
            c.position = LyricsConfig.Position.HUD;
            c.applyPreset(LyricsConfig.Preset.MINIMAL);
            c.pill = true;
            c.save();
            Playback.play(demo());
            Playback.seek(11_000);
            Playback.pause();
        });
        context.waitTicks(10);
        context.takeScreenshot("media_8_hud_default");
        context.setScreen(() -> new HudEditorScreen(null));
        context.waitTicks(8);
        context.takeScreenshot("media_9_hud_editor");

        // drag the now playing bar to the top left corner (it snaps to the margin), screenshot while held
        HudElement bar = context.computeOnClient(mc -> LyricsFeature.instance().hudBar);
        HudElement text = context.computeOnClient(mc -> LyricsFeature.instance().hudText);
        double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
        HudLayout.Box b = box(context, bar);
        double sx = (b.x + b.w / 2) * scale;
        double sy = (b.y + b.h / 2) * scale;
        context.getInput().setCursorPos(sx, sy);
        context.waitTicks(2);
        context.getInput().holdMouse(0);
        context.waitTicks(1);
        double tx = (b.w / 2 + 5) * scale;
        double ty = (b.h / 2 + 6) * scale;
        for (int i = 1; i <= 10; i++) {
            context.getInput().setCursorPos(sx + (tx - sx) * i / 10, sy + (ty - sy) * i / 10);
            context.waitTicks(1);
        }
        context.takeScreenshot("media_10_hud_dragging");
        context.getInput().releaseMouse(0);
        context.waitTicks(2);
        HudLayout.Box moved = box(context, bar);
        System.out.printf(Locale.ROOT, "[vibeyclient-media-test] bar moved from %.0f,%.0f to %.0f,%.0f (%s)%n", b.x, b.y, moved.x, moved.y,
                context.computeOnClient(mc -> HudLayout.placement(bar).anchor));
        if (moved.x > 6 || moved.y > 6) throw new AssertionError("bar did not snap to the top left: " + moved.x + "," + moved.y);

        // scroll over the lyrics to make them bigger, then drag them to the lower right third
        HudLayout.Box t = box(context, text);
        context.getInput().setCursorPos((t.x + t.w / 2) * scale, (t.y + t.h / 2) * scale);
        context.waitTicks(1);
        for (int i = 0; i < 5; i++) {
            context.getInput().scroll(1);
            context.waitTicks(1);
        }
        float s = context.computeOnClient(mc -> HudLayout.placement(text).scale);
        if (s < 1.2f) throw new AssertionError("lyrics did not scale up: " + s);
        t = box(context, text);
        double lx = (t.x + t.w / 2) * scale;
        double ly = (t.y + t.h / 2) * scale;
        context.getInput().setCursorPos(lx, ly);
        context.getInput().holdMouse(0);
        context.waitTicks(1);
        for (int i = 1; i <= 8; i++) {
            context.getInput().setCursorPos(lx + 60 * scale * i / 8, ly + 70 * scale * i / 8);
            context.waitTicks(1);
        }
        context.getInput().releaseMouse(0);
        context.waitTicks(3);
        context.takeScreenshot("media_11_hud_scaled_moved");
        context.getInput().pressKey(Keys.ESCAPE);
        context.waitTicks(10);
        if (context.computeOnClient(mc -> mc.screen) != null) throw new AssertionError("editor did not close with Esc");
        context.takeScreenshot("media_12_hud_result");
        boolean saved = context.computeOnClient(mc -> {
            Path f = HudLayout.file();
            try {
                return Files.isRegularFile(f) && Files.readString(f).contains("media.lyrics.bar");
            } catch (Exception e) {
                return false;
            }
        });
        if (!saved) throw new AssertionError("hud.json not written");
        context.runOnClient(mc -> {
            HudLayout.resetAll();
            HudLayout.save();
            LyricsConfig c = LyricsConfig.get();
            c.position = LyricsConfig.Position.SKY;
            c.applyPreset(LyricsConfig.Preset.LAVENDER);
            c.save();
        });
    }

    private static HudLayout.Box box(ClientGameTestContext context, HudElement e) {
        return context.computeOnClient(mc -> HudLayout.box(e, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), new HudLayout.Box()));
    }

    // ============================================================================================ util

    private static Song demo() {
        Song demo = SongLibrary.find(SongLibrary.DEMO.toLowerCase(Locale.ROOT));
        if (demo == null) throw new AssertionError("Demo song missing from " + SongLibrary.songs());
        return demo;
    }

    private static float wrap(float d) {
        return ((d % 360f) + 540f) % 360f - 180f;
    }

    private static void guiScale(ClientGameTestContext context, int scale) {
        context.runOnClient(mc -> {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
        });
    }

    @SuppressWarnings("unused")
    private static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
