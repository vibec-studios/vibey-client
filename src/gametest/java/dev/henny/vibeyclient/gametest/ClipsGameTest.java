package dev.henny.vibeyclient.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.MixinHealth;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.module.clips.ClipSaver;
import dev.henny.vibeyclient.module.clips.ClipsFiles;
import dev.henny.vibeyclient.module.clips.ClipsHooks;
import dev.henny.vibeyclient.module.clips.ClipsModule;
import dev.henny.vibeyclient.module.clips.ClipsStats;
import dev.henny.vibeyclient.module.clips.Recorder;
import dev.henny.vibeyclient.module.clips.capture.FrameGrabber;
import dev.henny.vibeyclient.module.clips.store.ClipStore;
import dev.henny.vibeyclient.module.clips.ui.ClipViewScreen;
import dev.henny.vibeyclient.module.clips.ui.ClipsGalleryScreen;
import dev.henny.vibeyclient.media.ffmpeg.Ffmpeg;
import dev.henny.vibeyclient.media.player.MediaPlayer;
import dev.henny.vibeyclient.media.ffmpeg.FfmpegPurpose;
import dev.henny.vibeyclient.media.ffmpeg.FfmpegSetupScreen;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Clips in the real game (Fabric 1.21.11, docs/CLIPS.md): settings page (en/de), first-time hint, recording dot,
 * pauses (inventory yes, chat no), hotkey save (valid MP4 + contract JSON + thumbnail), an automatic multi-kill clip,
 * server rules {@code clips.auto} / {@code clips}, the gallery and the player (keep, rename, delete), and the frame time
 * with and without the buffer. Screenshots: versions/fabric-1.21.11/build/run/clientGameTest/screenshots/NNNN_clips_*.png.
 * Needs an FFmpeg (our download or the PATH).
 */
public final class ClipsGameTest implements FabricClientGameTest {
    private static final String P = "[vibeyclient-clips-test] ";

    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            ClipsModule m = m();
            if (m == null || !m.isEnabled()) throw new AssertionError("clips module should be on");
            for (String key : new String[] {"clips", "clips.auto"}) {
                if (!ServerRules.supportedKeys().contains(key)) throw new AssertionError("rule key not declared: " + key);
            }
            if (m.buffer.on()) throw new AssertionError("buffer must be off by default");
            if (m.inInventories.on()) throw new AssertionError("no recording in inventories by default");
            if (m.ramCap.get() != 96) throw new AssertionError("RAM cap default 96 MB");
            if (Math.abs(m.folderLimit.get() - 2f) > 0.001) throw new AssertionError("folder limit default 2 GB");
            Recorder.ignoreFocusForTest = true;
            if (!MixinHealth.failures().isEmpty()) System.out.println(P + "hook failures: " + MixinHealth.failures());
        });
        if (Boolean.getBoolean("vibeyclient.clips.lgplTest")) {
            // the real consent + download path: ignore the PATH FFmpeg, ask, download BtbN's LGPL build into the run dir
            context.runOnClient(mc -> {
                Ffmpeg.hiddenForTest = true;
                Ffmpeg.refresh();
            });
            context.setScreen(() -> new FfmpegSetupScreen(null, FfmpegPurpose.RECORDING));
            context.waitTicks(10);
            shot(context, "clips_00_ffmpeg_consent_en");
            context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.GERMAN));
            context.setScreen(() -> new FfmpegSetupScreen(null, FfmpegPurpose.RECORDING));
            context.waitTicks(10);
            shot(context, "clips_00_ffmpeg_consent_de");
            context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
            context.runOnClient(mc -> ((FfmpegSetupScreen) mc.screen).go());
            for (int i = 0; i < 20 * 600 && !context.computeOnClient(mc -> {
                Ffmpeg.refresh();
                return Ffmpeg.available();
            }); i += 20) context.waitTicks(20);
            String ff = context.computeOnClient(mc -> Ffmpeg.find());
            System.out.println(P + "LGPL FFmpeg installed: " + ff);
            if (ff == null || !ff.contains("vibeyclient")) throw new AssertionError("LGPL download did not install: " + ff);
            context.waitTicks(5);
            context.setScreen(() -> null);
        }
        if (!context.computeOnClient(mc -> Ffmpeg.available())) throw new AssertionError("this test needs FFmpeg");

        page(context, "clips_01_page_off_en", false);

        try (TestSingleplayerContext world = context.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeyclips"))
                .create()) {
            world.getClientWorld().waitForChunksRender();
            int[] p = context.computeOnClient(mc -> new int[] {(int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getZ())});
            int x = p[0], z = p[1], y = 120;
            cmd(world, "gamemode creative @a");
            cmd(world, "time set 6000");
            cmd(world, "weather clear");
            cmd(world, "gamerule doDaylightCycle false");
            cmd(world, "fill %d %d %d %d %d %d minecraft:air", x - 12, y + 1, z - 12, x + 12, y + 10, z + 12);
            cmd(world, "fill %d %d %d %d %d %d minecraft:grass_block", x - 12, y, z - 12, x + 12, y, z + 12);
            cmd(world, "tp @a %.1f %d %.1f 0 10", x + 0.5, y + 1, z + 0.5);
            context.waitTicks(30);
            world.getClientWorld().waitForChunksRender();

            context.waitTicks(100); // let world loading settle before measuring
            // ---------------------------------------------------------------------------------- buffer on
            context.runOnClient(mc -> m().buffer.set(true));
            context.waitTicks(10);
            shot(context, "clips_02_hint_toast");
            waitFor(context, "recording", 400, () -> m().recorder().state() == Recorder.State.RECORDING);
            context.runOnClient(mc -> dev.henny.vibeyclient.ui.toast.ToastStack.dismiss("clips.hint"));
            context.waitTicks(40);
            shot(context, "clips_03_recording_dot");
            System.out.println(P + "encoder " + context.computeOnClient(mc -> m().recorder().encoder()));
            page(context, "clips_04_page_on_en", true);
            context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.GERMAN));
            page(context, "clips_05_page_on_de", true);
            context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));

            // ---------------------------------------------------------------------------------- pauses
            context.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
            context.waitTicks(10);
            Recorder.Pause inv = context.computeOnClient(mc -> m().recorder().pause());
            shot(context, "clips_06_inventory_paused");
            if (inv != Recorder.Pause.INVENTORY) throw new AssertionError("inventory should pause, got " + inv);
            context.setScreen(() -> new ChatScreen("", false));
            context.waitTicks(10);
            Recorder.State chat = context.computeOnClient(mc -> m().recorder().state());
            if (chat != Recorder.State.RECORDING) throw new AssertionError("chat keeps recording, got " + chat);
            context.setScreen(() -> null);
            context.waitTicks(10);

            // ---------------------------------------------------------------------------------- frame time off/on
            // The PC is shared with other work: alternate off/on five times and compare medians. The low-FPS pause
            // would stop recording on a loaded machine, so it is off for the measurement.
            context.runOnClient(mc -> {
                m().lowFpsPause.set(false);
                // real frame rate: no 30 fps cap for an inactive window, no vsync, no 120 fps limit
                mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
                mc.options.enableVsync().set(false);
                mc.getWindow().updateVsync(false);
                mc.options.framerateLimit().set(260);
            });
            double[][] off = new double[5][], on = new double[5][];
            for (int i = 0; i < 5; i++) {
                context.runOnClient(mc -> m().buffer.set(false));
                context.waitTicks(40);
                off[i] = frames(context, "off#" + i);
                FrameGrabber.Readback how = FrameGrabber.mode;
                context.runOnClient(mc -> m().buffer.set(true));
                waitFor(context, "recording #" + i, 600, () -> m().recorder().state() == Recorder.State.RECORDING);
                context.waitTicks(60);
                context.runOnClient(mc -> m().recorder().grabber().resetStats());
                on[i] = frames(context, "on#" + i + " " + how);
                for (String l : context.computeOnClient(mc -> ClipsStats.lines())) System.out.println(P + "stats: " + l);
            }
            System.out.printf(Locale.ROOT, P + "FRAMES median off %.3f ms (p99 %.2f), on %.3f ms (p99 %.2f), render thread %.3f ms/capture%n",
                    median(off, 0), median(off, 1), median(on, 0), median(on, 1),
                    context.computeOnClient(mc -> m().recorder().grabber() == null ? -1 : m().recorder().grabber().renderMsAvg));
            FrameGrabber.mode = FrameGrabber.Readback.valueOf(System.getProperty("vibeyclient.clips.readback", "ASYNC"));
            context.runOnClient(mc -> {
                m().lowFpsPause.set(true);
                mc.options.framerateLimit().set(120);
            });
            context.waitTicks(200); // ring holds > 10 s for the saves below

            // ---------------------------------------------------------------------------------- hotkey save
            int before = clips().size();
            context.getInput().pressKey(m().saveKey.mapping());
            waitFor(context, "hotkey clip", 600, () -> !ClipSaver.saving() && clips().size() > before);
            context.waitTicks(4);
            shot(context, "clips_07_saved_toast");
            ClipStore.Clip hot = clips().get(0);
            checkClip(hot, "hotkey");

            // ---------------------------------------------------------------------------------- automatic multi-kill
            context.runOnClient(mc -> m().auto().testKill("Steve"));
            context.waitTicks(20);
            context.runOnClient(mc -> m().auto().testKill("Kai"));
            int before2 = clips().size();
            waitFor(context, "auto clip", 1200, () -> !m().auto().pending() && !ClipSaver.saving() && clips().size() > before2);
            ClipStore.Clip auto = clips().get(0);
            checkClip(auto, "multikill");
            if (auto.integer("kills", 0) != 2) throw new AssertionError("multikill should have 2 kills");
            if (!(auto.json().get("events") != null && auto.json().getAsJsonArray("events").size() == 2)) {
                throw new AssertionError("two events expected: " + auto.json());
            }

            // ---------------------------------------------------------------------------------- server rules
            context.runOnClient(mc -> ServerRules.apply(List.of("clips.auto"), "No auto clips here"));
            int before3 = clips().size();
            int kills = context.computeOnClient(mc -> m().auto().kills);
            context.runOnClient(mc -> m().auto().testKill("Alex"));
            context.waitTicks(100);
            if (context.computeOnClient(mc -> m().auto().pending() || m().auto().kills != kills) || clips().size() != before3) {
                throw new AssertionError("clips.auto must stop automatic clips");
            }
            context.runOnClient(mc -> ServerRules.apply(List.of("clips"), "Recording is not allowed on this server"));
            context.waitTicks(5);
            boolean dropped = context.computeOnClient(mc -> m().recorder().ring() == null && m().recorder().state() == Recorder.State.BLOCKED);
            if (!dropped) throw new AssertionError("clips rule must stop the buffer and drop the ring");
            page(context, "clips_08_rule_badge", true);
            context.runOnClient(mc -> ServerRules.clear());
            context.waitTicks(5);

            // ---------------------------------------------------------------------------------- gallery + player
            context.runOnClient(mc -> m().buffer.set(false));
            context.setScreen(() -> new ClipsGalleryScreen(null));
            context.waitTicks(60);
            shot(context, "clips_09_gallery_en");
            context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.GERMAN));
            context.setScreen(() -> new ClipsGalleryScreen(null));
            context.waitTicks(60);
            shot(context, "clips_10_gallery_de");
            context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
            ClipsGalleryScreen[] gallery = new ClipsGalleryScreen[1];
            context.runOnClient(mc -> {
                gallery[0] = new ClipsGalleryScreen(null);
                mc.setScreen(new ClipViewScreen(gallery[0], auto));
            });
            waitFor(context, "playing", 400, () -> view() != null && view().player() != null
                    && view().player().status() == MediaPlayer.Status.PLAYING && view().player().texture() != null);
            context.waitTicks(40);
            shot(context, "clips_11_player");
            context.runOnClient(mc -> view().toggleKeep());
            ClipsFiles.drain(3000);
            context.waitTicks(4);
            context.runOnClient(mc -> {
                view().startRename();
            });
            context.waitTicks(2);
            context.runOnClient(mc -> view().rename("Bridge 1v2"));
            ClipsFiles.drain(3000);
            context.waitTicks(6);
            context.runOnClient(mc -> view().togglePlay());
            context.waitTicks(4);
            shot(context, "clips_12_player_kept_renamed");
            ClipStore.Clip edited = ClipsFiles.store().get(auto.id());
            if (edited == null || !edited.kept() || !"Bridge 1v2".equals(edited.title())) {
                throw new AssertionError("keep/rename not saved: " + (edited == null ? null : edited.json()));
            }
            context.runOnClient(mc -> {
                view().delete();
                view().delete(); // confirm
            });
            ClipsFiles.drain(8000);
            context.waitTicks(10);
            if (ClipsFiles.store().get(auto.id()) != null || Files.exists(auto.video())) throw new AssertionError("delete failed");
            shot(context, "clips_13_gallery_after_delete");
            context.setScreen(() -> null);
            System.out.println(P + "ok");
        }
    }

    // ============================================================================================ checks

    private static void checkClip(ClipStore.Clip c, String reason) {
        try {
            if (!reason.equals(c.reason())) throw new AssertionError("reason " + c.reason() + " != " + reason);
            if (!Files.isRegularFile(c.video())) throw new AssertionError("no video");
            if (c.thumbnail() == null) throw new AssertionError("no thumbnail");
            JsonObject j = JsonParser.parseString(Files.readString(c.video().resolveSibling(c.id() + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
            for (String k : new String[] {"schema", "id", "created_at", "video", "duration_ms", "size_bytes", "width", "height", "fps",
                    "video_codec", "reason", "player", "mode", "minecraft", "loader", "client", "encoder", "kept"}) {
                if (!j.has(k)) throw new AssertionError("missing " + k + " in " + j);
            }
            if (!"singleplayer".equals(j.get("mode").getAsString())) throw new AssertionError("mode " + j.get("mode"));
            if (j.has("server")) throw new AssertionError("no server in singleplayer");
            if (!j.has("world")) throw new AssertionError("world expected");
            long ms = j.get("duration_ms").getAsLong();
            // decodes completely, and its length matches the JSON
            Process p = new ProcessBuilder(Ffmpeg.find(), "-hide_banner", "-v", "error", "-i", c.video().toString(), "-f", "null", "-")
                    .redirectErrorStream(true).start();
            String err = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            if (!p.waitFor(30, TimeUnit.SECONDS) || p.exitValue() != 0) throw new AssertionError("ffmpeg cannot decode the clip: " + err);
            System.out.println(P + "clip " + c.id() + " " + ms + " ms, " + c.sizeBytes() / 1024 + " KB, decode errors: "
                    + (err.isEmpty() ? "none" : err));
            if (ms < 1000) throw new AssertionError("clip too short: " + ms);
        } catch (java.io.IOException | InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    private static double median(double[][] runs, int k) {
        double[] v = new double[runs.length];
        for (int i = 0; i < runs.length; i++) v[i] = runs[i][k];
        Arrays.sort(v);
        return v[v.length / 2];
    }

    /** Average and p99 frame interval (ms) over 300 frames; prints FFmpeg CPU when recording. */
    private static double[] frames(ClientGameTestContext ctx, String label) {
        ctx.computeOnClient(mc -> ClipsStats.lines()); // baseline for the CPU delta
        System.out.println(P + "PHASE start " + label + " " + System.currentTimeMillis());
        ctx.runOnClient(mc -> ClipsHooks.measureFrames(4000));
        ctx.waitTicks(300);
        long[] t = ctx.computeOnClient(mc -> ClipsHooks.stopMeasuring());
        System.out.println(P + "PHASE end " + label + " " + System.currentTimeMillis());
        ctx.computeOnClient(mc -> ClipsStats.lines());
        if (t.length < 50) return new double[] {-1, -1};
        long[] s = t.clone();
        Arrays.sort(s);
        double avg = Arrays.stream(t).average().orElse(0) / 1e6;
        double p99 = s[(int) (s.length * 0.99)] / 1e6;
        System.out.printf(Locale.ROOT, P + "frames %s: %d frames, avg %.3f ms (%.0f fps), p99 %.2f ms, ffmpeg cpu %.1f %% of a core, rss %d MB%n",
                label, t.length, avg, 1000 / avg, p99, ClipsStats.cpuPercentOfCore(), ClipsStats.rssBytes() / 1048576);
        return new double[] {avg, p99};
    }

    private static List<ClipStore.Clip> clips() {
        return ClipsFiles.store().list();
    }

    private static ClipViewScreen view() {
        return net.minecraft.client.Minecraft.getInstance().screen instanceof ClipViewScreen v ? v : null;
    }

    private static void waitFor(ClientGameTestContext ctx, String what, int maxTicks, java.util.function.BooleanSupplier ok) {
        for (int i = 0; i < maxTicks; i++) {
            if (ctx.computeOnClient(mc -> ok.getAsBoolean())) return;
            ctx.waitTicks(1);
        }
        String status = ctx.computeOnClient(mc -> m().recorder().state() + " " + m().recorder().pause() + " " + m().recorder().failure()
                + " | " + String.join(" | ", ClipsStats.lines()));
        throw new AssertionError("timed out waiting for " + what + " (" + status + ")");
    }

    private static void page(ClientGameTestContext ctx, String shot, boolean scrolled) {
        ModulesPanel.preselect("clips");
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(10);
        shot(ctx, shot);
        ctx.setScreen(() -> null);
        ctx.waitTicks(2);
    }

    private static ClipsModule m() {
        return ModuleManager.get().get(ClipsModule.class);
    }

    private static void cmd(TestSingleplayerContext world, String format, Object... args) {
        world.getServer().runCommand(args.length == 0 ? format : String.format(Locale.ROOT, format, args));
    }

    private static void shot(ClientGameTestContext ctx, String name) {
        ctx.runOnClient(mc -> mc.gui.getChat().clearMessages(false));
        ctx.waitTicks(1);
        ctx.takeScreenshot(name);
    }
}
