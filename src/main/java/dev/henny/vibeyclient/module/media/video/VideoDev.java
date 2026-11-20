package dev.henny.vibeyclient.module.media.video;

import dev.henny.vibeyclient.dev.DevDriver;
import dev.henny.vibeyclient.media.ffmpeg.Ffmpeg;
import dev.henny.vibeyclient.media.ffmpeg.FfmpegSetupScreen;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.media.video.ui.SetupNeed;
import dev.henny.vibeyclient.module.media.video.ui.SetupScreen;
import dev.henny.vibeyclient.module.media.video.ui.SetupScreens;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class VideoDev {
    private VideoDev() {}

    static void register(Video v) {
        if (!DevDriver.active()) return;
        DevDriver.EXTRA.put("video", arg -> exec(v, arg));
    }

    private static MediaScreen at(Video v, String idx) {
        List<MediaScreen> all = new ArrayList<>(v.screens.values());
        int i = Integer.parseInt(idx.trim());
        return i >= 0 && i < all.size() ? all.get(i) : null;
    }

    private static String exec(Video v, String arg) {
        String[] a = arg.split(" ");
        String sub = a[0].toLowerCase(Locale.ROOT);
        Minecraft mc = Minecraft.getInstance();
        return switch (sub) {
            case "list" -> list(v);
            case "place" -> VideoDevPlace.place(v, a);
            case "placing" -> {
                MediaSource src = MediaSource.parse(arg.substring(sub.length()).trim());
                if (src == null) yield "err bad source";
                v.actions.newScreen(src, 16f / 9f);
                yield "ok";
            }
            case "new", "key" -> {
                v.actions.screensKey();
                yield "ok";
            }
            case "play", "pause", "seek", "overlay", "accept", "remove", "lock" -> onScreen(v, sub, a);
            case "others" -> {
                v.cfg.others.set(VideoFeature.Others.valueOf(a[1].toUpperCase(Locale.ROOT)));
                yield "ok";
            }
            case "setup" -> {
                ClientCompat.openScreen(SetupScreens.of(null, switch (a[1]) {
                    case "mod" -> SetupNeed.BROWSER_MOD;
                    case "natives" -> SetupNeed.BROWSER_NATIVES;
                    default -> SetupNeed.FFMPEG;
                }));
                yield "ok";
            }
            case "hideffmpeg" -> {
                Ffmpeg.hiddenForTest = a[1].equals("on");
                Ffmpeg.refresh();
                yield "ok available=" + Ffmpeg.available();
            }
            case "setupgo" -> setupGo(mc);
            case "edit" -> {
                dev.henny.vibeyclient.ui.hud.HudLayout.openEditor(ClientCompat.screen());
                yield "ok";
            }
            case "screen" -> "ok " + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName());
            default -> "err video " + sub;
        };
    }

    private static String list(Video v) {
        StringBuilder sb = new StringBuilder("ok");
        int i = 0;
        for (MediaScreen s : v.screens.values()) {
            String st = s.player == null ? "none" : s.player.status().name();
            sb.append(String.format(Locale.ROOT, " [%d %s %s remote=%s accepted=%s player=%s pos=%d/%d exp=%d status=%s]", i++,
                    s.source == null ? "-" : s.source.kind(), s.source == null ? "-" : s.source.label(), s.remote, s.accepted, st,
                    s.player == null ? -1 : s.player.positionMs(), s.timeline.durationMs(), s.expectedMs(), v.status.line(s)));
        }
        return sb.toString();
    }

    private static String onScreen(Video v, String sub, String[] a) {
        MediaScreen s = a.length > 1 ? at(v, a[1]) : null;
        if (s == null) return "err no screen";
        switch (sub) {
            case "play", "pause" -> v.actions.setPlaying(s, sub.equals("play"));
            case "seek" -> v.actions.seek(s, Long.parseLong(a[2]));
            case "overlay" -> v.actions.toggleOverlay(s);
            case "accept" -> s.accepted = true;
            case "lock" -> v.actions.toggleLock(s);
            default -> v.actions.remove(s);
        }
        return "ok";
    }

    private static String setupGo(Minecraft mc) {
        if (mc.screen instanceof SetupScreen ss) {
            ss.go();
            return "ok";
        }
        if (mc.screen instanceof FfmpegSetupScreen ss) {
            ss.go();
            return "ok";
        }
        return "err no setup screen";
    }
}
